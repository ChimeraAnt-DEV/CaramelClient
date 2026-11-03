package org.chimeramc.client.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;

import org.chimeramc.client.R;
import org.chimeramc.client.core.antegg.AntEggLoader;
import org.chimeramc.client.core.antegg.AntEggPackage;
import org.chimeramc.client.core.antegg.AntEggScriptHost;
import org.chimeramc.client.core.antegg.ScriptManager;
import org.chimeramc.client.core.antegg.ScriptStateStore;
import org.chimeramc.client.databinding.ActivityScriptManagerBinding;
import org.chimeramc.client.ui.adapter.ScriptManagerAdapter;
import org.chimeramc.client.ui.animation.DynamicAnim;
import org.chimeramc.client.ui.dialogs.CustomAlertDialog;
import org.chimeramc.client.util.MainThread;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * The script manager screen.
 *
 * <p>Lists every package in the {@code .AntEgg} sandbox with its declared dependencies, the order
 * the loader would install them, and a per-package enable switch. Disabling a script stops it being
 * run by {@code runEnabled} without removing the package, so a script that misbehaves can be turned
 * off and back on without re-importing it.
 *
 * <p>The screen is a plain view of {@link ScriptManager}'s read model: the list is built off the UI
 * thread (it reads manifests from disk) and rendered through a {@code ListAdapter} so a toggle
 * animates one row.
 */
public class ScriptManagerActivity extends BaseActivity implements ScriptManagerAdapter.Listener {

    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    private ActivityScriptManagerBinding binding;
    private ScriptManagerAdapter adapter;
    private ScriptStateStore stateStore;
    private Set<String> disabledIds = Collections.emptySet();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScriptManagerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        stateStore = new ScriptStateStore(this);

        if (binding.scriptManagerRoot != null) {
            DynamicAnim.applyPressScaleRecursively(binding.scriptManagerRoot);
        }

        adapter = new ScriptManagerAdapter(this);
        binding.scriptManagerRecycler.setLayoutManager(new LinearLayoutManager(this));
        binding.scriptManagerRecycler.setAdapter(adapter);

        binding.scriptManagerReloadButton.setOnClickListener(v -> reloadScripts());
        DynamicAnim.applyPressScale(binding.scriptManagerReloadButton);

        loadPackages();
    }

    /** Reads the sandbox off the UI thread and renders the rows. */
    private void loadPackages() {
        binding.scriptManagerLoading.setVisibility(View.VISIBLE);
        submit(() -> {
            File sandbox = AntEggLoader.sandboxRoot(this);
            List<ScriptManager.ScriptInfo> rows = ScriptManager.list(sandbox);
            Set<String> disabled = stateStore.disabledIds();
            MainThread.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                disabledIds = disabled;
                binding.scriptManagerLoading.setVisibility(View.GONE);
                adapter.submitList(rows);
                binding.scriptManagerEmpty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    /**
     * Re-runs every enabled script, reporting how many succeeded.
     *
     * <p>The run happens off the UI thread because it executes Lua; the result is a toast rather
     * than a dialog because the outcome is a short summary, and the per-script detail is already in
     * the in-app log through {@code AntEggScriptHost}.
     */
    private void reloadScripts() {
        submit(() -> {
            File sandbox = AntEggLoader.sandboxRoot(this);
            Set<String> disabled = stateStore.disabledIds();
            List<ScriptManager.RunOutcome> outcomes = ScriptManager.runEnabled(sandbox, disabled);
            int success = 0;
            for (ScriptManager.RunOutcome outcome : outcomes) {
                if (outcome.success) success++;
            }
            final int total = outcomes.size();
            final int ok = success;
            MainThread.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (total == 0) {
                    Toast.makeText(this, R.string.script_manager_reload_none, Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Toast.makeText(this,
                            getString(R.string.script_manager_reload_done, ok, total),
                            Toast.LENGTH_LONG).show();
                }
                loadPackages();
            });
        });
    }

    @Override
    public void onToggle(ScriptManager.ScriptInfo info, boolean enabled) {
        stateStore.setEnabled(info.id, enabled);
        disabledIds = stateStore.disabledIds();
    }

    @Override
    public void onRun(ScriptManager.ScriptInfo info) {
        submit(() -> {
            File dir = new File(AntEggLoader.sandboxRoot(this), info.id);
            AntEggPackage.Result read = AntEggPackage.readExtracted(dir);
            if (read == null || !read.isValid() || read.manifest == null) {
                MainThread.post(() -> Toast.makeText(this, R.string.script_manager_run_failed,
                        Toast.LENGTH_SHORT).show());
                return;
            }
            AntEggScriptHost.Result result = AntEggScriptHost.run(dir, read.manifest);
            MainThread.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (result.success) {
                    Toast.makeText(this, getString(R.string.script_manager_run_ok, info.name),
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, getString(R.string.script_manager_run_failed_reason,
                            result.error), Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    @Override
    public void onRemove(ScriptManager.ScriptInfo info) {
        new CustomAlertDialog(this)
                .setTitleText(getString(R.string.script_manager_remove))
                .setMessage(getString(R.string.script_manager_remove_confirm, info.name))
                .setPositiveButton(getString(R.string.script_manager_remove),
                        d -> removePackage(info))
                .setNegativeButton(getString(R.string.cancel), d -> { })
                .show();
    }

    private void removePackage(ScriptManager.ScriptInfo info) {
        submit(() -> {
            File dir = new File(AntEggLoader.sandboxRoot(this), info.id);
            boolean deleted = AntEggPackage.deleteRecursively(dir);
            MainThread.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (!deleted) {
                    Toast.makeText(this, R.string.script_manager_remove_failed, Toast.LENGTH_SHORT)
                            .show();
                }
                loadPackages();
            });
        });
    }

    private void submit(Runnable task) {
        if (worker.isShutdown()) return;
        try {
            worker.execute(task);
        } catch (RejectedExecutionException rejected) {
            // The screen is gone; dropping the work is the right outcome.
        }
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }
}

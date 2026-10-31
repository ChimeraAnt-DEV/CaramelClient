package org.chimeramc.client.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import org.chimeramc.client.R;
import org.chimeramc.client.databinding.ActivitySetupWizardBinding;
import org.chimeramc.client.core.auth.MsftAccountStore;
import org.chimeramc.client.core.versions.GameVersion;
import org.chimeramc.client.core.versions.VersionManager;
import org.chimeramc.client.launcher.core.setup.SetupWizard;
import org.chimeramc.client.settings.FeatureSettings;
import org.chimeramc.client.ui.animation.DynamicAnim;
import org.chimeramc.client.util.ApkImportManager;
import org.chimeramc.client.util.PersonalizationManager;

/**
 * The first-run wizard (U13): import the game, pick a version, sign in, ready.
 *
 * <p>The step order and the resume behaviour live in {@link SetupWizard} (pure and unit-tested);
 * this screen only renders the current step and performs the action each one needs. That split is
 * deliberate — the ordering rules are the part that can be wrong in a way nobody notices until a
 * user hits the dead end, so they are not entangled with a Context.
 *
 * <p>The wizard is skippable. A user who already has a working install must never be trapped in it,
 * so "Skip" marks it done and opens the launcher. It is also only ever shown when the launcher has
 * no installed instance, so an existing user never sees it at all.
 */
public class SetupWizardActivity extends BaseActivity {

    /** Set once the user finishes or skips; the splash reads it to decide whether to show this. */
    public static final String KEY_WIZARD_DONE = "setup_wizard_done";

    private VersionManager versionManager;
    private ApkImportManager apkImportManager;
    private ActivityResultLauncher<Intent> importLauncher;

    private ActivitySetupWizardBinding binding;
    private TextView title;
    private TextView body;
    private TextView status;
    private Button primaryButton;
    private Button secondaryButton;
    private LinearLayout rail;
    private LinearLayout compatRows;

    /** A single thread for the compatibility gather; a newer render supersedes an older result. */
    private final java.util.concurrent.ExecutorService compatExecutor =
            java.util.concurrent.Executors.newSingleThreadExecutor();
    private int compatGeneration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySetupWizardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        versionManager = VersionManager.get(this);

        apkImportManager = new ApkImportManager(this, null);
        apkImportManager.setOnImportCompleteListener(() -> {
            versionManager.loadAllVersions();
            render();
        });
        apkImportManager.setOnImportFailedListener((message, versionName) -> {
            if (status != null) {
                status.setText(getString(R.string.setup_import_failed, message));
                status.setVisibility(View.VISIBLE);
            }
        });
        importLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (apkImportManager != null) {
                        apkImportManager.handleActivityResult(result.getResultCode(), result.getData());
                    }
                });

        title = binding.setupTitle;
        body = binding.setupBody;
        status = binding.setupStatus;
        rail = binding.setupRail;
        compatRows = binding.setupCompatRows;
        primaryButton = binding.setupPrimary;
        secondaryButton = binding.setupSecondary;

        if (primaryButton != null) primaryButton.setOnClickListener(v -> onPrimary());
        if (secondaryButton != null) {
            secondaryButton.setOnClickListener(v -> onSecondary());
            DynamicAnim.applyPressScale(secondaryButton);
        }
        if (primaryButton != null) DynamicAnim.applyPressScale(primaryButton);

        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // An import or a sign-in happens on another screen; re-read the facts when we come back so
        // the wizard advances without the user having to press anything.
        render();
    }

    // ---------------------------------------------------------------------------- facts

    private SetupWizard.Facts facts() {
        boolean imported = hasInstalledVersion();
        GameVersion selected = versionManager != null ? versionManager.getSelectedVersion() : null;
        boolean signInRequired = FeatureSettings.getInstance().isLauncherManagedMcLoginEnabled();
        return new SetupWizard.Facts()
                .gameImported(imported)
                .versionChosen(selected != null)
                .signInRequired(signInRequired)
                .signedIn(isSignedIn());
    }

    private boolean hasInstalledVersion() {
        try {
            return versionManager != null && !versionManager.getInstalledVersions().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isSignedIn() {
        try {
            for (MsftAccountStore.MsftAccount account : MsftAccountStore.list(this)) {
                if (account.active && account.minecraftUsername != null
                        && !account.minecraftUsername.isEmpty()) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    // ---------------------------------------------------------------------------- render

    private void render() {
        SetupWizard.Facts facts = facts();
        SetupWizard.Step step = SetupWizard.currentStep(facts);

        if (step == SetupWizard.Step.READY) {
            finishWizard();
            return;
        }

        renderRail(facts);
        renderCompat(facts);
        if (status != null) status.setVisibility(View.GONE);

        switch (step) {
            case IMPORT_GAME:
                title.setText(R.string.setup_step_import_title);
                body.setText(R.string.setup_step_import_body);
                primaryButton.setText(R.string.setup_step_import_action);
                secondaryButton.setText(R.string.setup_skip);
                break;
            case PICK_VERSION:
                title.setText(R.string.setup_step_version_title);
                body.setText(R.string.setup_step_version_body);
                primaryButton.setText(R.string.setup_step_version_action);
                secondaryButton.setText(R.string.setup_skip);
                break;
            case SIGN_IN:
                title.setText(R.string.setup_step_signin_title);
                body.setText(R.string.setup_step_signin_body);
                primaryButton.setText(R.string.setup_step_signin_action);
                secondaryButton.setText(R.string.setup_skip);
                break;
            default:
                break;
        }
    }

    /**
     * Draws the progress rail from the *active* steps, so a wizard with sign-in off shows three
     * dots rather than four with one greyed out. The fill follows {@link SetupWizard#currentIndex}.
     */
    private void renderRail(SetupWizard.Facts facts) {
        if (rail == null) return;
        rail.removeAllViews();
        SetupWizard.Step[] steps = SetupWizard.activeSteps(facts);
        int current = SetupWizard.currentIndex(facts);
        float density = getResources().getDisplayMetrics().density;
        int accent = new PersonalizationManager(this).getAccentColor();
        if (accent == 0) accent = getColor(R.color.primary);

        for (int i = 0; i < steps.length; i++) {
            View dot = new View(this);
            int size = (int) ((i == current ? 10 : 8) * density);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins((int) (4 * density), 0, (int) (4 * density), 0);
            dot.setLayoutParams(lp);
            android.graphics.drawable.GradientDrawable shape =
                    new android.graphics.drawable.GradientDrawable();
            shape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            shape.setColor(i <= current ? accent : getColor(R.color.text_secondary));
            dot.setBackground(shape);
            rail.addView(dot);
        }
    }

    /**
     * Renders the device compatibility pass (item 284).
     *
     * <p>The check only means something once there is a version to judge (its ABI and game-files
     * facts are about the selected instance), so it is shown from the pick-version step onward and
     * hidden on the import step, where there is nothing to check yet.
     */
    private void renderCompat(SetupWizard.Facts facts) {
        if (compatRows == null) return;
        android.view.View card = binding.setupCompat;
        if (card == null) return;

        if (!facts.gameImported) {
            card.setVisibility(View.GONE);
            return;
        }
        card.setVisibility(View.VISIBLE);

        // Gathering reads the disk (free space) and opens the APK (ABI), so it runs off the UI
        // thread; the rows are painted back on it. A gather generation counter drops a result whose
        // screen has already moved on.
        final int generation = ++compatGeneration;
        final GameVersion selected = versionManager != null
                ? versionManager.getSelectedVersion() : null;
        compatExecutor.execute(() -> {
            final org.chimeramc.client.core.setup.CompatibilityCheck.Facts checkFacts =
                    org.chimeramc.client.launcher.core.setup.CompatibilityProbe.gather(
                            SetupWizardActivity.this, selected);
            final java.util.List<org.chimeramc.client.core.setup.CompatibilityCheck.Result> results =
                    org.chimeramc.client.core.setup.CompatibilityCheck.evaluate(checkFacts);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (generation != compatGeneration || compatRows == null) return;
                compatRows.removeAllViews();
                for (org.chimeramc.client.core.setup.CompatibilityCheck.Result result : results) {
                    addCompatRow(result, checkFacts);
                }
            });
        });
    }

    /** One compatibility row: a status line and, when there is something to do, a Fix button. */
    private void addCompatRow(org.chimeramc.client.core.setup.CompatibilityCheck.Result result,
                              org.chimeramc.client.core.setup.CompatibilityCheck.Facts checkFacts) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(0, (int) (6 * getResources().getDisplayMetrics().density),
                0, (int) (6 * getResources().getDisplayMetrics().density));

        TextView text = new TextView(this);
        text.setText(compatLabel(result, checkFacts));
        text.setTextSize(12);
        text.setTextColor(compatColor(result));
        text.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(text);

        if (result.fix != org.chimeramc.client.core.setup.CompatibilityCheck.Fix.NONE) {
            Button fix = new Button(this);
            fix.setText(R.string.setup_compat_fix);
            fix.setMinHeight(0);
            fix.setMinimumHeight(0);
            fix.setPadding((int) (12 * getResources().getDisplayMetrics().density), 0,
                    (int) (12 * getResources().getDisplayMetrics().density), 0);
            fix.setOnClickListener(v -> onCompatFix(result.fix));
            DynamicAnim.applyPressScale(fix);
            row.addView(fix);
        }
        compatRows.addView(row);
    }

    private CharSequence compatLabel(
            org.chimeramc.client.core.setup.CompatibilityCheck.Result result,
            org.chimeramc.client.core.setup.CompatibilityCheck.Facts checkFacts) {
        switch (result.id) {
            case FREE_STORAGE: {
                String size = org.chimeramc.client.core.setup.CompatibilityCheck
                        .formatBytes(checkFacts.freeBytes);
                if (result.severity == org.chimeramc.client.core.setup.CompatibilityCheck.Severity.OK) {
                    return getString(R.string.setup_compat_free_space_ok, size);
                }
                if (result.severity
                        == org.chimeramc.client.core.setup.CompatibilityCheck.Severity.WARNING) {
                    return getString(R.string.setup_compat_free_space_low, size);
                }
                String needed = org.chimeramc.client.core.setup.CompatibilityCheck.formatBytes(
                        org.chimeramc.client.core.setup.CompatibilityCheck.MIN_FREE_BYTES);
                return getString(R.string.setup_compat_free_space_blocking, size, needed);
            }
            case ABI:
                return getString(result.severity
                        == org.chimeramc.client.core.setup.CompatibilityCheck.Severity.OK
                        ? R.string.setup_compat_abi_ok : R.string.setup_compat_abi_blocking);
            case PLAY_STORE_INSTALL:
                return getString(result.severity
                        == org.chimeramc.client.core.setup.CompatibilityCheck.Severity.OK
                        ? R.string.setup_compat_play_store_ok : R.string.setup_compat_play_store_warn);
            case STORAGE_PERMISSION:
                return getString(result.severity
                        == org.chimeramc.client.core.setup.CompatibilityCheck.Severity.OK
                        ? R.string.setup_compat_perm_ok : R.string.setup_compat_perm_blocking);
            default:
                return "";
        }
    }

    private int compatColor(org.chimeramc.client.core.setup.CompatibilityCheck.Result result) {
        switch (result.severity) {
            case OK:
                return getColor(R.color.ui_text_secondary);
            case WARNING:
                return getColor(R.color.ui_warning);
            case BLOCKING:
            default:
                return getColor(R.color.ui_danger);
        }
    }

    /** Runs the fix a compatibility row offers. */
    private void onCompatFix(org.chimeramc.client.core.setup.CompatibilityCheck.Fix fix) {
        switch (fix) {
            case OPEN_STORAGE:
                openStorageSettings();
                break;
            case OPEN_PERMISSIONS:
                openAppSettings();
                break;
            case IMPORT_GAME:
                startImportPicker();
                break;
            case REPAIR:
            case NONE:
            default:
                break;
        }
    }

    private void openStorageSettings() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS);
            startActivity(intent);
        } catch (Throwable t) {
            // Not every OEM exposes the internal-storage settings screen; fall back to the app's own
            // settings page rather than leaving the button dead.
            openAppSettings();
        }
    }

    private void openAppSettings() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Throwable t) {
            if (status != null) {
                status.setText(R.string.setup_compat_perm_blocking);
                status.setVisibility(View.VISIBLE);
            }
        }
    }

    // ---------------------------------------------------------------------------- actions

    private void onPrimary() {
        SetupWizard.Step step = SetupWizard.currentStep(facts());
        switch (step) {
            case IMPORT_GAME:
                startImportPicker();
                break;
            case PICK_VERSION:
                // The Library tab is where instances are chosen; opening it is the step's action
                // rather than duplicating a picker here.
                startActivity(new Intent(this, LibraryActivity.class));
                break;
            case SIGN_IN:
                startActivity(new Intent(this, AccountsActivity.class));
                break;
            default:
                finishWizard();
                break;
        }
    }

    private void onSecondary() {
        finishWizard();
    }

    private void startImportPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {
                "application/vnd.android.package-archive",
                "application/octet-stream",
                "application/zip",
        });
        importLauncher.launch(intent);
    }

    /** Marks the wizard done and opens the launcher. */
    private void finishWizard() {
        getSharedPreferences("glowberry_setup", MODE_PRIVATE)
                .edit().putBoolean(KEY_WIZARD_DONE, true).apply();
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }

    /**
     * Whether the wizard should be shown on this launch.
     *
     * <p>Two conditions, and both matter: the user has not finished it before, *and* there is no
     * installed instance. The second means an existing user upgrading into a build that has the
     * wizard never sees it — their install already answers the first step, so showing a "welcome"
     * would be pure friction.
     */
    public static boolean shouldShow(Activity activity) {
        if (activity == null) return false;
        if (activity.getSharedPreferences("glowberry_setup", Activity.MODE_PRIVATE)
                .getBoolean(KEY_WIZARD_DONE, false)) {
            return false;
        }
        try {
            return VersionManager.get(activity).getInstalledVersions().isEmpty();
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        compatExecutor.shutdownNow();
        // The wizard's import manager owns a single-thread pool; without this it outlived the wizard.
        if (apkImportManager != null) {
            apkImportManager.release();
            apkImportManager = null;
        }
        super.onDestroy();
    }
}

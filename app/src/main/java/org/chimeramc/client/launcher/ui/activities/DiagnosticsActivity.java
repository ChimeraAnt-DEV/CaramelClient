package org.chimeramc.client.launcher.ui.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.StatFs;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.chimeramc.client.BuildConfig;
import org.chimeramc.client.R;
import org.chimeramc.client.core.versions.GameVersion;
import org.chimeramc.client.core.versions.VersionManager;
import org.chimeramc.client.ui.activities.BaseActivity;
import org.chimeramc.client.util.AppLog;

import java.io.File;
import java.util.List;
import java.util.Locale;

/**
 * The diagnostics screen: one copyable report of the build, the device and the instance store.
 *
 * <p>This used to be a placeholder class that drew nothing, so the settings entry "Diagnostics"
 * opened a blank window. It now collects the facts a support answer needs — version, device, ABI,
 * storage headroom and the installed instances — into text that can be copied or shared, rather
 * than a screen of decorative gauges that measure nothing.
 */
public class DiagnosticsActivity extends BaseActivity {

    private static final String TAG = "DiagnosticsActivity";

    private final StringBuilder report = new StringBuilder();
    private TextView reportView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        collect();
        setContentView(buildUi());
        loadInstances();
    }

    /** Everything that can be read without waiting on a manager. */
    private void collect() {
        append(getString(R.string.caramel_diagnostics_build));
        append("Version: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");
        append("Package: " + BuildConfig.APPLICATION_ID);
        append("Build type: " + BuildConfig.BUILD_TYPE);
        append("");
        append(getString(R.string.caramel_diagnostics_device));
        append("Android " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")");
        append("Device: " + Build.MANUFACTURER + " " + Build.MODEL);
        append("ABIs: " + String.join(", ", Build.SUPPORTED_ABIS));
        append("Locale: " + Locale.getDefault().toLanguageTag());
        append("");
        append(getString(R.string.caramel_diagnostics_storage));
        append(storageLine(getFilesDir()));
        append(storageLine(getCacheDir()));
    }

    /** Free and total space for the volume {@code dir} lives on, in whole megabytes. */
    private String storageLine(File dir) {
        try {
            StatFs stat = new StatFs(dir.getAbsolutePath());
            long freeMb = (stat.getAvailableBlocksLong() * stat.getBlockSizeLong()) / (1024L * 1024L);
            long totalMb = (stat.getBlockCountLong() * stat.getBlockSizeLong()) / (1024L * 1024L);
            return dir.getName() + ": " + freeMb + " MB free of " + totalMb + " MB";
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "storage-" + dir.getName(), "Could not read the free space", t);
            return dir.getName() + ": unavailable";
        }
    }

    /** Adds the instance list once the version store has loaded, then refreshes the view. */
    private void loadInstances() {
        VersionManager.initializeAsync(this, new VersionManager.InitCallback() {
            @Override
            public void onReady(VersionManager manager) {
                runOnUiThread(() -> {
                    append("");
                    append(getString(R.string.caramel_diagnostics_instances));
                    List<GameVersion> installed = manager.getInstalledVersions();
                    List<GameVersion> custom = manager.getCustomVersions();
                    append("Installed: " + (installed == null ? 0 : installed.size()));
                    append("Imported: " + (custom == null ? 0 : custom.size()));
                    if (custom != null) {
                        for (GameVersion v : custom) {
                            String name = v.displayName == null ? v.directoryName : v.displayName;
                            append("  - " + name);
                        }
                    }
                    refresh();
                });
            }

            @Override
            public void onFailed(Throwable error) {
                AppLog.warnOnce(TAG, "instances", "Could not read the instance store", error);
                runOnUiThread(() -> {
                    append("");
                    append(getString(R.string.caramel_diagnostics_instances));
                    append("unavailable");
                    refresh();
                });
            }
        });
    }

    private void append(String line) {
        report.append(line).append('\n');
    }

    private void refresh() {
        if (reportView != null) reportView.setText(report.toString());
    }

    private LinearLayout buildUi() {
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(getColor(R.color.launcher_backdrop));
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText(R.string.caramel_diagnostics_title);
        title.setTextColor(getColor(R.color.white));
        title.setTextSize(22f);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText(R.string.caramel_diagnostics_sub);
        sub.setTextColor(0xFF9AA8D0);
        sub.setTextSize(13f);
        sub.setPadding(0, Math.round(4 * getResources().getDisplayMetrics().density), 0, pad);
        root.addView(sub);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        reportView = new TextView(this);
        reportView.setText(report.toString());
        reportView.setTextColor(0xFFEAF0FF);
        reportView.setTextSize(12.5f);
        reportView.setTypeface(Typeface.MONOSPACE);
        reportView.setTextIsSelectable(true);
        scroll.addView(reportView);
        root.addView(scroll);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);
        actions.setPadding(0, pad, 0, 0);
        actions.addView(button(R.string.caramel_diagnostics_copy, v -> copyReport()));
        actions.addView(button(R.string.caramel_diagnostics_share, v -> shareReport()));
        root.addView(actions);
        return root;
    }

    private Button button(int labelRes, android.view.View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(labelRes);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(Math.round(8 * getResources().getDisplayMetrics().density), 0, 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private void copyReport() {
        try {
            ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText(
                        getString(R.string.caramel_diagnostics_title), report.toString()));
                org.chimeramc.client.ui.compose.caramel.CaramelNoticeBus.post(this,
                        getString(R.string.caramel_diagnostics_copied));
            }
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "copy", "Could not copy the report", t);
        }
    }

    private void shareReport() {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.caramel_diagnostics_title));
            intent.putExtra(Intent.EXTRA_TEXT, report.toString());
            startActivity(Intent.createChooser(intent, getString(R.string.caramel_diagnostics_share)));
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "share", "Could not share the report", t);
        }
    }
}

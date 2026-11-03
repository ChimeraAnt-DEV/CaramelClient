package org.chimeramc.client.ui.activities;

import org.chimeramc.client.util.AppLog;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import org.chimeramc.client.R;
import org.chimeramc.client.databinding.ActivitySetupWizardBinding;
import org.chimeramc.client.launcher.core.setup.SetupWizard;
import org.chimeramc.client.ui.animation.DynamicAnim;
import org.chimeramc.client.util.ApkImportManager;
import org.chimeramc.client.util.PersonalizationManager;

/**
 * The five-step first-run wizard: licence, install an instance, sign in, customise, changelog/Discord.
 *
 * <p>The ordering and the rules live in {@link SetupWizard} (pure and unit-tested); this screen only
 * renders the current step and performs its action. Only the licence is mandatory — its checkbox
 * gates the Next button — and every later step can be skipped, so a user who wants to play now is
 * never trapped.
 *
 * <p>The wizard is shown only on a first launch with no installed instance, so an existing user
 * upgrading into a build with the wizard never sees it.
 */
public class SetupWizardActivity extends BaseActivity {

    /** Set once the user finishes or skips; the splash reads it to decide whether to show this. */
    public static final String KEY_WIZARD_DONE = "setup_wizard_done";

    private ActivitySetupWizardBinding binding;
    private TextView stepLabel;
    private TextView title;
    private TextView body;
    private TextView status;
    private TextView terms;
    private View termsScroll;
    private View licenseCheck;
    private View finishActions;
    private Button primary;
    private Button back;
    private Button skip;
    private Button changelog;
    private Button discord;
    private LinearLayout rail;
    private LinearLayout compatRows;

    private ApkImportManager apkImportManager;
    private ActivityResultLauncher<Intent> importLauncher;

    private SetupWizard.Step step = SetupWizard.Step.LICENSE;
    private boolean licenseAccepted;

    /** A single thread for the compatibility gather; a newer render supersedes an older result. */
    private final java.util.concurrent.ExecutorService compatExecutor =
            java.util.concurrent.Executors.newSingleThreadExecutor();
    private int compatGeneration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySetupWizardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        stepLabel = binding.setupStepLabel;
        title = binding.setupTitle;
        body = binding.setupBody;
        status = binding.setupStatus;
        terms = binding.setupTerms;
        termsScroll = binding.setupTermsScroll;
        licenseCheck = binding.setupLicenseCheck;
        finishActions = binding.setupFinishActions;
        primary = binding.setupPrimary;
        back = binding.setupBack;
        skip = binding.setupSkip;
        changelog = binding.setupChangelog;
        discord = binding.setupDiscord;
        rail = binding.setupRail;
        compatRows = binding.setupCompatRows;

        terms.setText(R.string.setup_license_terms);

        if (licenseCheck instanceof com.google.android.material.checkbox.MaterialCheckBox) {
            ((com.google.android.material.checkbox.MaterialCheckBox) licenseCheck)
                    .setOnCheckedChangeListener((btn, checked) -> {
                        licenseAccepted = checked;
                        updatePrimaryState();
                    });
        }
        primary.setOnClickListener(v -> onNext());
        back.setOnClickListener(v -> onBack());
        skip.setOnClickListener(v -> onSkip());
        if (changelog != null) {
            changelog.setOnClickListener(v -> startActivity(
                    new Intent(this, org.chimeramc.client.launcher.ui.activities.ChangelogActivity.class)));
        }
        if (discord != null) {
            discord.setOnClickListener(v -> openDiscord());
        }
        DynamicAnim.applyPressScale(primary);
        DynamicAnim.applyPressScale(back);
        DynamicAnim.applyPressScale(skip);

        apkImportManager = new ApkImportManager(this, null);
        apkImportManager.setOnImportCompleteListener(this::render);
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

        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // An import or a sign-in happens on another screen; re-render on return.
        render();
    }

    // ---------------------------------------------------------------------------- render

    private void render() {
        if (step == SetupWizard.Step.READY) {
            finishWizard();
            return;
        }
        if (status != null && step != SetupWizard.Step.INSTALL_INSTANCE) {
            status.setVisibility(View.GONE);
        }

        renderRail();
        renderStepContent();
        renderCompat();
    }

    private void renderStepContent() {
        boolean license = step == SetupWizard.Step.LICENSE;
        termsScroll.setVisibility(license ? View.VISIBLE : View.GONE);
        licenseCheck.setVisibility(license ? View.VISIBLE : View.GONE);
        finishActions.setVisibility(step == SetupWizard.Step.CHANGELOG_DISCORD
                ? View.VISIBLE : View.GONE);

        switch (step) {
            case LICENSE:
                title.setText(R.string.setup_license_title);
                body.setText(R.string.setup_license_body);
                break;
            case INSTALL_INSTANCE:
                title.setText(R.string.setup_instance_title);
                body.setText(R.string.setup_instance_body);
                break;
            case SIGN_IN:
                title.setText(R.string.setup_signin_title);
                body.setText(R.string.setup_signin_body);
                break;
            case CUSTOMIZE:
                title.setText(R.string.setup_customize_title);
                body.setText(R.string.setup_customize_body);
                break;
            case CHANGELOG_DISCORD:
                title.setText(R.string.setup_finish_title);
                body.setText(R.string.setup_finish_body);
                break;
            default:
                break;
        }
        primary.setText(step == SetupWizard.Step.CHANGELOG_DISCORD
                ? R.string.setup_finish : R.string.setup_next);
        back.setVisibility(step == SetupWizard.Step.LICENSE ? View.INVISIBLE : View.VISIBLE);
        skip.setVisibility(SetupWizard.canSkip(step) ? View.VISIBLE : View.GONE);
        updatePrimaryState();
    }

    private void updatePrimaryState() {
        SetupWizard.Facts facts = new SetupWizard.Facts().licenseAccepted(licenseAccepted);
        primary.setEnabled(SetupWizard.canProceed(step, facts));
        primary.setAlpha(primary.isEnabled() ? 1f : 0.5f);
    }

    /** Draws the progress line and the "Step N of 5" label from the pure step list. */
    private void renderRail() {
        int index = SetupWizard.index(step);
        stepLabel.setText(getString(R.string.setup_progress_of, index + 1, SetupWizard.STEPS.length));
        if (rail == null) return;
        rail.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int accent = new PersonalizationManager(this).getAccentColor();
        if (accent == 0) accent = getColor(R.color.primary);
        for (int i = 0; i < SetupWizard.STEPS.length; i++) {
            View dot = new View(this);
            int size = (int) ((i == index ? 10 : 8) * density);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins((int) (4 * density), 0, (int) (4 * density), 0);
            dot.setLayoutParams(lp);
            android.graphics.drawable.GradientDrawable shape =
                    new android.graphics.drawable.GradientDrawable();
            shape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            shape.setColor(i <= index ? accent : getColor(R.color.text_secondary));
            dot.setBackground(shape);
            rail.addView(dot);
        }
    }

    /**
     * Renders the device compatibility pass, which only means something once an instance exists, so
     * it is shown from the sign-in step onward and hidden before that. Gathering reads the disk and
     * opens the APK, so it runs off the UI thread; a generation counter drops a stale result.
     */
    private void renderCompat() {
        if (compatRows == null) return;
        View card = binding.setupCompat;
        if (card == null) return;

        boolean relevant = SetupWizard.index(step) >= SetupWizard.index(SetupWizard.Step.SIGN_IN);
        if (!relevant || !hasInstalledVersion()) {
            card.setVisibility(View.GONE);
            return;
        }
        card.setVisibility(View.VISIBLE);

        final int generation = ++compatGeneration;
        final org.chimeramc.client.core.versions.GameVersion selected =
                org.chimeramc.client.core.versions.VersionManager.get(this).getSelectedVersion();
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
            startActivity(new Intent(android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS));
        } catch (Throwable t) {
            // Not every OEM exposes the internal-storage screen; fall back to the app's own settings.
            openAppSettings();
        }
    }

    private void openAppSettings() {
        try {
            startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName())));
        } catch (Throwable t) {
            if (status != null) {
                status.setText(R.string.setup_compat_perm_blocking);
                status.setVisibility(View.VISIBLE);
            }
        }
    }

    // ---------------------------------------------------------------------------- actions

    private void onNext() {
        SetupWizard.Facts facts = new SetupWizard.Facts().licenseAccepted(licenseAccepted);
        if (!SetupWizard.canProceed(step, facts)) return;
        performStepAction(step);
        step = SetupWizard.next(step);
        render();
    }

    private void onBack() {
        step = SetupWizard.previous(step);
        render();
    }

    private void onSkip() {
        if (!SetupWizard.canSkip(step)) return;
        step = SetupWizard.next(step);
        render();
    }

    /** Performs the action a step's Next triggers; steps with no action simply advance. */
    private void performStepAction(SetupWizard.Step current) {
        switch (current) {
            case LICENSE:
                getSharedPreferences("LauncherPrefs", MODE_PRIVATE)
                        .edit().putBoolean("eula_accepted", true).apply();
                break;
            case INSTALL_INSTANCE:
                startImportPicker();
                break;
            case SIGN_IN:
                startActivity(new Intent(this, AccountsActivity.class));
                break;
            case CUSTOMIZE:
                startActivity(new Intent(this, CustomizeActivity.class));
                break;
            default:
                break;
        }
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

    private void openDiscord() {
        // UrlOpener reports a failure through AppLog; the screen only has to say so.
        if (!org.chimeramc.client.util.UrlOpener.open(this,
                getString(R.string.discord_join_url))) {
            if (status != null) {
                status.setText(R.string.discord_join_failed);
                status.setVisibility(View.VISIBLE);
            }
        }
    }

    private boolean hasInstalledVersion() {
        try {
            return !org.chimeramc.client.core.versions.VersionManager.get(this)
                    .getInstalledVersions().isEmpty();
        } catch (Throwable t) {
            return false;
        }
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
     * Whether the wizard should be shown on this launch: the user has not finished it before, and
     * there is no installed instance, so an existing user never sees it.
     */
    public static boolean shouldShow(Activity activity) {
        if (activity == null) return false;
        if (activity.getSharedPreferences("glowberry_setup", Activity.MODE_PRIVATE)
                .getBoolean(KEY_WIZARD_DONE, false)) {
            return false;
        }
        try {
            return org.chimeramc.client.core.versions.VersionManager.get(activity)
                    .getInstalledVersions().isEmpty();
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        compatExecutor.shutdownNow();
        if (apkImportManager != null) {
            apkImportManager.release();
            apkImportManager = null;
        }
        super.onDestroy();
    }
}

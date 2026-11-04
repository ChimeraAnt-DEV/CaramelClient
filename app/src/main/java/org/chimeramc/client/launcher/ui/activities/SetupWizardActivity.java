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
    /** How many instances the wizard imported this run, capped by SetupWizard.IMPORT_LIMIT. */
    private int importedThisRun;
    /** The list of instances shown on the install step, rebuilt when the set changes. */
    private LinearLayout instanceList;
    private View instanceSection;
    private TextView instanceEmpty;
    private Button instanceImport;
    private TextView instanceLimit;
    /** Whether the launcher-managed Microsoft sign-in is available; the sign-in step needs it. */
    private boolean signInAvailable;

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
        instanceList = binding.setupInstanceList;
        instanceSection = binding.setupInstanceSection;
        instanceEmpty = binding.setupInstanceEmpty;
        instanceImport = binding.setupInstanceImport;
        instanceLimit = binding.setupInstanceLimit;

        // The sign-in step can only do something when the launcher-managed Microsoft sign-in is on;
        // when it is off the Accounts screen shows a "feature disabled" placeholder, so the wizard
        // hides the step rather than sending the user to a dead end.
        signInAvailable = org.chimeramc.client.settings.FeatureSettings.getInstance()
                .isLauncherManagedMcLoginEnabled();

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
        if (instanceImport != null) {
            // Import straight from the step, rather than only as a side effect of Next. The old
            // flow opened the file picker when the user pressed Next, which meant the picker
            // appeared only after they had already moved on, and the step itself looked inert.
            instanceImport.setOnClickListener(v -> startImportPicker());
            DynamicAnim.applyPressScale(instanceImport);
        }
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
        apkImportManager.setOnImportCompleteListener(() -> {
            importedThisRun++;
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
        boolean install = step == SetupWizard.Step.INSTALL_INSTANCE;
        termsScroll.setVisibility(license ? View.VISIBLE : View.GONE);
        licenseCheck.setVisibility(license ? View.VISIBLE : View.GONE);
        finishActions.setVisibility(step == SetupWizard.Step.CHANGELOG_DISCORD
                ? View.VISIBLE : View.GONE);
        if (instanceSection != null) {
            instanceSection.setVisibility(install ? View.VISIBLE : View.GONE);
        }
        if (install) renderInstances();

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

    /**
     * Draws the install step's list of instances and its import button.
     *
     * <p>The list is what the step was missing: a user could import and never see what they had.
     * The button is gated by the wizard's own {@link SetupWizard#IMPORT_LIMIT}, and the remaining
     * count is stated so the limit is a rule the user can see rather than a surprise.
     */
    private void renderInstances() {
        if (instanceList == null) return;
        java.util.List<org.chimeramc.client.core.versions.GameVersion> installed;
        try {
            installed = org.chimeramc.client.core.versions.VersionManager.get(this)
                    .getInstalledVersions();
        } catch (Throwable t) {
            installed = java.util.Collections.emptyList();
        }
        if (installed == null) installed = java.util.Collections.emptyList();

        instanceList.removeAllViews();
        for (org.chimeramc.client.core.versions.GameVersion version : installed) {
            if (version == null) continue;
            TextView row = new TextView(this);
            String name = version.displayName != null && !version.displayName.isEmpty()
                    ? version.displayName
                    : (version.versionCode != null ? version.versionCode : version.directoryName);
            String code = version.versionCode != null ? version.versionCode : "";
            row.setText(getString(R.string.setup_instance_list_item, name, code));
            row.setTextColor(getColor(R.color.ui_text_primary));
            row.setTextAppearance(this, com.google.android.material.R.style
                    .TextAppearance_Material3_BodyMedium);
            row.setPadding(0, (int) (4 * getResources().getDisplayMetrics().density),
                    0, (int) (4 * getResources().getDisplayMetrics().density));
            instanceList.addView(row);
        }
        boolean empty = installed.isEmpty();
        if (instanceEmpty != null) instanceEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);

        boolean canImport = SetupWizard.canImportMore(importedThisRun);
        if (instanceImport != null) {
            instanceImport.setEnabled(canImport);
            instanceImport.setAlpha(canImport ? 1f : 0.5f);
        }
        if (instanceLimit != null) {
            if (canImport) {
                instanceLimit.setText(getString(R.string.setup_instance_limit_remaining,
                        SetupWizard.IMPORT_LIMIT - importedThisRun));
            } else {
                instanceLimit.setText(getString(R.string.setup_instance_limit_reached,
                        SetupWizard.IMPORT_LIMIT));
            }
            instanceLimit.setVisibility(View.VISIBLE);
        }
    }

    private void updatePrimaryState() {
        SetupWizard.Facts facts = new SetupWizard.Facts().licenseAccepted(licenseAccepted);
        primary.setEnabled(SetupWizard.canProceed(step, facts));
        primary.setAlpha(primary.isEnabled() ? 1f : 0.5f);
    }

    /**
     * Draws the progress line and the "Step N of M" label from the visible step list.
     *
     * <p>The count is the number of steps the user will actually see, so with the sign-in step
     * hidden the label reads "of 4" rather than "of 5" — a progress line that counts a step the user
     * can never reach is a small lie the spec asks us not to tell.
     */
    private void renderRail() {
        SetupWizard.Step[] visible = SetupWizard.visibleSteps(signInAvailable);
        int index = SetupWizard.visibleIndex(step, signInAvailable);
        if (index < 0) index = 0;
        stepLabel.setText(getString(R.string.setup_progress_of, index + 1, visible.length));
        if (rail == null) return;
        rail.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        int accent = new PersonalizationManager(this).getAccentColor();
        if (accent == 0) accent = getColor(R.color.primary);
        for (int i = 0; i < visible.length; i++) {
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

        boolean relevant = SetupWizard.visibleIndex(step, signInAvailable)
                >= SetupWizard.visibleIndex(SetupWizard.Step.INSTALL_INSTANCE, signInAvailable);
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
        step = SetupWizard.nextVisible(step, signInAvailable);
        render();
    }

    private void onBack() {
        step = SetupWizard.previousVisible(step, signInAvailable);
        render();
    }

    private void onSkip() {
        if (!SetupWizard.canSkip(step)) return;
        step = SetupWizard.nextVisible(step, signInAvailable);
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
                // The step has its own Import button, so Next only advances. Opening the picker
                // here is what made the file manager appear after the user had already moved on.
                break;
            case SIGN_IN:
                // Only reachable when the feature is on; a dead-end "disabled" screen is never
                // shown because the step is hidden otherwise.
                if (SetupWizard.signInAvailable(signInAvailable)) {
                    startActivity(new Intent(this, AccountsActivity.class));
                }
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

    /** The prefs file the wizard's completion flag lives in. */
    private static final String PREFS = "glowberry_setup";

    /**
     * Marks the wizard done and opens the launcher.
     *
     * <p>{@code commit()} rather than {@code apply()}: the flag decides whether the wizard is shown
     * on the next launch, and {@code apply()} writes asynchronously. Finishing the wizard and then
     * having the process torn down (or the game launched immediately) before the async write landed
     * is exactly how the wizard came back every launch — the user had completed it, but the flag
     * had never reached disk. The write is a few bytes, once, so the synchronous cost is nothing.
     */
    private void finishWizard() {
        markCompleted(this);
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }

    /** Writes the completion flag durably. Exposed so a test can pin that it is a commit. */
    static void markCompleted(Activity activity) {
        activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE)
                .edit().putBoolean(KEY_WIZARD_DONE, true).commit();
    }

    /**
     * Whether the wizard should be shown on this launch.
     *
     * <p>The rule is one thing: it has not been completed. It used to also require no installed
     * instance, so an existing user never saw it — but the wizard owns the licence, the account, the
     * theme and the changelog, so an installed instance is not a reason to skip it. The rule itself
     * lives in {@link SetupWizard#shouldShow(boolean)} so it is unit-tested.
     *
     * <p>It is shown only on the first run because completion is written with {@code commit()} (see
     * {@link #markCompleted}); an asynchronous write is what let it reappear on every launch.
     */
    public static boolean shouldShow(Activity activity) {
        if (activity == null) return false;
        boolean completed = activity.getSharedPreferences(PREFS, Activity.MODE_PRIVATE)
                .getBoolean(KEY_WIZARD_DONE, false);
        return SetupWizard.shouldShow(completed);
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

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
import org.chimeramc.client.util.ThemeManager;

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

    /** Instance-state keys, so a recreate (a live theme change) keeps the user on their page. */
    private static final String KEY_STEP = "wizard_step";
    private static final String KEY_LICENSE_ACCEPTED = "wizard_license_accepted";
    private static final String KEY_IMPORTED = "wizard_imported";

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
    private View themeSection;
    private android.widget.RadioGroup themeGroup;
    private android.widget.RadioButton themeSystem;
    private android.widget.RadioButton themeLight;
    private android.widget.RadioButton themeDark;
    private View changelogSection;
    private TextView changelogText;
    private boolean changelogRequested;
    private boolean suppressThemeRadioCallback;
    /** The step the pop-up entrance last played for, so a re-render does not replay it. */
    private SetupWizard.Step lastAnimatedStep;

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
        themeSection = binding.setupThemeSection;
        themeGroup = binding.setupThemeGroup;
        themeSystem = binding.setupThemeSystem;
        themeLight = binding.setupThemeLight;
        themeDark = binding.setupThemeDark;
        changelogSection = binding.setupChangelogSection;
        changelogText = binding.setupChangelogText;
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
        wireThemeRadios();
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

        restoreInstanceState(savedInstanceState);
        render();
    }

    /**
     * Restores the step and the progress bits a recreate would otherwise drop.
     *
     * <p>The customise step's theme radios recreate this activity (a live theme change); without
     * this the wizard snapped back to the licence page, which read as the last steps being broken.
     * The accepted licence is re-applied to the checkbox so the Next button stays lit, and the
     * per-run import count is restored so the limit is not silently reset.
     */
    private void restoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState == null) return;
        String stored = savedInstanceState.getString(KEY_STEP);
        if (stored != null) {
            try {
                step = SetupWizard.Step.valueOf(stored);
            } catch (IllegalArgumentException ignored) {
                step = SetupWizard.Step.LICENSE;
            }
        }
        licenseAccepted = savedInstanceState.getBoolean(KEY_LICENSE_ACCEPTED, false);
        importedThisRun = savedInstanceState.getInt(KEY_IMPORTED, 0);
        if (licenseCheck instanceof com.google.android.material.checkbox.MaterialCheckBox) {
            ((com.google.android.material.checkbox.MaterialCheckBox) licenseCheck)
                    .setChecked(licenseAccepted);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_STEP, step != null ? step.name() : SetupWizard.Step.LICENSE.name());
        outState.putBoolean(KEY_LICENSE_ACCEPTED, licenseAccepted);
        outState.putInt(KEY_IMPORTED, importedThisRun);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-assert that the popup has been opened, because a theme change (the customise step's
        // radios) recreates this activity and a fresh MainActivity would otherwise try to open a
        // second popup over the launcher.
        MainActivity.markSetupWizardLaunched();
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
        animateStepInIfChanged();
    }

    private void renderStepContent() {
        boolean license = step == SetupWizard.Step.LICENSE;
        boolean install = step == SetupWizard.Step.INSTALL_INSTANCE;
        boolean customize = step == SetupWizard.Step.CUSTOMIZE;
        boolean finish = step == SetupWizard.Step.CHANGELOG_DISCORD;

        // Each step shows exactly its own block and hides the rest, so the popup card is only ever
        // as tall as the page it is on — the size the user asked for. The theme and the changelog
        // are rendered inline rather than on a separate screen, which is what left the last two
        // steps looking empty: the content lived on another activity the wizard navigated to.
        termsScroll.setVisibility(license ? View.VISIBLE : View.GONE);
        licenseCheck.setVisibility(license ? View.VISIBLE : View.GONE);
        if (instanceSection != null) {
            instanceSection.setVisibility(install ? View.VISIBLE : View.GONE);
        }
        if (themeSection != null) {
            themeSection.setVisibility(customize ? View.VISIBLE : View.GONE);
        }
        if (changelogSection != null) {
            changelogSection.setVisibility(finish ? View.VISIBLE : View.GONE);
        }
        finishActions.setVisibility(finish ? View.VISIBLE : View.GONE);

        if (install) renderInstances();
        if (customize) syncThemeRadios();
        if (finish) loadChangelogInline();

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
        primary.setText(finish ? R.string.setup_finish : R.string.setup_next);
        back.setVisibility(license ? View.INVISIBLE : View.VISIBLE);
        skip.setVisibility(SetupWizard.canSkip(step) ? View.VISIBLE : View.GONE);
        updatePrimaryState();
    }

    /**
     * Applies the pop-up entrance to the card whenever the visible step changes.
     *
     * <p>The card scales up from a slightly smaller size on every step change, so moving through
     * the wizard reads as a page popping in rather than the content silently swapping. The first
     * render (and every plain re-render, such as returning from the file picker) is skipped so the
     * animation does not replay while the user is on the same page. {@code overshootScaleIn} honours
     * reduced motion, so the pop-up is skipped when animations are off.
     */
    private void animateStepInIfChanged() {
        if (step == lastAnimatedStep) return;
        lastAnimatedStep = step;
        View card = findViewById(R.id.setup_card);
        if (card != null) {
            DynamicAnim.overshootScaleIn(card, 0L);
        }
    }

    /** Wires the theme radios so picking one applies the theme live, in the popup itself. */
    private void wireThemeRadios() {
        if (themeGroup == null) return;
        themeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (suppressThemeRadioCallback) return;
            int mode = ThemeManager.MODE_FOLLOW_SYSTEM;
            if (checkedId == R.id.setup_theme_light) {
                mode = ThemeManager.MODE_LIGHT;
            } else if (checkedId == R.id.setup_theme_dark) {
                mode = ThemeManager.MODE_DARK;
            }
            // setThemeMode recreates the activity, so the new theme is visible on this screen at
            // once; the recreated popup re-syncs the radios from the stored mode.
            new ThemeManager(this).setThemeMode(mode);
        });
    }

    /** Points the radios at the stored theme mode without re-triggering the change listener. */
    private void syncThemeRadios() {
        if (themeGroup == null) return;
        suppressThemeRadioCallback = true;
        int mode = new ThemeManager(this).getCurrentMode();
        if (mode == ThemeManager.MODE_LIGHT) {
            themeLight.setChecked(true);
        } else if (mode == ThemeManager.MODE_DARK) {
            themeDark.setChecked(true);
        } else {
            themeSystem.setChecked(true);
        }
        suppressThemeRadioCallback = false;
    }

    /**
     * Fills the finish step's changelog text from the bundled feed.
     *
     * <p>Read once per wizard run and off the UI thread, because the asset is a file read; the
     * parsed headline is cached in {@link #changelogRequested} so re-rendering the step (a rotation,
     * a resume) does not re-read it.
     */
    private void loadChangelogInline() {
        if (changelogText == null || changelogRequested) return;
        changelogRequested = true;
        changelogText.setText(R.string.setup_finish_changelog_loading);
        final String fallback = getString(R.string.changelog_empty);
        org.chimeramc.client.util.AppExecutors.run(
                org.chimeramc.client.util.AppExecutors.Kind.DISK, "wizard-changelog", () -> {
            String line;
            try (java.io.InputStream in = getAssets().open("launcher/changelog_feed.json")) {
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                byte[] chunk = new byte[4096];
                int read;
                while ((read = in.read(chunk)) != -1) out.write(chunk, 0, read);
                line = org.chimeramc.client.core.changelog.ChangelogFeed
                        .parse(new String(out.toByteArray(), java.nio.charset.StandardCharsets.UTF_8))
                        .headlineLine();
            } catch (Throwable t) {
                line = "";
            }
            final String text = (line == null || line.isEmpty()) ? fallback : line;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed() || changelogText == null) return;
                changelogText.setText(text);
            });
        });
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
                // The theme picker is rendered inline in this popup, so Next only advances; the
                // radios apply the theme as they are tapped. Opening CustomizeActivity here is what
                // left the step looking empty — its content was on the next screen.
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
        // The wizard is a popup over the launcher, so finishing it simply returns to the launcher
        // behind it. When it is somehow the task root (no launcher behind), open one so the user is
        // not left on an empty screen.
        if (isTaskRoot()) {
            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            // The launcher will be created after this popup is gone, so clear the once-per-process
            // guard; otherwise it would never open (or would reopen) the overlay.
            MainActivity.resetSetupWizardLaunched();
        }
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

    /**
     * The wizard is a popup over the launcher, so it must not carry the launcher's own navigation
     * chrome (the tab bar / rail): showing it behind a modal card makes the popup read as a screen.
     * The card supplies its own Back, Skip and Next.
     */
    @Override
    protected boolean shouldSkipNavBar() {
        return true;
    }

    /** The popup owns its keys (the theme radios); the nav handler must not swallow them. */
    @Override
    protected boolean shouldHandleNavKeys() {
        return false;
    }

    /**
     * The system back gesture backs out of the wizard: it steps back one page, and on the first page
     * it closes the popup (marking the wizard done) so the user is not trapped. It never exits the
     * app, because the launcher is behind it.
     */
    @Override
    public void onBackPressed() {
        if (step != SetupWizard.Step.LICENSE) {
            onBack();
            return;
        }
        finishWizard();
    }


    @Override
    protected void onDestroy() {
        if (apkImportManager != null) {
            apkImportManager.release();
            apkImportManager = null;
        }
        super.onDestroy();
    }
}

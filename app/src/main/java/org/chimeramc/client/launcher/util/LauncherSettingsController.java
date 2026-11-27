package org.chimeramc.client.launcher.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import org.chimeramc.client.R;
import org.chimeramc.client.core.mods.inbuilt.manager.InbuiltModManager;
import org.chimeramc.client.settings.PerformancePresetManager;
import org.chimeramc.client.core.curseforge.CurseForgeKeyDialog;
import org.chimeramc.client.core.curseforge.CurseForgeKeyStore;
import org.chimeramc.client.preloader.SweetLoaderSignatureRulesManager;
import org.chimeramc.client.launcher.settings.SettingsRegistry;
import org.chimeramc.client.launcher.ui.activities.DiagnosticsActivity;
import org.chimeramc.client.ui.activities.MainActivity;
import org.chimeramc.client.settings.FeatureSettings;
import org.chimeramc.client.ui.animation.DynamicAnim;
import org.chimeramc.client.util.AppLog;
import org.chimeramc.client.util.GithubReleaseUpdater;
import org.chimeramc.client.util.LauncherFonts;
import org.chimeramc.client.util.LauncherSettingsBackup;
import org.chimeramc.client.util.LauncherStorage;
import org.chimeramc.client.util.LanguageManager;
import org.chimeramc.client.util.PersonalizationManager;
import org.chimeramc.client.util.ThemeManager;
import org.chimeramc.client.util.WorldBackupConfig;
import org.chimeramc.client.util.WorldBackupScheduler;
import org.chimeramc.client.util.WorldBackupStore;
import org.chimeramc.client.ui.compose.caramel.CaramelSettingFlag;
import org.chimeramc.client.ui.compose.caramel.CaramelSettingsActions;
import org.chimeramc.client.ui.compose.caramel.CaramelSettingsProvider;
import org.chimeramc.client.ui.compose.caramel.CaramelSettingsState;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * The settings panel's action side: one place where a settings control writes to the store that
 * owns the value.
 *
 * <p>Both surfaces that show the panel — the launcher's settings modal and the full-screen
 * {@code SettingsActivity} — hand their presses here, so a slider cannot be live on one surface and
 * dead on the other. The class owns the {@link CaramelSettingsState} it publishes, so a change is
 * written to the store and the panel's copy is rebuilt from the store rather than from the press.
 */
public final class LauncherSettingsController implements CaramelSettingsActions, CaramelSettingsProvider {

    private static final String TAG = "CaramelSettings";
    /** Developer mode is a launcher preference, not a module switch, so it lives on its own. */
    private static final String PREF_DEVELOPER = "caramel_developer_mode";

    private final Activity activity;
    private final PersonalizationManager personalization;
    private final ThemeManager themeManager;
    private final LanguageManager languageManager;
    private final CaramelSettingsState state = new CaramelSettingsState();
    private final List<String> fontKeys = new ArrayList<>();
    private final List<String> fontLabels = new ArrayList<>();
    private GithubReleaseUpdater updater;
    private ActivityResultLauncher<String> backgroundImageLauncher;
    private ActivityResultLauncher<Uri> worldBackupDestinationLauncher;
    private ActivityResultLauncher<String> settingsExportLauncher;
    private ActivityResultLauncher<String[]> settingsImportLauncher;

    public LauncherSettingsController(Activity activity) {
        this.activity = activity;
        this.personalization = new PersonalizationManager(activity);
        this.themeManager = new ThemeManager(activity);
        this.languageManager = new LanguageManager(activity);
        for (LauncherFonts.Entry entry : LauncherFonts.entries()) {
            fontKeys.add(entry.key);
            fontLabels.add(activity.getString(entry.labelRes));
        }
        registerBackgroundImagePicker();
        registerSettingsFileLaunchers();
        refresh();
    }

    /**
     * Registers the system image picker.
     *
     * <p>Registered in the constructor, before the host activity is started, which is the only point
     * the activity-result registry accepts it. A host that is not an {@link ActivityResultCaller}
     * (there is none today) simply has no picker rather than a crash.
     */
    private void registerBackgroundImagePicker() {
        if (!(activity instanceof ActivityResultCaller)) return;
        backgroundImageLauncher = ((ActivityResultCaller) activity).registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) return;
                    personalization.setBackgroundImage(uri, activity);
                    refresh();
                });
    }

    /**
     * Registers the folder picker and the settings export/import file pickers.
     *
     * <p>Registered in the constructor for the same reason as the image picker: the registry only
     * accepts a registration before the host is STARTED.
     */
    private void registerSettingsFileLaunchers() {
        if (!(activity instanceof ActivityResultCaller)) return;
        ActivityResultCaller caller = (ActivityResultCaller) activity;
        worldBackupDestinationLauncher = caller.registerForActivityResult(
                new ActivityResultContracts.OpenDocumentTree(),
                this::onWorldBackupDestinationPicked);
        settingsExportLauncher = caller.registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/json"),
                this::exportSettingsBackupTo);
        settingsImportLauncher = caller.registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                this::importSettingsBackupFrom);
    }

    /** The panel's state, owned here so a change survives the composition that made it. */
    public CaramelSettingsState state() {
        return state;
    }

    @Override
    public CaramelSettingsState settingsState() {
        return state;
    }

    @Override
    public CaramelSettingsActions settingsActions() {
        return this;
    }

    public List<String> fontKeys() {
        return fontKeys;
    }

    public List<String> fontLabels() {
        return fontLabels;
    }

    /** Rebuilds the panel's state from the stores. */
    public void refresh() {
        state.setThemeMode(themeManager.getCurrentMode());
        state.setDynamicColor(personalization.isDynamicColorEnabled());
        state.setFontFamilyKey(personalization.getFontFamily());
        state.setFontScale(personalization.getFontScale());
        state.setCardRounding(personalization.getCardRounding());
        state.setIconSize(personalization.getIconSize());
        state.setReduceMotion(personalization.isReduceMotion());
        state.setHaptics(personalization.isHapticFeedbackEnabled());
        state.setBlurIntensity(personalization.getBlurIntensity());
        state.setAnimationSpeed(personalization.getAnimationSpeed());
        state.setUiTransparency(personalization.getUiTransparency());
        state.setBackgroundImageSelected(personalization.hasBackgroundImage());
        state.setBackgroundImageBlur(personalization.getBackgroundImageBlur());
        state.setBackgroundImageBrightness(personalization.getBackgroundImageBrightness());
        state.setBackupRetention(FeatureSettings.getInstance().getBackupRetentionKeep());
        state.setPerformancePreset(PerformancePresetManager.current(activity).name());
        state.setCosmeticSync(InbuiltModManager.getInstance(activity).isCosmeticSyncEnabled());
        state.setSharedStorageLayout(LauncherStorage.isUsingNewSharedStorage(activity));
        refreshWorldBackup();
        refreshRuntimeDataStatus();
        refreshCurseForgeKey();
        refreshMigration();
        state.setLanguageTag(languageManager.getCurrentLanguage());
        state.setFlags(buildFlags());
        state.setDeveloperMode(activity.getSharedPreferences(PREF_DEVELOPER, android.content.Context.MODE_PRIVATE)
                .getBoolean("enabled", false));
        state.setUpdateAvailable(GithubReleaseUpdater.isUpdateAvailable(activity));
        state.setVersionName(installedVersionName());
    }

    /**
     * Every client switch, in registry order, with its translated label.
     *
     * <p>An action (a setting with no stored value) is skipped: it has nothing to toggle, and the
     * panel would draw a switch that cannot move.
     */
    private List<CaramelSettingFlag> buildFlags() {
        FeatureSettings features = FeatureSettings.getInstance();
        List<CaramelSettingFlag> flags = new ArrayList<>();
        for (SettingsRegistry.Setting setting : SettingsRegistry.all()) {
            if (setting.kind == SettingsRegistry.Kind.ACTION) continue;
            flags.add(new CaramelSettingFlag(
                    setting.id,
                    activity.getString(setting.titleRes),
                    activity.getString(setting.descriptionRes),
                    setting.value(features)));
        }
        return flags;
    }

    private String installedVersionName() {
        try {
            return activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public void setThemeMode(int mode) {
        themeManager.setThemeMode(mode);
        state.setThemeMode(themeManager.getCurrentMode());
    }

    @Override
    public void setDynamicColor(boolean enabled) {
        personalization.setDynamicColorEnabled(enabled);
        state.setDynamicColor(personalization.isDynamicColorEnabled());
    }

    @Override
    public void setFontFamilyKey(String key) {
        personalization.setFontFamily(key);
        state.setFontFamilyKey(personalization.getFontFamily());
    }

    @Override
    public void setFontScale(float scale) {
        personalization.setFontScale(scale);
        state.setFontScale(personalization.getFontScale());
    }

    @Override
    public void setCardRounding(int dp) {
        personalization.setCardRounding(dp);
        state.setCardRounding(personalization.getCardRounding());
    }

    @Override
    public void setIconSize(int percent) {
        personalization.setIconSize(percent);
        state.setIconSize(personalization.getIconSize());
    }

    @Override
    public void setReduceMotion(boolean reduce) {
        personalization.setReduceMotion(reduce);
        state.setReduceMotion(personalization.isReduceMotion());
    }

    @Override
    public void setHaptics(boolean enabled) {
        personalization.setHapticFeedbackEnabled(enabled);
        state.setHaptics(personalization.isHapticFeedbackEnabled());
    }

    @Override
    public void setBlurIntensity(int percent) {
        personalization.setBlurIntensity(percent);
        state.setBlurIntensity(personalization.getBlurIntensity());
    }

    @Override
    public void setAnimationSpeed(float speed) {
        personalization.setAnimationSpeed(speed);
        // Applied immediately as well as persisted, so the next dialog or press honours the new
        // speed without waiting for a restart.
        DynamicAnim.setGlobalSpeedMultiplier(personalization.getAnimationSpeed());
        state.setAnimationSpeed(personalization.getAnimationSpeed());
    }

    @Override
    public void setUiTransparency(int percent) {
        personalization.setUiTransparency(percent);
        state.setUiTransparency(personalization.getUiTransparency());
    }

    @Override
    public void pickBackgroundImage() {
        if (backgroundImageLauncher == null) {
            org.chimeramc.client.ui.compose.caramel.CaramelNoticeBus.post(activity, R.string.bg_image_picker_unavailable);
            return;
        }
        backgroundImageLauncher.launch("image/*");
    }

    @Override
    public void clearBackgroundImage() {
        personalization.clearBackgroundImage();
        refresh();
    }

    @Override
    public void setBackgroundImageBlur(int px) {
        personalization.setBackgroundImageBlur(px);
        state.setBackgroundImageBlur(personalization.getBackgroundImageBlur());
    }

    @Override
    public void setBackgroundImageBrightness(int percent) {
        personalization.setBackgroundImageBrightness(percent);
        state.setBackgroundImageBrightness(personalization.getBackgroundImageBrightness());
    }

    @Override
    public void setBackupRetention(int keep) {
        FeatureSettings.getInstance().setBackupRetentionKeep(keep);
        state.setBackupRetention(FeatureSettings.getInstance().getBackupRetentionKeep());
    }

    @Override
    public void setPerformancePreset(String name) {
        PerformancePresetManager.Preset preset;
        try {
            preset = PerformancePresetManager.Preset.valueOf(name);
        } catch (IllegalArgumentException e) {
            preset = PerformancePresetManager.Preset.BALANCED;
        }
        PerformancePresetManager.apply(activity, preset);
        state.setPerformancePreset(PerformancePresetManager.current(activity).name());
        // A preset writes the individual switches, so the flag list has to be re-read or the panel
        // would keep drawing the values from before the preset.
        state.setFlags(buildFlags());
    }

    @Override
    public void setCosmeticSync(boolean enabled) {
        InbuiltModManager.getInstance(activity).setCosmeticSyncEnabled(enabled);
        state.setCosmeticSync(InbuiltModManager.getInstance(activity).isCosmeticSyncEnabled());
    }

    @Override
    public void setSharedStorageLayout(boolean enabled) {
        LauncherStorage.setUseNewSharedStorage(activity, enabled);
        state.setSharedStorageLayout(LauncherStorage.isUsingNewSharedStorage(activity));
        org.chimeramc.client.ui.compose.caramel.CaramelNoticeBus.postWarning(activity, R.string.shared_storage_layout_changed);
    }

    @Override
    public void setWorldBackupEnabled(boolean enabled) {
        new WorldBackupConfig(activity).setEnabled(enabled);
        // The scheduler is the thing that makes the switch mean something: without the reschedule
        // the alarm is never set and the panel would show an "on" switch that backs up nothing.
        WorldBackupScheduler.reschedule(activity);
        refreshWorldBackup();
    }

    @Override
    public void setWorldBackupIntervalHours(int hours) {
        new WorldBackupConfig(activity).setIntervalHours(hours);
        WorldBackupScheduler.reschedule(activity);
        refreshWorldBackup();
    }

    @Override
    public void setWorldBackupRetention(int count) {
        new WorldBackupConfig(activity).setRetention(count);
        state.setWorldBackupRetention(new WorldBackupConfig(activity).getRetention());
    }

    @Override
    public void pickWorldBackupDestination() {
        if (worldBackupDestinationLauncher == null) {
            org.chimeramc.client.ui.compose.caramel.CaramelNoticeBus.post(activity, R.string.file_picker_unavailable);
            return;
        }
        worldBackupDestinationLauncher.launch(null);
    }

    @Override
    public void runWorldBackupNow() {
        WorldBackupScheduler.runNow(activity);
        org.chimeramc.client.ui.compose.caramel.CaramelNoticeBus.post(activity, R.string.settings_world_backup_back_up_now);
    }

    @Override
    public void refreshRuntimeData() {
        state.setRuntimeDataBusy(true);
        state.setRuntimeDataStatus(activity.getString(R.string.sweetloader_sigs_updating));
        SweetLoaderSignatureRulesManager.refreshNow(activity, result -> {
            state.setRuntimeDataBusy(false);
            refreshRuntimeDataStatus();
            if (result.success) {
                org.chimeramc.client.ui.compose.caramel.CaramelNoticeBus.post(
                        activity, R.string.sweetloader_sigs_update_success);
            } else {
                org.chimeramc.client.ui.compose.caramel.CaramelNoticeBus.postWarning(
                        activity, R.string.sweetloader_sigs_update_failed);
            }
        });
    }

    @Override
    public void editCurseForgeKey() {
        CurseForgeKeyDialog.show(activity, this::refreshCurseForgeKey);
    }

    @Override
    public void cleanHistoricalDirectory() {
        LauncherStorage.LegacyCleanupResult result = LauncherStorage.cleanupLegacyRoot(activity);
        if (result.success) {
            org.chimeramc.client.ui.compose.caramel.CaramelNoticeBus.post(activity,
                    activity.getString(R.string.migration_cleanup_success, result.deletedFiles,
                            org.chimeramc.client.core.setup.CompatibilityCheck.formatBytes(result.deletedBytes)));
        } else {
            org.chimeramc.client.ui.compose.caramel.CaramelNoticeBus.postWarning(activity,
                    activity.getString(R.string.migration_cleanup_failed, result.errorMessage));
        }
        refreshMigration();
    }

    @Override
    public void exportSettingsBackup() {
        if (settingsExportLauncher == null) {
            org.chimeramc.client.ui.compose.caramel.CaramelNoticeBus.post(activity, R.string.file_picker_unavailable);
            return;
        }
        settingsExportLauncher.launch("caramel-client-settings.json");
    }

    @Override
    public void importSettingsBackup() {
        if (settingsImportLauncher == null) {
            org.chimeramc.client.ui.compose.caramel.CaramelNoticeBus.post(activity, R.string.file_picker_unavailable);
            return;
        }
        settingsImportLauncher.launch(new String[]{"application/json"});
    }

    /** The world-backup block: the switch, the interval, the destination and the run status. */
    private void refreshWorldBackup() {
        WorldBackupConfig config = new WorldBackupConfig(activity);
        state.setWorldBackupEnabled(config.isEnabled());
        state.setWorldBackupIntervalHours(config.getIntervalHours());
        state.setWorldBackupRetention(config.getRetention());
        state.setWorldBackupReady(config.hasDestinationPermission());
        Uri uri = config.getFolderUri();
        state.setWorldBackupDestination(config.hasDestinationPermission() && uri != null
                ? WorldBackupStore.displayNameFromTreeUri(activity, uri)
                : activity.getString(R.string.settings_world_backup_destination_missing));
        state.setWorldBackupStatus(worldBackupStatusLine(config));
    }

    private String worldBackupStatusLine(WorldBackupConfig config) {
        if (!config.isEnabled()) return activity.getString(R.string.settings_world_backup_disabled);
        if (!config.hasDestinationPermission()) {
            return activity.getString(R.string.settings_world_backup_next_run_pending);
        }
        long next = WorldBackupConfig.nextRunAfter(config.getLastRunAt(), config.getIntervalHours());
        if (config.getLastRunAt() <= 0L || next <= System.currentTimeMillis()) {
            return activity.getString(R.string.settings_world_backup_next_run_pending);
        }
        String when = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT,
                Locale.getDefault()).format(new Date(next));
        return activity.getString(R.string.settings_world_backup_next_run, when);
    }

    /** The runtime-data row's "last update" line, read from the signature-rules manager. */
    private void refreshRuntimeDataStatus() {
        long last = SweetLoaderSignatureRulesManager.getLastSuccessfulUpdateTime(activity);
        state.setRuntimeDataStatus(last > 0L
                ? activity.getString(R.string.sweetloader_sigs_last_update,
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT,
                                Locale.getDefault()).format(new Date(last)))
                : activity.getString(R.string.sweetloader_sigs_never_updated));
    }

    /** The CurseForge key row: its status line and the button's own label. */
    private void refreshCurseForgeKey() {
        String key = CurseForgeKeyStore.getApiKey(activity);
        boolean has = key != null && !key.isEmpty();
        state.setCurseForgeKeyStatus(activity.getString(has
                ? R.string.curseforge_api_key_configured
                : R.string.curseforge_api_key_not_set, maskKey(key)));
        state.setCurseForgeKeyAction(activity.getString(
                has ? R.string.curseforge_api_key_change : R.string.curseforge_api_key_set));
    }

    /** Shows enough of the key to tell two of them apart without exposing it. */
    private static String maskKey(String key) {
        if (key == null || key.isEmpty()) return "";
        if (key.length() <= 8) return "\u2026";
        return key.substring(0, 4) + "\u2026" + key.substring(key.length() - 4);
    }

    /** The migration row: whether the historical directory can be cleaned, and the status line. */
    private void refreshMigration() {
        boolean migrationCompleted = LauncherStorage.isMigrationCompleted(activity);
        boolean legacyExists = LauncherStorage.getLegacyRoot().isDirectory();
        state.setMigrationCleanupAvailable(migrationCompleted && legacyExists);
        if (!migrationCompleted) {
            state.setMigrationStatus(activity.getString(R.string.migration_cleanup_unavailable_not_completed));
        } else if (!legacyExists) {
            state.setMigrationStatus(activity.getString(R.string.migration_cleanup_unavailable_missing));
        } else {
            state.setMigrationStatus(activity.getString(R.string.migration_cleanup_ready));
        }
    }

    /** Writes the export to the file the user picked. */
    private void exportSettingsBackupTo(Uri target) {
        if (target == null) return;
        try (java.io.OutputStream out = activity.getContentResolver().openOutputStream(target)) {
            if (out == null) return;
            String json = LauncherSettingsBackup.export(activity, true, true);
            out.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            out.flush();
            org.chimeramc.client.ui.compose.caramel.CaramelNoticeBus.post(activity, R.string.settings_backup_exported);
        } catch (Exception e) {
            AppLog.warnOnce(TAG, "LauncherSettingsController:export", "Settings export failed", e);
            org.chimeramc.client.ui.compose.caramel.CaramelNoticeBus.postWarning(activity, activity.getString(R.string.settings_backup_failed, e.getMessage()));
        }
    }

    /** Restores the settings from the file the user picked, then rebuilds the panel. */
    private void importSettingsBackupFrom(Uri source) {
        if (source == null) return;
        try (java.io.InputStream in = activity.getContentResolver().openInputStream(source)) {
            if (in == null) return;
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = in.read(chunk)) != -1) buffer.write(chunk, 0, read);
            LauncherSettingsBackup.ImportResult result =
                    LauncherSettingsBackup.importFrom(activity, buffer.toString("UTF-8"));
            if (result.success) {
                org.chimeramc.client.ui.compose.caramel.CaramelNoticeBus.post(activity,
                        activity.getString(R.string.settings_backup_imported, result.profileCount));
            } else {
                org.chimeramc.client.ui.compose.caramel.CaramelNoticeBus.postWarning(activity,
                        activity.getString(R.string.settings_backup_failed, result.message));
            }
            refresh();
        } catch (Exception e) {
            AppLog.warnOnce(TAG, "LauncherSettingsController:import", "Settings import failed", e);
            org.chimeramc.client.ui.compose.caramel.CaramelNoticeBus.postWarning(activity, activity.getString(R.string.settings_backup_failed, e.getMessage()));
        }
    }

    /** Grants and records the picked backup destination, then arms the schedule. */
    private void onWorldBackupDestinationPicked(Uri uri) {
        if (uri == null) return;
        try {
            activity.getContentResolver().takePersistableUriPermission(uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (SecurityException e) {
            AppLog.warnOnce(TAG, "LauncherSettingsController:destination",
                    "Ignored a persistable-permission failure", e);
        }
        WorldBackupConfig config = new WorldBackupConfig(activity);
        config.setFolderUri(uri);
        config.setEnabled(true);
        WorldBackupScheduler.reschedule(activity);
        refreshWorldBackup();
    }

    @Override
    public void setLanguageTag(String tag) {
        languageManager.setAppLanguage(tag);
        state.setLanguageTag(tag);
    }

    @Override
    public void setFlag(String id, boolean value) {
        SettingsRegistry.Setting setting = SettingsRegistry.find(id);
        if (setting == null) return;
        setting.apply(FeatureSettings.getInstance(), value);
        state.setFlags(buildFlags());
    }

    @Override
    /**
     * A manual press always answers, in the Compose modal the panel already declares: a progress
     * circle while the release request is in flight, then the verdict. The legacy alert is
     * suppressed so the same answer is not shown twice in two visual languages.
     */
    public void checkForUpdates() {
        if (updater == null) {
            updater = new GithubReleaseUpdater(activity, GithubReleaseUpdater.REPO_OWNER,
                    GithubReleaseUpdater.REPO_NAME, null);
        }
        state.setUpdateDialogOpen(true);
        state.setUpdateRunning(true);
        state.setUpdateStatus(activity.getString(R.string.caramel_checking_updates));
        state.setUpdateLatest("");
        state.setUpdateUrl("");
        updater.checkUpdate((outcome, latestVersion, downloadUrl, error) -> {
            state.setUpdateRunning(false);
            switch (outcome) {
                case UPDATE_AVAILABLE:
                    state.setUpdateAvailable(true);
                    state.setUpdateLatest(latestVersion);
                    state.setUpdateUrl(downloadUrl);
                    state.setUpdateStatus(activity.getString(
                            R.string.caramel_update_available_version, latestVersion));
                    break;
                case UP_TO_DATE:
                    state.setUpdateAvailable(false);
                    state.setUpdateLatest(latestVersion);
                    state.setUpdateStatus(activity.getString(
                            R.string.already_latest_version, latestVersion));
                    break;
                default:
                    state.setUpdateStatus(activity.getString(R.string.caramel_update_check_failed));
                    break;
            }
        }, false);
    }

    @Override
    public void dismissUpdateDialog() {
        if (state.getUpdateRunning()) return;
        state.setUpdateDialogOpen(false);
    }

    @Override
    public void openUpdateDownload() {
        String url = state.getUpdateUrl();
        if (url == null || url.isEmpty()) return;
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)));
        } catch (RuntimeException e) {
            AppLog.warnOnce(TAG, "update-download", "No browser could open the release page", e);
            org.chimeramc.client.ui.compose.caramel.CaramelNoticeBus.postWarning(activity,
                    activity.getString(R.string.caramel_update_check_failed));
        }
    }

    @Override
    public void setDeveloperMode(boolean enabled) {
        activity.getSharedPreferences(PREF_DEVELOPER, android.content.Context.MODE_PRIVATE)
                .edit().putBoolean("enabled", enabled).apply();
        state.setDeveloperMode(enabled);
    }

    @Override
    public void openDiagnostics() {
        activity.startActivity(new Intent(activity, DiagnosticsActivity.class));
    }

    @Override
    public void openChangelog() {
        // The changelog is the launcher's News tab; opening it there keeps one renderer for it
        // rather than a second copy of the feed.
        Intent intent = new Intent(activity, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_INITIAL_TAB, "News");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
    }

    @Override
    public void resetSettings() {
        personalization.resetAllCustomizations();
        SettingsRegistry.resetToDefaults(FeatureSettings.getInstance());
        refresh();
        org.chimeramc.client.ui.compose.caramel.CaramelNoticeBus.post(activity, R.string.caramel_settings_reset_done);
    }
}

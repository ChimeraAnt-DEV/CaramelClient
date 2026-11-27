package org.chimeramc.client.ui.compose.caramel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.chimeramc.client.R

/**
 * The launcher's settings, as one screen and one modal body.
 *
 * <p>The screen the settings tab opened was a placeholder that drew nothing, and the modal the
 * launcher's own gear opened held every control in `remember { mutableStateOf(...) }` — moving a
 * slider changed a number on screen and wrote nothing. Both surfaces now render this panel, and
 * every control calls back into a real store: [PersonalizationManager] for the look,
 * [org.chimeramc.client.util.ThemeManager] for the night mode, [LanguageManager] for the locale and
 * [org.chimeramc.client.settings.FeatureSettings] (through [org.chimeramc.client.launcher.settings.SettingsRegistry])
 * for the client switches.
 *
 * <p>The state lives in [CaramelSettingsState] and is owned by the activity, not the composition, so
 * a value the activity changed (a font the user picked) is the value the panel draws after a
 * recomposition rather than a local copy that drifts.
 */

/** The launcher's persisted settings, as the panel draws them. */
class CaramelSettingsState {
    /** One of [org.chimeramc.client.util.ThemeManager.MODE_FOLLOW_SYSTEM] / LIGHT / DARK. */
    var themeMode by mutableStateOf(0)
    var dynamicColor by mutableStateOf(false)
    /** The persisted font key; the label comes from [org.chimeramc.client.util.LauncherFonts]. */
    var fontFamilyKey by mutableStateOf("")
    var fontScale by mutableStateOf(1f)
    /** Card corner rounding, in dp (the store keeps the same unit and converts for pixels). */
    var cardRounding by mutableStateOf(16)
    var iconSize by mutableStateOf(100)
    var reduceMotion by mutableStateOf(false)
    var haptics by mutableStateOf(true)
    var blurIntensity by mutableStateOf(50)
    /** The motion speed multiplier (0.5..3.0) that scales every launcher animation. */
    var animationSpeed by mutableStateOf(1f)
    /** How opaque the glass cards are, 0..100. Lower lets the wallpaper through. */
    var uiTransparency by mutableStateOf(85)
    /** True when a custom launcher background image is stored. */
    var backgroundImageSelected by mutableStateOf(false)
    var backgroundImageBlur by mutableStateOf(0)
    var backgroundImageBrightness by mutableStateOf(100)
    /** How many instance backups to keep, 1..50. */
    var backupRetention by mutableStateOf(5)
    /** The chosen [org.chimeramc.client.launcher.settings.PerformancePresetManager.Preset] name. */
    var performancePreset by mutableStateOf("BALANCED")
    /** Whether equipped cosmetics are advertised to other Caramel users. */
    var cosmeticSync by mutableStateOf(true)
    /** Whether game data lives in the new shared content directory. */
    var sharedStorageLayout by mutableStateOf(false)

    /** Whether the scheduled world-backup feature is on. */
    var worldBackupEnabled by mutableStateOf(false)
    /** The backup interval in whole hours (1..168). */
    var worldBackupIntervalHours by mutableStateOf(24)
    /** How many backups of each world to keep. */
    var worldBackupRetention by mutableStateOf(5)
    /** The picked destination's display name, or the "no folder chosen" line. */
    var worldBackupDestination by mutableStateOf("")
    /** The next-run / last-result line under the schedule controls. */
    var worldBackupStatus by mutableStateOf("")
    /** True when the destination is granted, so "Back up now" can run. */
    var worldBackupReady by mutableStateOf(false)

    /** "Last update: <date>" for the SweetLoader runtime data. */
    var runtimeDataStatus by mutableStateOf("")
    /** Whether a runtime-data refresh is in flight, so the button can show it. */
    var runtimeDataBusy by mutableStateOf(false)

    /** The CurseForge key row: "Configured (abcd…)" or "No key configured". */
    var curseForgeKeyStatus by mutableStateOf("")
    /** The CurseForge key button's label: "Set key" or "Change". */
    var curseForgeKeyAction by mutableStateOf("")

    /** The migration section's status line. */
    var migrationStatus by mutableStateOf("")
    /** True when the historical directory can actually be cleaned. */
    var migrationCleanupAvailable by mutableStateOf(false)

    /** The persisted BCP-47 language tag. */
    var languageTag by mutableStateOf("")
    /**
     * Every client switch, in registry order, with its translated label and its live value.
     *
     * <p>The labels are resolved by the activity that owns the registry, because the registry holds
     * resource ids and the panel would otherwise need a second copy of the id-to-label mapping.
     */
    var flags by mutableStateOf<List<CaramelSettingFlag>>(emptyList())
    /** True when a newer release has been seen, so the Updates section can say so. */
    var updateAvailable by mutableStateOf(false)
    /** The installed version name, shown in About. */
    var versionName by mutableStateOf("")
    /** A status line for a long action (an update check); empty when idle. */
    var busyMessage by mutableStateOf("")

    /** Whether the update-check dialog is open. */
    var updateDialogOpen by mutableStateOf(false)
    /** True while the release request is in flight, so the dialog shows a progress circle. */
    var updateRunning by mutableStateOf(false)
    /** The dialog's status line: checking, up to date, available or failed. */
    var updateStatus by mutableStateOf("")
    /** The version the last check found, empty when it found none. */
    var updateLatest by mutableStateOf("")
    /** The release page for the found version, empty when there is nothing to open. */
    var updateUrl by mutableStateOf("")
    /** Whether the developer-only rows (the CurseForge key) are shown. */
    var developerMode by mutableStateOf(false)
}

/** One client switch, as the panel draws it: its id, its label, its description and its value. */
class CaramelSettingFlag(
    val id: String,
    val title: String,
    val desc: String,
    val value: Boolean,
)

/** Everything the settings panel can change. */
interface CaramelSettingsActions {
    fun setThemeMode(mode: Int)
    fun setDynamicColor(enabled: Boolean)
    fun setFontFamilyKey(key: String)
    fun setFontScale(scale: Float)
    fun setCardRounding(dp: Int)
    fun setIconSize(percent: Int)
    fun setReduceMotion(reduce: Boolean)
    fun setHaptics(enabled: Boolean)
    fun setBlurIntensity(percent: Int)
    fun setAnimationSpeed(speed: Float)
    fun setUiTransparency(percent: Int)
    fun pickBackgroundImage()
    fun clearBackgroundImage()
    fun setBackgroundImageBlur(px: Int)
    fun setBackgroundImageBrightness(percent: Int)
    fun setBackupRetention(keep: Int)
    fun setPerformancePreset(name: String)
    fun setCosmeticSync(enabled: Boolean)
    fun setSharedStorageLayout(enabled: Boolean)
    fun setWorldBackupEnabled(enabled: Boolean)
    fun setWorldBackupIntervalHours(hours: Int)
    fun setWorldBackupRetention(count: Int)
    fun pickWorldBackupDestination()
    fun runWorldBackupNow()
    fun refreshRuntimeData()
    fun editCurseForgeKey()
    fun cleanHistoricalDirectory()
    fun exportSettingsBackup()
    fun importSettingsBackup()
    fun setLanguageTag(tag: String)
    fun setFlag(id: String, value: Boolean)
    fun checkForUpdates()
    /** Closes the update dialog; refused while a check is still running. */
    fun dismissUpdateDialog()
    /** Opens the release page the last check found. */
    fun openUpdateDownload()
    fun setDeveloperMode(enabled: Boolean)
    fun openDiagnostics()
    fun openChangelog()
    fun resetSettings()
}

/** The language choices, in the order the picker shows them. */
private val languages = listOf(
    "en" to R.string.english,
    "zh-CN" to R.string.chinese,
    "fr" to R.string.french,
    "hi" to R.string.hindi,
    "id" to R.string.indonesian,
    "ja" to R.string.japanese,
    "pt" to R.string.portuguese,
    "ru" to R.string.russian,
    "es" to R.string.spanish,
    "tr" to R.string.turkish,
    "vi" to R.string.vietnamese,
)

/** A switch on the panel: its label, its description, its value and the setter that persists it. */
private class SwitchSpec(
    val title: String,
    val desc: String,
    val value: Boolean,
    val set: (Boolean) -> Unit,
)

/** One settings row: a title, its description and a trailing control. */
@Composable
private fun ToggleRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    SettingRow(title, desc) { CaramelToggle(checked, onChange) }
}

@Composable
private fun SectionHeader(title: String) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Text(title, style = T.Title)
        Spacer(Modifier.height(2.dp))
    }
}

/** The font keys and their translated labels, in catalogue order. */
@Composable
private fun fontCatalogue(): Pair<List<String>, List<String>> {
    // stringResource, not Context.getString: the label must be re-read when the configuration
    // (locale) changes, which a LocalContext read does not do.
    val entries = remember { org.chimeramc.client.util.LauncherFonts.entries() }
    val keys = remember(entries) { entries.map { it.key } }
    val labels = entries.map { stringResource(it.labelRes) }
    return keys to labels
}

/**
 * The settings body, shared by the full-screen [CaramelSettingsScreen] and the launcher's modal.
 */
@Composable
fun CaramelSettingsPanel(
    state: CaramelSettingsState,
    actions: CaramelSettingsActions,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
) {
    val (fontKeys, fontLabels) = fontCatalogue()
    var query by remember { mutableStateOf("") }
    val q = query.trim().lowercase()

    fun matches(title: String, desc: String): Boolean =
        q.isEmpty() || title.lowercase().contains(q) || desc.lowercase().contains(q)

    val body: @Composable () -> Unit = {
        val general = listOf(
            SwitchSpec(
                stringResource(R.string.reduce_motion),
                stringResource(R.string.reduce_motion_desc),
                state.reduceMotion,
                { v -> actions.setReduceMotion(v) },
            ),
            SwitchSpec(
                stringResource(R.string.haptic_feedback),
                stringResource(R.string.haptic_feedback_desc),
                state.haptics,
                { v -> actions.setHaptics(v) },
            ),
        )

        val themeLabels = listOf(
            stringResource(R.string.theme_follow_system),
            stringResource(R.string.theme_light),
            stringResource(R.string.theme_dark),
        )

        SectionHeader(stringResource(R.string.caramel_section_general))
        general.forEach { spec ->
            if (matches(spec.title, spec.desc)) ToggleRow(spec.title, spec.desc, spec.value, spec.set)
        }

        SectionHeader(stringResource(R.string.caramel_appearance))
        if (matches(stringResource(R.string.theme_title), stringResource(R.string.theme_title))) {
            SettingRow(stringResource(R.string.theme_title), null) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    themeLabels.forEachIndexed { i, label ->
                        FilterChip(label, state.themeMode == i, { actions.setThemeMode(i) })
                    }
                }
            }
        }
        if (matches(stringResource(R.string.dynamic_color), stringResource(R.string.dynamic_color_desc))) {
            ToggleRow(
                stringResource(R.string.dynamic_color),
                stringResource(R.string.dynamic_color_desc),
                state.dynamicColor,
            ) { actions.setDynamicColor(it) }
        }
        if (matches(stringResource(R.string.font_family), stringResource(R.string.font_family))) {
            SettingRow(stringResource(R.string.font_family), null) {
                CaramelSimpleDropdown(
                    state.fontFamilyKey,
                    fontLabels,
                    { label ->
                        val index = fontLabels.indexOf(label)
                        if (index >= 0 && index < fontKeys.size) actions.setFontFamilyKey(fontKeys[index])
                    },
                    width = 200,
                )
            }
        }
        if (matches(stringResource(R.string.font_scale), stringResource(R.string.font_scale))) {
            SliderRow(
                stringResource(R.string.font_scale),
                stringResource(R.string.font_scale_value, state.fontScale),
                (state.fontScale - 0.8f) / 0.7f,
                { actions.setFontScale(0.8f + it * 0.7f) },
            )
        }
        if (matches(stringResource(R.string.card_rounding), stringResource(R.string.card_rounding))) {
            SliderRow(
                stringResource(R.string.card_rounding),
                stringResource(R.string.card_rounding_value, state.cardRounding),
                state.cardRounding / 32f,
                { actions.setCardRounding((it * 32f).toInt()) },
            )
        }
        if (matches(stringResource(R.string.icon_size), stringResource(R.string.icon_size))) {
            SliderRow(
                stringResource(R.string.icon_size),
                stringResource(R.string.icon_size_value, state.iconSize),
                (state.iconSize - 50) / 100f,
                { actions.setIconSize((50 + it * 100f).toInt()) },
            )
        }
        if (matches(stringResource(R.string.blur_intensity), stringResource(R.string.blur_intensity))) {
            SliderRow(
                stringResource(R.string.blur_intensity),
                stringResource(R.string.blur_intensity_value, state.blurIntensity),
                state.blurIntensity / 100f,
                { actions.setBlurIntensity((it * 100f).toInt()) },
            )
        }
        if (matches(stringResource(R.string.animation_speed), stringResource(R.string.animation_speed))) {
            SliderRow(
                stringResource(R.string.animation_speed),
                stringResource(R.string.animation_speed_value, state.animationSpeed),
                (state.animationSpeed - 0.5f) / 2.5f,
                { actions.setAnimationSpeed(0.5f + it * 2.5f) },
            )
        }
        if (matches(stringResource(R.string.ui_transparency), stringResource(R.string.ui_transparency))) {
            SliderRow(
                stringResource(R.string.ui_transparency),
                stringResource(R.string.ui_transparency_value, state.uiTransparency),
                state.uiTransparency / 100f,
                { actions.setUiTransparency((it * 100f).toInt()) },
            )
        }

        // Background image: one row to pick or clear, and the two look controls that only apply
        // while an image is set, so the panel never offers a slider that does nothing.
        if (matches(stringResource(R.string.bg_image_title), stringResource(R.string.bg_image_desc))) {
            SettingRow(
                stringResource(R.string.bg_image_title),
                stringResource(
                    if (state.backgroundImageSelected) R.string.bg_image_selected
                    else R.string.bg_image_desc
                ),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlassButton(
                        stringResource(R.string.select_image),
                        actions::pickBackgroundImage,
                        height = 34,
                    )
                    if (state.backgroundImageSelected) {
                        GlassButton(
                            stringResource(R.string.clear),
                            actions::clearBackgroundImage,
                            danger = true,
                            height = 34,
                        )
                    }
                }
            }
        }
        if (state.backgroundImageSelected) {
            if (matches(stringResource(R.string.bg_image_blur), stringResource(R.string.bg_image_blur))) {
                SliderRow(
                    stringResource(R.string.bg_image_blur),
                    stringResource(R.string.bg_image_blur_value, state.backgroundImageBlur),
                    state.backgroundImageBlur / 25f,
                    { actions.setBackgroundImageBlur((it * 25f).toInt()) },
                )
            }
            if (matches(stringResource(R.string.bg_image_brightness), stringResource(R.string.bg_image_brightness))) {
                SliderRow(
                    stringResource(R.string.bg_image_brightness),
                    stringResource(R.string.bg_image_brightness_value, state.backgroundImageBrightness),
                    (state.backgroundImageBrightness - 1) / 149f,
                    { actions.setBackgroundImageBrightness((1 + it * 149f).toInt()) },
                )
            }
        }

        val languageTitle = stringResource(R.string.language)
        if (matches(languageTitle, languageTitle)) {
            SectionHeader(languageTitle)
            SettingRow(languageTitle, null) {
                val labels = languages.map { stringResource(it.second) }
                val current = languages.firstOrNull { it.first == state.languageTag }?.let {
                    stringResource(it.second)
                } ?: labels.first()
                CaramelSimpleDropdown(current, labels, { label ->
                    val index = labels.indexOf(label)
                    if (index >= 0) actions.setLanguageTag(languages[index].first)
                }, width = 200)
            }
        }

        // The performance preset sets several switches at once, so it lives in its own section
        // above them: the three choices are the store's own, and the note says what a preset is
        // (a starting point) rather than implying it pins the individual switches.
        val presetTitle = stringResource(R.string.performance_preset_title)
        if (matches(presetTitle, stringResource(R.string.performance_preset_note))) {
            SectionHeader(presetTitle)
            SettingRow(presetTitle, stringResource(R.string.performance_preset_note)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val presetNames = listOf(
                        "BATTERY" to stringResource(R.string.performance_preset_battery),
                        "BALANCED" to stringResource(R.string.performance_preset_balanced),
                        "PERFORMANCE" to stringResource(R.string.performance_preset_performance),
                    )
                    presetNames.forEach { (id, label) ->
                        FilterChip(label, state.performancePreset == id, { actions.setPerformancePreset(id) })
                    }
                }
            }
        }

        // Storage and retention. Both write to stores outside FeatureSettings, so they are drawn
        // here rather than carried in the registry's flag list.
        val storageTitle = stringResource(R.string.settings_tab_storage)
        if (matches(storageTitle, storageTitle)) {
            SectionHeader(storageTitle)
            if (matches(stringResource(R.string.backup_retention_title), stringResource(R.string.backup_retention_desc))) {
                SliderRow(
                    stringResource(R.string.backup_retention_title),
                    stringResource(R.string.backup_retention_value, state.backupRetention),
                    (state.backupRetention - 1) / 49f,
                    { actions.setBackupRetention((1 + it * 49f).toInt()) },
                )
            }
            if (matches(stringResource(R.string.shared_storage_layout_title), stringResource(R.string.shared_storage_layout_desc))) {
                ToggleRow(
                    stringResource(R.string.shared_storage_layout_title),
                    stringResource(R.string.shared_storage_layout_desc),
                    state.sharedStorageLayout,
                ) { actions.setSharedStorageLayout(it) }
            }
            if (matches(stringResource(R.string.settings_backup_title), stringResource(R.string.settings_backup_desc))) {
                SettingRow(
                    stringResource(R.string.settings_backup_title),
                    stringResource(R.string.settings_backup_desc),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GlassButton(
                            stringResource(R.string.settings_backup_export),
                            actions::exportSettingsBackup,
                            height = 34,
                        )
                        GlassButton(
                            stringResource(R.string.settings_backup_import),
                            actions::importSettingsBackup,
                            height = 34,
                        )
                    }
                }
                Text(stringResource(R.string.settings_backup_note), style = T.Body)
            }
        }

        // Scheduled world backups: the switch, the interval, the destination and "Back up now".
        // The status line under them is the same value the scheduler uses, so the panel cannot
        // claim a backup is scheduled when the alarm was never set.
        val worldBackupTitle = stringResource(R.string.settings_world_backup_title)
        if (matches(worldBackupTitle, stringResource(R.string.settings_world_backup_desc))) {
            SectionHeader(worldBackupTitle)
            SettingRow(worldBackupTitle, stringResource(R.string.settings_world_backup_desc)) {
                CaramelToggle(state.worldBackupEnabled, actions::setWorldBackupEnabled)
            }
            if (state.worldBackupEnabled) {
                if (matches(stringResource(R.string.settings_world_backup_interval), stringResource(R.string.settings_world_backup_interval))) {
                    SettingRow(stringResource(R.string.settings_world_backup_interval), null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val intervals = listOf(
                                24 to stringResource(R.string.settings_world_backup_interval_daily),
                                72 to stringResource(R.string.settings_world_backup_interval_3_days),
                                168 to stringResource(R.string.settings_world_backup_interval_weekly),
                            )
                            intervals.forEach { (hours, label) ->
                                FilterChip(
                                    label,
                                    state.worldBackupIntervalHours == hours,
                                    { actions.setWorldBackupIntervalHours(hours) },
                                )
                            }
                        }
                    }
                }
                if (matches(stringResource(R.string.settings_world_backup_destination), stringResource(R.string.settings_world_backup_destination))) {
                    SettingRow(
                        stringResource(R.string.settings_world_backup_destination),
                        state.worldBackupDestination,
                    ) {
                        GlassButton(
                            stringResource(R.string.settings_world_backup_destination_pick),
                            actions::pickWorldBackupDestination,
                            height = 34,
                        )
                    }
                }
                SliderRow(
                    stringResource(R.string.settings_world_backup_retention),
                    stringResource(R.string.backup_retention_value, state.worldBackupRetention),
                    (state.worldBackupRetention - 1) / 49f,
                    { actions.setWorldBackupRetention((1 + it * 49f).toInt()) },
                )
                SettingRow(stringResource(R.string.settings_world_backup_back_up_now), state.worldBackupStatus) {
                    GlassButton(
                        stringResource(R.string.settings_world_backup_back_up_now),
                        actions::runWorldBackupNow,
                        enabled = state.worldBackupReady,
                        height = 34,
                    )
                }
            } else {
                Text(stringResource(R.string.settings_world_backup_disabled), style = T.Body)
            }
        }

        // Runtime data and the CurseForge key: both live outside FeatureSettings, so they are
        // drawn here rather than carried as registry flags.
        if (matches(stringResource(R.string.sweetloader_sigs_title), stringResource(R.string.sweetloader_sigs_last_update))) {
            SectionHeader(stringResource(R.string.sweetloader_sigs_title))
            SettingRow(
                stringResource(R.string.sweetloader_sigs_update),
                state.runtimeDataStatus,
            ) {
                GlassButton(
                    stringResource(R.string.sweetloader_sigs_update),
                    actions::refreshRuntimeData,
                    enabled = !state.runtimeDataBusy,
                    height = 34,
                )
            }
        }
        // The key row is developer-only: the client ships its own default CurseForge key, so an
        // ordinary user never has to see or enter one.
        if (state.developerMode
            && matches(stringResource(R.string.curseforge_api_key_title), state.curseForgeKeyStatus)) {
            SettingRow(
                stringResource(R.string.curseforge_api_key_title),
                state.curseForgeKeyStatus,
            ) {
                GlassButton(state.curseForgeKeyAction, actions::editCurseForgeKey, height = 34)
            }
        }

        // Migration: the historical-directory cleanup, drawn only when there is something to say.
        val migrationTitle = stringResource(R.string.settings_tab_migration)
        if (matches(migrationTitle, migrationTitle)
            || matches(stringResource(R.string.migration_cleanup_title), stringResource(R.string.migration_cleanup_desc))) {
            SectionHeader(migrationTitle)
            SettingRow(
                stringResource(R.string.migration_cleanup_title),
                state.migrationStatus,
            ) {
                GlassButton(
                    stringResource(R.string.migration_cleanup_button),
                    actions::cleanHistoricalDirectory,
                    danger = true,
                    enabled = state.migrationCleanupAvailable,
                    height = 34,
                )
            }
        }

        val clientTitle = stringResource(R.string.caramel_section_client)
        val visibleFlags = state.flags.filter { matches(it.title, it.desc) }
        val cosmeticSyncTitle = stringResource(R.string.cosmetics_sync_setting_title)
        val showCosmeticSync = matches(cosmeticSyncTitle, stringResource(R.string.cosmetics_sync_setting_desc))
        if (visibleFlags.isNotEmpty() || showCosmeticSync) {
            SectionHeader(clientTitle)
            if (showCosmeticSync) {
                ToggleRow(
                    cosmeticSyncTitle,
                    stringResource(R.string.cosmetics_sync_setting_desc),
                    state.cosmeticSync,
                ) { actions.setCosmeticSync(it) }
            }
            visibleFlags.forEach { flag ->
                ToggleRow(flag.title, flag.desc, flag.value) { actions.setFlag(flag.id, it) }
            }
        }

        SectionHeader(stringResource(R.string.settings_tab_updates))
        if (matches(stringResource(R.string.caramel_menu_check_updates), stringResource(R.string.caramel_check_updates_desc))) {
            SettingRow(
                stringResource(R.string.caramel_menu_check_updates),
                stringResource(
                    if (state.updateAvailable) R.string.caramel_update_available
                    else R.string.caramel_check_updates_desc
                ),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (state.updateRunning) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = C.Caramel,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    GlassButton(
                        stringResource(R.string.check_update),
                        actions::checkForUpdates,
                        icon = Icons.Rounded.RocketLaunch,
                        enabled = !state.updateRunning,
                        height = 34,
                    )
                }
            }
        }

        SectionHeader(stringResource(R.string.settings_tab_about))
        if (matches(stringResource(R.string.caramel_version), stringResource(R.string.caramel_version))) {
            SettingRow(stringResource(R.string.caramel_version), null) {
                Text(state.versionName, style = T.Label.copy(color = C.Caramel, fontWeight = FontWeight.Medium))
            }
        }
        if (matches(stringResource(R.string.settings_diagnostics), stringResource(R.string.settings_diagnostics_desc))) {
            SettingRow(
                stringResource(R.string.settings_diagnostics),
                stringResource(R.string.settings_diagnostics_desc),
            ) {
                GlassButton(stringResource(R.string.settings_diagnostics_open), actions::openDiagnostics, icon = Icons.Rounded.Build, height = 34)
            }
        }
        if (matches(stringResource(R.string.settings_open_changelog), stringResource(R.string.settings_open_changelog))) {
            SettingRow(stringResource(R.string.settings_open_changelog), null) {
                GlassButton(stringResource(R.string.settings_open_changelog), actions::openChangelog, icon = Icons.Rounded.Article, height = 34)
            }
        }
        if (matches(stringResource(R.string.settings_developer_mode), stringResource(R.string.settings_developer_mode_desc))) {
            ToggleRow(
                stringResource(R.string.settings_developer_mode),
                stringResource(R.string.settings_developer_mode_desc),
                state.developerMode,
            ) { actions.setDeveloperMode(it) }
        }
        SettingRow(stringResource(R.string.settings_reset), stringResource(R.string.settings_reset_desc)) {
            GlassButton(
                stringResource(R.string.reset),
                actions::resetSettings,
                icon = Icons.Rounded.Refresh,
                danger = true,
                height = 34,
            )
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SearchField(query, { query = it }, stringResource(R.string.caramel_search), Modifier.fillMaxWidth())
        if (state.busyMessage.isNotEmpty()) {
            Text(state.busyMessage, style = T.Body.copy(color = C.Caramel))
        }
        if (scrollable) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) { body() }
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) { body() }
        }
    }
    CaramelUpdateDialog(state, actions)
}

/**
 * The update-check dialog.
 *
 * <p>Pressing "Check" used to run a request with no visible state: the button stayed live, nothing
 * said whether the check was running, and a found release was a line of text. This is the menu the
 * press should open — a progress circle while the release request is in flight, then the verdict
 * ("you are on the latest release" or the newer version) with a link to the release page.
 */
@Composable
private fun CaramelUpdateDialog(state: CaramelSettingsState, actions: CaramelSettingsActions) {
    if (!state.updateDialogOpen) return
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { if (!state.updateRunning) actions.dismissUpdateDialog() },
    ) {
        GlassPanel(Modifier.width(440.dp), RoundedCornerShape(22.dp), popup = true, elevation = 40.dp) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.caramel_menu_check_updates), style = T.Title)
                        Text(
                            stringResource(R.string.caramel_update_current, state.versionName),
                            style = T.Body,
                        )
                    }
                    GlassButton(
                        null,
                        { if (!state.updateRunning) actions.dismissUpdateDialog() },
                        icon = Icons.Rounded.Close,
                        enabled = !state.updateRunning,
                        height = 32,
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (state.updateRunning) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = C.Caramel,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(28.dp),
                        )
                    } else {
                        Icon(
                            if (state.updateUrl.isNotEmpty()) Icons.Rounded.RocketLaunch else Icons.Rounded.CheckCircle,
                            null,
                            Modifier.size(26.dp),
                            if (state.updateUrl.isNotEmpty()) C.Caramel else C.Green,
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            state.updateStatus.ifEmpty { stringResource(R.string.caramel_checking_updates) },
                            style = T.Body,
                        )
                        if (state.updateLatest.isNotEmpty() && !state.updateRunning) {
                            Text(
                                stringResource(R.string.caramel_update_latest, state.updateLatest),
                                style = T.Caption,
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (state.updateUrl.isNotEmpty() && !state.updateRunning) {
                        PrimaryButton(
                            stringResource(R.string.download_update),
                            actions::openUpdateDownload,
                            Modifier.weight(1f),
                            tone = Tone.Caramel,
                            icon = Icons.Rounded.RocketLaunch,
                            height = 40,
                        )
                    }
                    GlassButton(
                        stringResource(R.string.close),
                        { if (!state.updateRunning) actions.dismissUpdateDialog() },
                        Modifier.weight(1f),
                        enabled = !state.updateRunning,
                        height = 40,
                    )
                }
            }
        }
    }
}

/** The full-screen settings surface, mounted by `SettingsActivity`. */
@Composable
fun CaramelSettingsScreen(
    state: CaramelSettingsState,
    actions: CaramelSettingsActions,
    onClose: () -> Unit,
) {
    // The full-screen host has no launcher frame behind it, so it draws the same backdrop the
    // launcher does. Without it the panel's light text sat on the window background, which is the
    // near-white `@color/background` — the "white screen" the settings screen showed.
    Box(Modifier.fillMaxSize().background(C.Abyss)) {
        CaramelBackdrop()
        CaramelBackgroundImage()
        Column(
            Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_title), style = T.Display)
                    Text(stringResource(R.string.caramel_settings_sub), style = T.Body)
                }
                GlassButton(null, onClose, icon = Icons.Rounded.Close, height = 34)
            }
            CaramelSettingsPanel(state, actions, Modifier.weight(1f))
        }
    }
}

/**
 * The settings surface a host hands to the panel.
 *
 * <p>The launcher's gear and the full-screen settings activity both need a state and an action
 * receiver, and the action receiver is a Java class, so the pair travels as one object rather than
 * as two nullable parameters that could disagree.
 */
interface CaramelSettingsProvider {
    fun settingsState(): CaramelSettingsState
    fun settingsActions(): CaramelSettingsActions
}

/** The Java-facing mount for the settings screen, mirroring [CaramelLauncherHost]. */
object CaramelSettingsHost {
    @JvmStatic
    fun state(): CaramelSettingsState = CaramelSettingsState()

    @JvmStatic
    fun setContent(
        composeView: ComposeView,
        state: CaramelSettingsState,
        actions: CaramelSettingsActions,
        onClose: Runnable,
    ) {
        composeView.setContent {
            CaramelTheme {
                CaramelSettingsScreen(state, actions) { onClose.run() }
            }
        }
    }
}


package org.chimeramc.client.ui.compose.modmenu

import androidx.compose.runtime.Immutable

/**
 * The Mod Menu's four destinations, in the order the sidebar shows them.
 *
 * The menu used to be a nine-page rail where the page set was itself a bug surface (a module could
 * live on one page and not another). The rebuild is four destinations, and the module catalogue is
 * one list filtered inside the Modules destination rather than spread across pages.
 */
enum class ModMenuDestination(val labelRes: Int) {
    MODULES(org.chimeramc.client.R.string.mod_menu_tab_modules),
    COSMETICS(org.chimeramc.client.R.string.mod_menu_tab_cosmetics),
    SAVE(org.chimeramc.client.R.string.mod_menu_tab_save),
    SETTINGS(org.chimeramc.client.R.string.mod_menu_tab_settings),
}

/** One configurable entry of a module, flattened for the list UI. */
@Immutable
data class ModConfigRow(
    val key: String,
    val label: String,
    val value: String,
    val isToggle: Boolean,
)

/** One module row. [stableKey] is the identity the toggle and the favorites set key on. */
@Immutable
data class ModRow(
    val stableKey: String,
    val name: String,
    val description: String,
    val groupName: String,
    val enabled: Boolean,
    val favorite: Boolean,
    val hasConfig: Boolean,
    /** Non-null when the module cannot run in this build; the row is badged and its toggle refused. */
    val unavailableReason: String?,
    val configs: List<ModConfigRow>,
)

/** A saved configuration (a whole loadout the player can switch to at once). */
@Immutable
data class SavedLoadout(
    val id: String,
    val name: String,
    val enabledCount: Int,
)

/** One row of the Settings destination. */
@Immutable
data class SettingsRow(
    val key: String,
    val label: String,
    val value: String,
    val kind: SettingsKind,
)

enum class SettingsKind { TOGGLE, SLIDER, CHOICE }

/** The whole Mod Menu state. Rebuilt on the Java side and pushed in as one immutable value. */
@Immutable
data class ModMenuState(
    val destination: ModMenuDestination = ModMenuDestination.MODULES,
    val title: String = "",
    val rows: List<ModRow> = emptyList(),
    val loadouts: List<SavedLoadout> = emptyList(),
    val settings: List<SettingsRow> = emptyList(),
    val search: String = "",
    val favoritesOnly: Boolean = false,
    /** The glass tint the player chose, as an ARGB int; 0 means "use the theme surface". */
    val glassTint: Int = 0,
    val opacityPercent: Int = 25,
) {
    /** The rows after the search box and the favorites filter, which is what the list draws. */
    val visibleRows: List<ModRow>
        get() {
            val query = search.trim().lowercase()
            return rows.filter { row ->
                (!favoritesOnly || row.favorite) &&
                    (query.isEmpty() ||
                        row.name.lowercase().contains(query) ||
                        row.description.lowercase().contains(query) ||
                        row.groupName.lowercase().contains(query))
            }
        }

    val activeCount: Int get() = rows.count { it.enabled }

    // Java-facing helpers. The generated `copy` takes nine positional arguments, which is easy to
    // mis-order from Java; these name the one field each changes, so the overlay cannot transpose
    // two of them.
    fun withDestination(destination: ModMenuDestination): ModMenuState =
        copy(destination = destination)

    fun withSearch(search: String): ModMenuState = copy(search = search)

    fun withFavoritesOnly(favoritesOnly: Boolean): ModMenuState =
        copy(favoritesOnly = favoritesOnly)

    fun withRows(rows: List<ModRow>): ModMenuState = copy(rows = rows)

    fun withSettings(settings: List<SettingsRow>): ModMenuState = copy(settings = settings)

    fun withLoadouts(loadouts: List<SavedLoadout>): ModMenuState = copy(loadouts = loadouts)

    /** Rebuilds the row list from a fresh catalogue, keeping the destination and the filters. */
    fun withCatalogue(
        rows: List<ModRow>,
        loadouts: List<SavedLoadout>,
        settings: List<SettingsRow>,
    ): ModMenuState = copy(rows = rows, loadouts = loadouts, settings = settings)
}

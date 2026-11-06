package org.chimeramc.client.ui.compose.modmenu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Mod Menu's pure state rules: which rows the list shows for a search, the favorites filter and
 * the active count.
 *
 * These are the rules the old View menu implemented inline in a 3400-line class, so they could only
 * be checked by opening the menu on a device. They are pure here, so a filter regression fails a
 * unit test instead.
 */
class ModMenuStateTest {

    private fun row(
        key: String,
        name: String = key,
        description: String = "",
        group: String = "inbuilt",
        enabled: Boolean = false,
        favorite: Boolean = false,
    ) = ModRow(
        stableKey = key,
        name = name,
        description = description,
        groupName = group,
        enabled = enabled,
        favorite = favorite,
        hasConfig = false,
        unavailableReason = null,
        configs = emptyList(),
    )

    @Test
    fun `an empty search shows every row in order`() {
        val state = ModMenuState(rows = listOf(row("a"), row("b"), row("c")))
        assertEquals(listOf("a", "b", "c"), state.visibleRows.map { it.stableKey })
    }

    @Test
    fun `the search matches the name, the description and the group`() {
        val state = ModMenuState(
            rows = listOf(
                row("zoom", name = "Zoom"),
                row("crystal", description = "highlights a crystal spot"),
                row("mod", name = "Something", group = "pvp"),
            ),
        )
        assertEquals(listOf("zoom"), state.copy(search = "zoo").visibleRows.map { it.stableKey })
        assertEquals(
            listOf("crystal"),
            state.copy(search = "CRYSTAL").visibleRows.map { it.stableKey },
        )
        assertEquals(listOf("mod"), state.copy(search = "pvp").visibleRows.map { it.stableKey })
    }

    @Test
    fun `the search ignores surrounding whitespace`() {
        val state = ModMenuState(rows = listOf(row("zoom", name = "Zoom")))
        assertEquals(1, state.copy(search = "  zoom  ").visibleRows.size)
    }

    @Test
    fun `a search with no match yields an empty list, which the empty state keys on`() {
        val state = ModMenuState(rows = listOf(row("zoom", name = "Zoom")))
        assertTrue(state.copy(search = "nothing-like-this").visibleRows.isEmpty())
    }

    @Test
    fun `the favorites filter keeps only pinned rows`() {
        val state = ModMenuState(
            rows = listOf(row("a", favorite = true), row("b", favorite = false)),
        )
        val filtered = state.copy(favoritesOnly = true).visibleRows
        assertEquals(listOf("a"), filtered.map { it.stableKey })
    }

    @Test
    fun `favorites and the search combine`() {
        val state = ModMenuState(
            rows = listOf(
                row("zoom", name = "Zoom", favorite = true),
                row("zoom2", name = "Zoom Plus", favorite = false),
            ),
        )
        val filtered = state.copy(favoritesOnly = true, search = "zoom").visibleRows
        assertEquals(listOf("zoom"), filtered.map { it.stableKey })
    }

    @Test
    fun `the active count counts enabled rows, not visible rows`() {
        val state = ModMenuState(
            rows = listOf(
                row("a", enabled = true),
                row("b", enabled = false),
                row("c", enabled = true),
            ),
        )
        assertEquals(2, state.activeCount)
        // Filtering the list must not change the count, which is the whole catalogue's.
        assertEquals(2, state.copy(search = "a").activeCount)
    }

    @Test
    fun `a default state is the Modules destination at 25 percent opacity`() {
        val state = ModMenuState()
        assertEquals(ModMenuDestination.MODULES, state.destination)
        assertEquals(25, state.opacityPercent)
        assertFalse(state.favoritesOnly)
        assertTrue(state.rows.isEmpty())
    }

    @Test
    fun `the four destinations are the sidebar order`() {
        assertEquals(
            listOf(
                ModMenuDestination.MODULES,
                ModMenuDestination.COSMETICS,
                ModMenuDestination.SAVE,
                ModMenuDestination.SETTINGS,
            ),
            ModMenuDestination.entries.toList(),
        )
    }
}

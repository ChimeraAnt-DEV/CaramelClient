package org.chimeramc.client.core.curseforge

import org.chimeramc.client.ui.compose.caramel.CaramelCurseForgeItem
import org.chimeramc.client.ui.compose.caramel.CurseForgeSection
import org.chimeramc.client.ui.compose.caramel.sortedByDownloads
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The browse contract of the CurseForge tab: which class each section asks for, and the ranking.
 *
 * <p>The class ids are the ones the live API reports for `gameId=78022` (`isClass = true`):
 * Addons 4984, Maps 6913, Skins 6925, Texture Packs 6929, Scripts 6940. A transposed digit does not
 * fail loudly — it returns an empty or unrelated section — so they are pinned here.
 */
class CurseForgeSectionsTest {

    @Test
    fun `each section asks for the API's own Bedrock class`() {
        assertEquals(0, CurseForgeSection.ALL.classId)
        assertEquals(4984, CurseForgeSection.ADDONS.classId)
        assertEquals(6929, CurseForgeSection.TEXTURE_PACKS.classId)
        assertEquals(6940, CurseForgeSection.SCRIPTS.classId)
        assertEquals(6913, CurseForgeSection.WORLDS.classId)
        assertEquals(6925, CurseForgeSection.SKINS.classId)
    }

    @Test
    fun `a class id round-trips and an unknown one falls back to All`() {
        for (section in CurseForgeSection.entries) {
            assertEquals(section, CurseForgeSection.forClassId(section.classId))
        }
        assertEquals(CurseForgeSection.ALL, CurseForgeSection.forClassId(12345))
    }

    @Test
    fun `the browse URL carries the section and the download ranking`() {
        val url = CurseForgeClient.searchUrl(
            "", CurseForgeSection.TEXTURE_PACKS.classId, null, 0, 30,
            CurseForgeClient.SORT_TOTAL_DOWNLOADS, "desc",
        )
        assertEquals("78022", url.queryParameter("gameId"))
        assertEquals("6929", url.queryParameter("classId"))
        assertEquals("6", url.queryParameter("sortField"))
        assertEquals("desc", url.queryParameter("sortOrder"))
        assertNull("an empty query is a browse, not a filter", url.queryParameter("searchFilter"))
    }

    @Test
    fun `All sends no class filter and a query becomes a searchFilter`() {
        val url = CurseForgeClient.searchUrl(
            "bed wars", CurseForgeSection.ALL.classId, null, 0, 30,
            CurseForgeClient.SORT_TOTAL_DOWNLOADS, "desc",
        )
        assertNull(url.queryParameter("classId"))
        assertEquals("bed wars", url.queryParameter("searchFilter"))
    }

    @Test
    fun `results are ranked by downloads, highest first`() {
        val rows = listOf(item(1, 10), item(2, 900), item(3, 5_000), item(4, 900))
        assertEquals(listOf(3, 2, 4, 1), sortedByDownloads(rows).map { it.id })
    }

    private fun item(id: Int, downloads: Long) =
        CaramelCurseForgeItem(id, "name-$id", "", "", downloads, "https://media.forgecdn.net/x.png")
}

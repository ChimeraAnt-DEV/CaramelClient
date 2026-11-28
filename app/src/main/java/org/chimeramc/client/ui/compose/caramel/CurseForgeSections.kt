package org.chimeramc.client.ui.compose.caramel

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.chimeramc.client.R

/**
 * The catalogue sections the CurseForge tab browses, in the order the tab shows them.
 *
 * <p>The class ids are the API's own for Minecraft Bedrock (`gameId` 78022), read from
 * `/v1/categories?gameId=78022`: the entries with `isClass = true` are Addons 4984, Maps 6913 (shown as Worlds: a Bedrock
 * world is distributed through that class),
 * Skins 6925, Texture Packs 6929 and Scripts 6940. They are not guesses — a wrong id would return
 * another game's catalogue or nothing at all.
 *
 * <p>[ALL] carries id 0, which the client omits from the request, so it means "no class filter".
 * Bedrock content predates classes in some listings, and this is the section that shows it.
 */
enum class CurseForgeSection(val classId: Int, private val labelRes: Int) {
    ALL(0, R.string.caramel_curseforge_section_all),
    ADDONS(4984, R.string.caramel_curseforge_section_addons),
    TEXTURE_PACKS(6929, R.string.caramel_curseforge_section_texture_packs),
    SCRIPTS(6940, R.string.caramel_curseforge_section_scripts),
    WORLDS(6913, R.string.caramel_curseforge_section_worlds),
    SKINS(6925, R.string.caramel_curseforge_section_skins),
    ;

    @Composable
    fun label(): String = stringResource(labelRes)

    companion object {
        /** The section for [classId], or [ALL] for an id this build does not know. */
        fun forClassId(classId: Int): CurseForgeSection =
            entries.firstOrNull { it.classId == classId } ?: ALL
    }
}

/**
 * Sorts [items] by downloads, highest first, keeping the API's order for equal counts.
 *
 * <p>The request already asks for total downloads, descending, so this is a second guard:
 * the section list is what a person reads top to bottom, and a row out of order reads as a bug in
 * the section rather than in the ranking. Pure, so the ordering rule is unit-tested.
 */
fun sortedByDownloads(items: List<CaramelCurseForgeItem>): List<CaramelCurseForgeItem> =
    items.sortedByDescending { it.downloads }

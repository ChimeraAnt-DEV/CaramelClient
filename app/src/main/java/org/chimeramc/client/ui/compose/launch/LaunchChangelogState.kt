package org.chimeramc.client.ui.compose.launch

import androidx.compose.runtime.Immutable

/** One release note, with the optional link the note carries. */
@Immutable
data class ChangelogEntry(
    val text: String,
    val url: String = "",
) {
    val hasUrl: Boolean get() = url.isNotEmpty()
}

/**
 * The Launch tab's changelog, grouped into the three buckets a player reads.
 *
 * <p>[other] carries the notes whose section named none of the three kinds, so nothing is dropped: a
 * section that reads "Native route cleanup" is still shown, under "Also in this release".
 *
 * <p>Pure and immutable, so the card and the popup are a function of one value and the grouping can
 * be reviewed without a device.
 */
@Immutable
data class LaunchChangelogState(
    val version: String = "",
    val headline: String = "",
    val summary: String = "",
    val added: List<ChangelogEntry> = emptyList(),
    val changed: List<ChangelogEntry> = emptyList(),
    val fixed: List<ChangelogEntry> = emptyList(),
    val other: List<ChangelogEntry> = emptyList(),
) {
    /** True when there is nothing to show, so the card and the popup can hide rather than open blank. */
    val isEmpty: Boolean
        get() = added.isEmpty() && changed.isEmpty() && fixed.isEmpty() && other.isEmpty()

    /** The count of every note across the buckets, which the card shows as the size of the release. */
    val totalCount: Int
        get() = added.size + changed.size + fixed.size + other.size
}

package org.chimeramc.client.ui.compose.caramel

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How the launcher's screens rearrange themselves for the width they are given.
 *
 * <p>{@link UiResizeRule} scales the dp layout so it fits; this decides what the layout *is*. A
 * phone-width window gets one column, a landscape phone or small tablet two, and a large tablet the
 * three-up row the design was drawn for. That is the difference between "a phone app stretched over
 * a big screen" and a screen that uses the space it has.
 *
 * <p>Pure and unit-tested: every band is a number a test can assert, and the sizes here are the ones
 * Material's own window-size classes use (600dp compact-to-medium, 840dp medium-to-expanded), so a
 * window changes shape where Android developers already expect it to.
 */
object CaramelLayoutRule {

    /** The number of news cards that fit side by side at [widthDp]. */
    @JvmStatic
    fun newsColumns(widthDp: Int): Int = when {
        widthDp <= 0 -> 1
        widthDp < 560 -> 1
        widthDp < 840 -> 2
        else -> 3
    }

    /**
     * Whether Home keeps its hero beside the friends/promo column, or stacks it.
     *
     * <p>The side column is a fixed 374dp and the hero needs about 330dp to keep its art and the
     * launch button legible, so 720dp is the narrowest window both fit in side by side. A portrait
     * phone (360-430dp) is well below that and stacks; a landscape phone (~800dp here) keeps the two
     * columns, which is also the only arrangement that fits a 360dp-tall window without scrolling.
     */
    @JvmStatic
    fun homeSplitsSideBySide(widthDp: Int): Boolean = widthDp >= 720

    /** The width of Home's right-hand column, or null when the layout stacks instead. */
    @JvmStatic
    fun sideColumnWidth(widthDp: Int): Dp? = if (homeSplitsSideBySide(widthDp)) 374.dp else null

    /**
     * The widest the content is allowed to get.
     *
     * <p>A 1200dp tablet drawn full-bleed gives cards a 40-character line length and a hero that is
     * mostly empty; capping the content keeps the design's proportions and centres it.
     */
    @JvmStatic
    fun contentMaxWidth(widthDp: Int): Dp = when {
        widthDp >= 1400 -> 1200.dp
        widthDp >= 1000 -> 1000.dp
        else -> Dp.Unspecified
    }

    /** The gap between cards at [widthDp]: tighter on a phone, looser where there is room. */
    @JvmStatic
    fun gutter(widthDp: Int): Dp = when {
        widthDp < 400 -> 12.dp
        widthDp < 840 -> 16.dp
        else -> 20.dp
    }
}

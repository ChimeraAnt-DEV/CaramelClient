package org.chimeramc.client.ui.compose.caramel

/**
 * How much the launcher's dp layout is scaled for the window it is actually in.
 *
 * <p>The Caramel screens are laid out in fixed dp: a 320dp hero, two-column rows, 20dp gutters. On a
 * typical phone the shortest side is 360dp and that is what they were drawn at. On a narrow window -
 * split screen, a small phone, a low-resolution tablet that reports a small width - the columns
 * overflow and the layout reads as broken; on a large tablet the same dp layout reads as a phone app
 * stretched over a big screen.
 *
 * <p>The rule scales the *density*, not the font, so padding, radii and glyphs move together and
 * nothing ends up with text that outgrows its box. It is deliberately gentle and leaves the common
 * phone untouched: an automatic scale that changes the default look is a regression, not a fix.
 *
 * <p>Pure, so every band is unit-tested without a device or a screenshot.
 */
object UiResizeRule {

    /** The scale for a window whose shortest side is [shortestSideDp]. */
    @JvmStatic
    fun scaleFor(shortestSideDp: Int): Float = when {
        shortestSideDp <= 0 -> 1f
        shortestSideDp < 320 -> 0.88f
        shortestSideDp < 360 -> 0.95f
        shortestSideDp < 600 -> 1f
        shortestSideDp < 840 -> 1.06f
        else -> 1.12f
    }

    /** The scale for a window in dp, taking the shorter side. */
    @JvmStatic
    fun scaleFor(widthDp: Int, heightDp: Int): Float =
        scaleFor(minOf(widthDp, heightDp))

    /**
     * The same rule for a screenshot: the capture is in pixels, so the density is needed to report
     * the window the way the layout saw it.
     */
    @JvmStatic
    fun scaleForPixels(widthPx: Int, heightPx: Int, density: Float): Float {
        if (density <= 0f) return 1f
        return scaleFor((widthPx / density).toInt(), (heightPx / density).toInt())
    }
}

package org.chimeramc.client.ui.compose.caramel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The automatic UI scale leaves an ordinary phone alone.
 *
 * <p>An "auto" scale that quietly shrinks or grows the default device is a regression dressed as a
 * fix, so the band a normal phone falls in is pinned at exactly 1.0, and the extremes are pinned to
 * grow/shrink monotonically.
 */
class UiResizeRuleTest {

    @Test
    fun aTypicalPhoneIsUnchanged() {
        assertEquals(1f, UiResizeRule.scaleFor(360, 640), 0f)
        assertEquals(1f, UiResizeRule.scaleFor(411, 891), 0f)
        assertEquals(1f, UiResizeRule.scaleFor(599, 1024), 0f)
    }

    @Test
    fun aNarrowWindowShrinksAndATabletGrows() {
        assertEquals(0.95f, UiResizeRule.scaleFor(340, 700), 0f)
        assertEquals(0.88f, UiResizeRule.scaleFor(300, 600), 0f)
        assertEquals(1.06f, UiResizeRule.scaleFor(700, 1200), 0f)
        assertEquals(1.12f, UiResizeRule.scaleFor(1000, 1600), 0f)
    }

    @Test
    fun theShorterSideDecidesInEitherOrientation() {
        assertEquals(UiResizeRule.scaleFor(300, 900), UiResizeRule.scaleFor(900, 300), 0f)
        assertEquals(0.88f, UiResizeRule.scaleFor(900, 300), 0f)
    }

    @Test
    fun aMissingSizeFallsBackToNoScaling() {
        assertEquals(1f, UiResizeRule.scaleFor(0), 0f)
        assertEquals(1f, UiResizeRule.scaleFor(-10, -10), 0f)
        assertEquals(1f, UiResizeRule.scaleForPixels(1080, 2400, 0f), 0f)
    }

    @Test
    fun theScaleNeverLeavesTheSafeBand() {
        for (side in 0..3000 step 7) {
            val scale = UiResizeRule.scaleFor(side)
            assertTrue("scale $scale out of band at $side dp", scale in 0.85f..1.15f)
        }
    }

    @Test
    fun pixelsAreReadAtTheDevicesDensity() {
        // A 1080x2400 phone at 3x is 360x800dp: no scaling, even though the pixel count is large.
        assertEquals(1f, UiResizeRule.scaleForPixels(1080, 2400, 3f), 0f)
        // The same pixels on a 1x panel are a 1080dp window: a tablet-sized layout.
        assertEquals(1.12f, UiResizeRule.scaleForPixels(1080, 2400, 1f), 0f)
    }
}

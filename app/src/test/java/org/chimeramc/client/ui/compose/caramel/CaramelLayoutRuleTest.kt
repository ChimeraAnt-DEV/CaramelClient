package org.chimeramc.client.ui.compose.caramel

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The launcher's rearrangement rule, at the widths real devices report.
 *
 * <p>Every band is pinned: a window whose width falls on the wrong side of 600dp or 840dp is the
 * difference between a two-column tablet screen and a squeezed phone row, and nothing about that is
 * visible on a build machine.
 */
class CaramelLayoutRuleTest {

    @Test
    fun `a phone-width window keeps one news card per row`() {
        assertEquals(1, CaramelLayoutRule.newsColumns(360))
        assertEquals(1, CaramelLayoutRule.newsColumns(411))
        assertEquals(1, CaramelLayoutRule.newsColumns(559))
    }

    @Test
    fun `a landscape phone or small tablet fits two`() {
        assertEquals(2, CaramelLayoutRule.newsColumns(560))
        assertEquals(2, CaramelLayoutRule.newsColumns(800))
        assertEquals(2, CaramelLayoutRule.newsColumns(839))
    }

    @Test
    fun `a large tablet gets the three-up row the design was drawn for`() {
        assertEquals(3, CaramelLayoutRule.newsColumns(840))
        assertEquals(3, CaramelLayoutRule.newsColumns(1280))
    }

    @Test
    fun `home stacks below the split threshold and splits above it`() {
        // 374dp side column + 16dp gutter + a legible hero: 720dp is the narrowest window that holds
        // both, which is why a portrait phone stacks and a landscape phone (~800dp) does not.
        assertFalse(CaramelLayoutRule.homeSplitsSideBySide(411))
        assertFalse(CaramelLayoutRule.homeSplitsSideBySide(719))
        assertTrue(CaramelLayoutRule.homeSplitsSideBySide(720))
        assertTrue(CaramelLayoutRule.homeSplitsSideBySide(800))
        assertTrue(CaramelLayoutRule.homeSplitsSideBySide(1280))
        assertNull("a stacked layout has no side column", CaramelLayoutRule.sideColumnWidth(600))
        assertEquals(374.dp, CaramelLayoutRule.sideColumnWidth(800))
    }

    @Test
    fun `an unmeasured window is treated as a phone`() {
        assertEquals(1, CaramelLayoutRule.newsColumns(0))
        assertEquals(1, CaramelLayoutRule.newsColumns(-1))
        assertFalse(CaramelLayoutRule.homeSplitsSideBySide(0))
    }

    @Test
    fun `the content is capped only where a full-width layout would stretch`() {
        assertEquals(androidx.compose.ui.unit.Dp.Unspecified, CaramelLayoutRule.contentMaxWidth(411))
        assertEquals(1000.dp, CaramelLayoutRule.contentMaxWidth(1000))
        assertEquals(1200.dp, CaramelLayoutRule.contentMaxWidth(1600))
    }

    @Test
    fun `the gutter tightens as the window narrows`() {
        assertEquals(12.dp, CaramelLayoutRule.gutter(360))
        assertEquals(16.dp, CaramelLayoutRule.gutter(411))
        assertEquals(20.dp, CaramelLayoutRule.gutter(1280))
    }
}

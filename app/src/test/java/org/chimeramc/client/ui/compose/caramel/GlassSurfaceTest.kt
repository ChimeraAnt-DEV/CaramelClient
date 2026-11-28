package org.chimeramc.client.ui.compose.caramel

import androidx.compose.ui.graphics.toArgb
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The glass surface in the light palette is a tinted card, not a white rectangle.
 *
 * <p>Panels were filled with the palette's navy, which is pure white in the light palette, so on a
 * light page every card vanished into the background and the shop read as a field of white boxes.
 * The base fill is its own token now, and the transparency floor is a rule rather than a number
 * written inline in a composable.
 */
class GlassSurfaceTest {

    @After
    fun resetPalette() {
        C.light = false
    }

    @Test
    fun theLightPaletteFillsWithACoolSurface() {
        C.light = true
        val glass = C.Glass.toArgb()
        val navy = C.Navy.toArgb()
        val page = C.Abyss.toArgb()

        assertEquals("navy is the palette's white in light mode", 0xFFFFFFFF.toInt(), navy)
        assertNotEquals("a card must not be pure white", 0xFFFFFFFF.toInt(), glass)
        assertNotEquals("a card must not be the page colour", page, glass)
        val r = (glass shr 16) and 0xFF
        val g = (glass shr 8) and 0xFF
        val b = glass and 0xFF
        assertTrue("the surface leans cool: blue >= red", b >= r)
        assertTrue("it is still a light surface, not a dark one", r > 0xE0)
        assertTrue("green sits below blue by design", g <= b)
    }

    @Test
    fun theDarkPaletteKeepsTheNavy() {
        C.light = false
        assertEquals(C.Navy.toArgb(), C.Glass.toArgb())
    }

    @Test
    fun theFillStopsShortOfInvisible() {
        assertEquals(0.30f, glassFill(0f), 0.001f)
        assertEquals(1.0f, glassFill(1f), 0.001f)
        assertEquals("above 1 is clamped", 1.0f, glassFill(4f), 0.001f)
        assertEquals("below 0 is clamped", 0.30f, glassFill(-2f), 0.001f)
        assertTrue(glassFill(0.5f) > 0.30f)
    }
}

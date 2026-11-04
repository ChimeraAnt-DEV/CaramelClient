package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The cape atlas sampler maps the vanilla 64x32 sheet onto the cloth panel.
 *
 * <p>The preview drew the cape with the procedural weave only, so an authored cape — the whole point
 * of the hand-designed set — never appeared in the preview: the player picked artwork and saw a
 * template. This sampler reads the visible panel of the authored sheet, so the preview and the in-game
 * texture show the same art. Pure: it takes an int[] atlas and returns a colour, so it is unit-tested
 * without a device.
 */
public class CapeAtlasTest {

    /** A 64x32 atlas with a distinct colour per pixel, so a sample can be checked exactly. */
    private static int[] atlasByPosition() {
        int[] a = new int[64 * 32];
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 64; x++) {
                a[y * 64 + x] = 0xFF000000 | (x << 8) | y;
            }
        }
        return a;
    }

    @Test
    public void theSamplerReadsTheVisiblePanelNotTheWholeSheet() {
        int[] atlas = atlasByPosition();
        // u,v = 0,0 is the top-left of the visible panel, which is x=1,y=1 in the sheet.
        assertEquals("the top-left sample must come from the panel origin",
                0xFF000000 | (1 << 8) | 1,
                CapeAtlas.sample(atlas, 64, 32, 0f, 0f));
        // u,v = 1,1 is the bottom-right of the 10x16 panel: x=10, y=16.
        assertEquals("the bottom-right sample must come from the panel end",
                0xFF000000 | (10 << 8) | 16,
                CapeAtlas.sample(atlas, 64, 32, 1f, 1f));
    }

    @Test
    public void aNullOrShortAtlasFailsClosed() {
        assertEquals("no atlas must return transparent, never throw",
                0, CapeAtlas.sample(null, 64, 32, 0.5f, 0.5f));
        assertEquals(0, CapeAtlas.sample(new int[10], 64, 32, 0.5f, 0.5f));
        assertEquals(0, CapeAtlas.sample(new int[64 * 32], 0, 32, 0.5f, 0.5f));
    }

    @Test
    public void outOfRangeCoordinatesAreClampedToThePanel() {
        int[] atlas = atlasByPosition();
        assertEquals("a u below 0 clamps to the panel's left column",
                CapeAtlas.sample(atlas, 64, 32, -1f, 0.5f),
                CapeAtlas.sample(atlas, 64, 32, 0f, 0.5f));
        assertEquals("a v above 1 clamps to the panel's bottom row",
                CapeAtlas.sample(atlas, 64, 32, 0.5f, 2f),
                CapeAtlas.sample(atlas, 64, 32, 0.5f, 1f));
    }

    @Test
    public void thePanelConstantsMatchTheVanillaLayout() {
        // The visible panel is the box "front" face at x=1..10, y=1..16, per CapeTexturePainter.
        assertEquals(1, CapeAtlas.PANEL_X);
        assertEquals(1, CapeAtlas.PANEL_Y);
        assertEquals(10, CapeAtlas.PANEL_WIDTH);
        assertEquals(16, CapeAtlas.PANEL_HEIGHT);
    }

    @Test
    public void anAuthoredCapeSampledAcrossThePanelIsNotOneFlatColour() {
        // Every authored cape must have visible variation across the panel; a single flat colour is
        // the "capes look like trash" report, so it is pinned rather than left to the eye.
        int[] atlas = atlasByPosition();
        assertNotEquals("the panel must not sample as one colour",
                CapeAtlas.sample(atlas, 64, 32, 0.1f, 0.1f),
                CapeAtlas.sample(atlas, 64, 32, 0.9f, 0.9f));
        assertTrue(CapeAtlas.PANEL_WIDTH > 1 && CapeAtlas.PANEL_HEIGHT > 1);
    }
}

package org.chimeramc.client.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The rule that keeps a label legible on an arbitrary background (the launch screen's Back button).
 *
 * <p>The button tints its fill with the user's chosen accent. With a light accent and the theme's
 * white label the letters vanish into the fill — the "the Back button has no letters" report. The
 * fix is to choose the label from the fill, so these tests pin that choice rather than a colour.
 */
public class TextContrastTest {

    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;

    @Test
    public void aLightFillGetsBlackText() {
        assertEquals(BLACK, TextContrast.on(WHITE));
        assertEquals(BLACK, TextContrast.on(0xFF71ACEF));
    }

    @Test
    public void aDarkFillGetsWhiteText() {
        assertEquals(WHITE, TextContrast.on(BLACK));
        assertEquals(WHITE, TextContrast.on(0xFF0F5BE4));
    }

    @Test
    public void theChosenColourAlwaysMeetsTheAaThreshold() {
        // Every accent the picker can produce must end up with a label at 4.5:1 or better, or the
        // label is "there but unreadable", which is the same bug in a different shade.
        for (int c = 0; c < 256; c++) {
            int fill = 0xFF000000 | (c << 16) | ((255 - c) << 8) | c;
            int label = TextContrast.on(fill);
            assertTrue("fill " + Integer.toHexString(fill) + " label contrast",
                    TextContrast.contrastRatio(fill, label) >= 4.5);
        }
    }

    @Test
    public void aTransparentFillIsTreatedAsItsRgb() {
        // setBackgroundTintList may hand back an alpha-bearing colour; only the RGB decides the
        // label, because the label sits over whatever the tint composites onto.
        assertEquals(TextContrast.on(0xFF0F5BE4), TextContrast.on(0x800F5BE4));
    }
}

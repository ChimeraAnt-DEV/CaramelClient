package org.chimeramc.client.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The colour-literal parser a mod config uses (item 350).
 *
 * <p>The two config editors used to call {@code Color.parseColor} inside an empty catch, so a
 * malformed stored value silently became white and nothing recorded that the value was wrong. This
 * pins the parse as a rule: the accepted forms, the shorthand expansion, and that every malformed
 * shape returns the caller's fallback rather than throwing.
 */
public class HexColorTest {

    @Test
    public void aSixDigitLiteralIsOpaque() {
        assertEquals(0xFF1A2B3C, HexColor.parseOr("#1A2B3C", 0));
        assertEquals(0xFF1A2B3C, HexColor.parseOr("#1a2b3c", 0));
    }

    @Test
    public void anEightDigitLiteralKeepsItsAlpha() {
        assertEquals(0x801A2B3C, HexColor.parseOr("#801A2B3C", 0));
        assertEquals(0x00FFFFFF, HexColor.parseOr("#00FFFFFF", 0));
    }

    @Test
    public void aThreeDigitLiteralExpandsEachDigit() {
        // #F0A -> #FF00AA, opaque. Doubling, not zero-filling.
        assertEquals(0xFFFF00AA, HexColor.parseOr("#F0A", 0));
        assertEquals(0xFFFFFFFF, HexColor.parseOr("#FFF", 0));
        assertEquals(0xFF000000, HexColor.parseOr("#000", 0));
    }

    @Test
    public void surroundingWhitespaceIsTolerated() {
        assertEquals(0xFF1A2B3C, HexColor.parseOr("  #1A2B3C  ", 0));
    }

    @Test
    public void everyMalformedShapeReturnsTheFallback() {
        int fallback = 0xFF123456;
        assertEquals(fallback, HexColor.parseOr(null, fallback));
        assertEquals(fallback, HexColor.parseOr("", fallback));
        assertEquals(fallback, HexColor.parseOr("   ", fallback));
        assertEquals(fallback, HexColor.parseOr("1A2B3C", fallback));      // no leading '#'
        assertEquals(fallback, HexColor.parseOr("#", fallback));            // nothing after '#'
        assertEquals(fallback, HexColor.parseOr("#12", fallback));          // too short
        assertEquals(fallback, HexColor.parseOr("#12345", fallback));       // between 3 and 6
        assertEquals(fallback, HexColor.parseOr("#1234567", fallback));     // between 6 and 8
        assertEquals(fallback, HexColor.parseOr("#123456789", fallback));   // too long
        assertEquals(fallback, HexColor.parseOr("#GGGGGG", fallback));      // not hex
        assertEquals(fallback, HexColor.parseOr("#12345Z", fallback));      // one bad digit
        assertEquals(fallback, HexColor.parseOr("rgb(1,2,3)", fallback));   // a different syntax
    }

    @Test
    public void parseOrWhiteUsesWhiteAsTheFallback() {
        assertEquals(HexColor.WHITE, HexColor.parseOrWhite("not a colour"));
        assertEquals(0xFF1A2B3C, HexColor.parseOrWhite("#1A2B3C"));
    }

    @Test
    public void validityMatchesWhatParseAccepts() {
        assertTrue(HexColor.isValid("#FFF"));
        assertTrue(HexColor.isValid("#1A2B3C"));
        assertTrue(HexColor.isValid("#801A2B3C"));
        assertFalse(HexColor.isValid(null));
        assertFalse(HexColor.isValid("#12"));
        assertFalse(HexColor.isValid("1A2B3C"));
        assertFalse(HexColor.isValid("#GGGGGG"));
        assertFalse(HexColor.isValid("#123456789"));
    }
}

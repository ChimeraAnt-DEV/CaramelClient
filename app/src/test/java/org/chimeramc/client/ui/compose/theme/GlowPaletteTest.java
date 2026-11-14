package org.chimeramc.client.ui.compose.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.chimeramc.client.ui.components.UiTokens;
import org.junit.Test;

/**
 * The navy palette and the pack's blue toggle, pinned without a device.
 *
 * <p>These are the decisions the redesign rests on: the surfaces are the UI pack's navy ramp (never
 * neutral grey), the accent is the pack's blue, and the Compose tokens equal the View tokens so the
 * two layers cannot drift. A contrast floor is asserted too, so a future tweak cannot quietly produce
 * an unreadable theme.
 */
public class GlowPaletteTest {

    @Test
    public void theComposeTokensEqualTheViewTokens() {
        // GlowPalette (Compose) and UiTokens (View) are two files for one palette. A change to one
        // that is not made to the other is how the Compose hero and the View mod menu end up two
        // shades apart, so the equality is pinned rather than trusted to review.
        assertEquals(UiTokens.BG_SURFACE, GlowPalette.NAVY_SURFACE);
        assertEquals(UiTokens.BG_SURFACE_HOVER, GlowPalette.NAVY_SURFACE_RAISED);
        assertEquals(UiTokens.ACCENT_500, GlowPalette.SNOW_BLUE);
        assertEquals(UiTokens.ACCENT_400, GlowPalette.SNOW_BLUE_LIGHT);
        assertEquals(UiTokens.TEXT_PRIMARY, GlowPalette.TEXT_PRIMARY);
        assertEquals(UiTokens.TEXT_SECONDARY, GlowPalette.TEXT_SECONDARY);
        assertEquals(UiTokens.TEXT_DISABLED, GlowPalette.TEXT_DISABLED);
        assertEquals(UiTokens.TOGGLE_OFF_TRACK, GlowPalette.TOGGLE_TRACK_OFF);
        assertEquals(UiTokens.SUCCESS, GlowPalette.SUCCESS);
        assertEquals(UiTokens.WARNING, GlowPalette.WARNING);
        assertEquals(UiTokens.DANGER, GlowPalette.DANGER);
    }

    @Test
    public void theSurfaceRampGoesUpFromTheWindow() {
        assertTrue("the panel surface must be lighter than the window",
                UiTokens.luminance(GlowPalette.NAVY_SURFACE)
                        > UiTokens.luminance(GlowPalette.NAVY_WINDOW));
        assertTrue("a raised surface must be lighter than a panel surface",
                UiTokens.luminance(GlowPalette.NAVY_SURFACE_RAISED)
                        > UiTokens.luminance(GlowPalette.NAVY_SURFACE));
    }

    @Test
    public void theNavyRampIsBlueDominant() {
        for (int c : new int[]{GlowPalette.NAVY_WINDOW, GlowPalette.NAVY_SURFACE,
                GlowPalette.NAVY_SURFACE_RAISED, GlowPalette.NAVY_STROKE}) {
            int r = (c >>> 16) & 0xFF, g = (c >>> 8) & 0xFF, b = c & 0xFF;
            assertTrue("a navy surface must have blue as its strongest channel", b >= r && b >= g);
        }
    }

    @Test
    public void snowBlueIsBlueDominantAndLegibleOnNavy() {
        int r = (GlowPalette.SNOW_BLUE >>> 16) & 0xFF;
        int g = (GlowPalette.SNOW_BLUE >>> 8) & 0xFF;
        int b = GlowPalette.SNOW_BLUE & 0xFF;
        assertTrue("snow blue must be blue-dominant", b > r && b >= g);
        assertTrue("snow blue must read on the navy window (>= 3:1)",
                UiTokens.contrastRatio(GlowPalette.SNOW_BLUE, GlowPalette.NAVY_WINDOW) >= 3.0);
        assertTrue("primary text must read on the navy surface (>= 4.5:1)",
                UiTokens.contrastRatio(GlowPalette.TEXT_PRIMARY, GlowPalette.NAVY_SURFACE) >= 4.5);
    }

    @Test
    public void theOffTrackIsNavyNotGrey() {
        int c = GlowPalette.TOGGLE_TRACK_OFF;
        int r = (c >>> 16) & 0xFF, g = (c >>> 8) & 0xFF, b = c & 0xFF;
        assertTrue("the off track must stay in the navy family", b >= r && b >= g);
    }

    @Test
    public void theToggleOnTrackIsSnowBlueAndFollowsTheAccent() {
        // With no accent, the on-track is snow blue itself.
        assertEquals(GlowPalette.SNOW_BLUE, GlowPalette.toggleOnTrack(GlowPalette.SNOW_BLUE));
        // A custom accent tints it, but the result stays closer to snow blue than to the accent.
        int accent = 0xFF0F5BE4;
        int track = GlowPalette.toggleOnTrack(accent);
        assertNotEquals(accent, track);
        int distToSnow = channelDistance(track, GlowPalette.SNOW_BLUE);
        int distToAccent = channelDistance(track, accent);
        assertTrue("the on-track must stay recognisably snow blue", distToSnow < distToAccent);
    }

    @Test
    public void theToggleTrackAndThumbDifferByState() {
        assertNotEquals(GlowPalette.toggleTrack(true, GlowPalette.SNOW_BLUE),
                GlowPalette.toggleTrack(false, GlowPalette.SNOW_BLUE));
        assertNotEquals(GlowPalette.toggleThumb(true), GlowPalette.toggleThumb(false));
    }

    @Test
    public void theThreeSchemesAreDistinctAndOrdered() {
        GlowPalette.Scheme dark = GlowPalette.scheme(false, false);
        GlowPalette.Scheme amoled = GlowPalette.scheme(false, true);
        GlowPalette.Scheme light = GlowPalette.scheme(true, false);

        assertEquals(GlowPalette.AMOLED_WINDOW, amoled.window);
        assertEquals(GlowPalette.NAVY_WINDOW, dark.window);
        // A light theme is never AMOLED, even when both flags are set.
        assertEquals(GlowPalette.scheme(true, false).window, GlowPalette.scheme(true, true).window);

        assertTrue("the dark navy window is darker than the light one",
                UiTokens.luminance(dark.window) < UiTokens.luminance(light.window));
        assertTrue("AMOLED is the darkest window",
                UiTokens.luminance(amoled.window) <= UiTokens.luminance(dark.window));
    }

    @Test
    public void mixClampsAndBlends() {
        assertEquals(0xFF000000, GlowPalette.mix(0xFF000000, 0xFFFFFFFF, 0f));
        assertEquals(0xFFFFFFFF, GlowPalette.mix(0xFF000000, 0xFFFFFFFF, 1f));
        assertEquals(0xFF7F7F7F, GlowPalette.mix(0xFF000000, 0xFFFFFFFF, 0.5f));
        // Out-of-range amounts clamp rather than wrap.
        assertEquals(0xFF000000, GlowPalette.mix(0xFF000000, 0xFFFFFFFF, -1f));
        assertEquals(0xFFFFFFFF, GlowPalette.mix(0xFF000000, 0xFFFFFFFF, 2f));
    }

    private static int channelDistance(int a, int b) {
        int dr = ((a >>> 16) & 0xFF) - ((b >>> 16) & 0xFF);
        int dg = ((a >>> 8) & 0xFF) - ((b >>> 8) & 0xFF);
        int db = (a & 0xFF) - (b & 0xFF);
        return Math.abs(dr) + Math.abs(dg) + Math.abs(db);
    }
}

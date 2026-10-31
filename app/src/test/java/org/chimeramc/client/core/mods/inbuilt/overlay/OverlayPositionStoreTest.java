package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Overlay positions stored as surface fractions (item 211). */
public class OverlayPositionStoreTest {

    @Test
    public void fractionIsThePixelOverTheSurface() {
        assertEquals(0.5f, OverlayPositionStore.fraction(960, 1920), 0.0001f);
        assertEquals(0.25f, OverlayPositionStore.fraction(270, 1080), 0.0001f);
        assertEquals(0f, OverlayPositionStore.fraction(0, 1920), 0.0001f);
        assertEquals(1f, OverlayPositionStore.fraction(1920, 1920), 0.0001f);
    }

    @Test
    public void aZeroSurfaceIsZeroRatherThanADivideByZero() {
        assertEquals(0f, OverlayPositionStore.fraction(500, 0), 0.0001f);
        assertEquals(0f, OverlayPositionStore.fraction(500, -10), 0.0001f);
    }

    @Test
    public void aFractionOutsideTheRangeIsClamped() {
        assertEquals(0f, OverlayPositionStore.fraction(-400, 1920), 0.0001f);
        assertEquals(1f, OverlayPositionStore.fraction(5000, 1920), 0.0001f);
    }

    @Test
    public void pixelRoundTripsTheFraction() {
        assertEquals(960, OverlayPositionStore.pixel(0.5f, 1920));
        assertEquals(270, OverlayPositionStore.pixel(0.25f, 1080));
        assertEquals(0, OverlayPositionStore.pixel(0f, 1920));
        assertEquals(1920, OverlayPositionStore.pixel(1f, 1920));
    }

    @Test
    public void aFractionSurvivesAResolutionScaleChange() {
        // Saved at 1920 wide, half way across; the same widget stays half way across at 1280.
        float f = OverlayPositionStore.fraction(960, 1920);
        assertEquals(640, OverlayPositionStore.pixel(f, 1280));
    }

    @Test
    public void aNaNOrBadSurfaceCannotProduceAPosition() {
        assertEquals(0, OverlayPositionStore.pixel(Float.NaN, 1920));
        assertEquals(0, OverlayPositionStore.pixel(0.5f, 0));
        assertEquals(0, OverlayPositionStore.pixel(-1f, 1920));
        assertEquals(1920, OverlayPositionStore.pixel(2f, 1920));
    }

    @Test
    public void validityRejectsNonFiniteAndOutOfRangeValues() {
        assertTrue(OverlayPositionStore.isValid(0f));
        assertTrue(OverlayPositionStore.isValid(1f));
        assertTrue(OverlayPositionStore.isValid(0.42f));
        assertFalse(OverlayPositionStore.isValid(-0.01f));
        assertFalse(OverlayPositionStore.isValid(1.01f));
        assertFalse(OverlayPositionStore.isValid(Float.NaN));
        assertFalse(OverlayPositionStore.isValid(Float.POSITIVE_INFINITY));
    }
}

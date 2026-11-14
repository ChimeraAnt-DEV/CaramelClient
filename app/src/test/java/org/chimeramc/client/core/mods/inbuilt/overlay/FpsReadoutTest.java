package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the FPS readout's source rule: the game's own exact frame rate is shown when available; an
 * inferred game rate and the launcher's own refresh cadence are both labelled so neither is passed
 * off as the game's exact rate. Pure, so the rule is verified without a device.
 */
public class FpsReadoutTest {

    @Test
    public void theExactGameRateIsPreferredAndUnmarked() {
        FpsReadout readout = FpsReadout.of(72, 60);
        assertEquals(72, readout.value);
        assertEquals(FpsReadout.Source.GAME, readout.source);
        assertFalse(readout.approximate());
        assertTrue(readout.hasReading());
        assertEquals("72 FPS", readout.text());
    }

    @Test
    public void anInferredGameRateIsMarkedApproximateButStillTheGamesOwn() {
        FpsReadout readout = FpsReadout.of(58, false, 60);
        assertEquals(58, readout.value);
        assertEquals(FpsReadout.Source.GAME_APPROXIMATE, readout.source);
        assertTrue(readout.approximate());
        assertFalse("an inferred game rate is not the launcher's refresh cadence",
                readout.isHostRefresh());
        assertEquals("~58 FPS", readout.text());
    }

    @Test
    public void theLauncherCadenceIsNeverShownUnderTheFpsUnit() {
        // The launcher's own Choreographer cadence is not the game's frame rate. It is exposed
        // separately so a surface can name it, but the readout's text (the value that sits under an
        // "FPS" unit) must read as "no reading" rather than as a bare "~60 FPS", which is the
        // fabricated-frame bug the marker alone did not fix.
        FpsReadout readout = FpsReadout.of(0, true, 60);
        assertEquals(60, readout.value);
        assertEquals(FpsReadout.Source.HOST_REFRESH, readout.source);
        assertTrue(readout.approximate());
        assertTrue(readout.isHostRefresh());
        assertEquals("the launcher cadence has no game rate behind it", 0, readout.gameValue());
        assertEquals(60, readout.launcherRefreshHz());
        assertEquals(ModStatsFormatter.NO_READING, readout.text());
    }

    @Test
    public void onlyTheGamesOwnRateIsExposedAsTheGameValue() {
        assertEquals(72, FpsReadout.of(72, true, 0).gameValue());
        assertEquals(58, FpsReadout.of(58, false, 60).gameValue());
        assertEquals("an inferred game rate is still the game's own", 58,
                FpsReadout.of(58, false, 60).gameValue());
        // The launcher cadence never leaks into the game value, even when it is the headline.
        assertEquals(0, FpsReadout.of(0, true, 60).gameValue());
        assertEquals(0, FpsReadout.of(-1, true, 48).gameValue());
        // And the launcher refresh is only reported when it is the headline.
        assertEquals(0, FpsReadout.of(72, true, 60).launcherRefreshHz());
    }

    @Test
    public void anInferredGameRateBeatsTheLauncherCadence() {
        // A game-path reading, even inferred, is closer to the game's frame rate than the launcher's
        // own vsync cadence, so it is preferred — and still marked.
        FpsReadout readout = FpsReadout.of(40, false, 60);
        assertEquals(FpsReadout.Source.GAME_APPROXIMATE, readout.source);
        assertEquals(40, readout.value);
    }

    @Test
    public void aNegativeGameReadingFallsBackToTheLauncherCadence() {
        // GameFrameRate.UNKNOWN is -1; it must not be shown as a real number.
        FpsReadout readout = FpsReadout.of(-1, true, 48);
        assertEquals(48, readout.value);
        assertEquals(FpsReadout.Source.HOST_REFRESH, readout.source);
        assertTrue(readout.approximate());
    }

    @Test
    public void noReadingFromAnySourceIsTheEmDash() {
        FpsReadout readout = FpsReadout.of(0, true, 0);
        assertFalse(readout.hasReading());
        assertEquals(FpsReadout.Source.NONE, readout.source);
        assertEquals(ModStatsFormatter.NO_READING, readout.text());
    }

    @Test
    public void anUnmarkedReadingIsAlwaysTheExactGameRate() {
        // The whole point: a reading that is not marked approximate must be the game's exact rate.
        for (int host = 1; host <= 240; host++) {
            FpsReadout readout = FpsReadout.of(0, true, host);
            assertTrue("host cadence must always be marked approximate", readout.approximate());
            assertEquals(FpsReadout.Source.HOST_REFRESH, readout.source);
        }
        FpsReadout inferred = FpsReadout.of(50, false, 0);
        assertTrue("an inferred game rate must be marked approximate", inferred.approximate());
    }
}

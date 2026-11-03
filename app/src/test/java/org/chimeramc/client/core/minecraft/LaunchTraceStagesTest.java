package org.chimeramc.client.core.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

/**
 * Pins the launch-trace stage arithmetic.
 *
 * <p>The subtle rule is that a stage's cost is the gap to the previous stage, not its elapsed time:
 * the last stage's elapsed time is the whole launch, and reporting that as the stage's own duration
 * would always blame the final step. These tests fail if that ever regresses.
 */
public class LaunchTraceStagesTest {

    @Test
    public void aStageCostsTheGapSinceThePreviousStage() {
        LaunchTraceStages stages = new LaunchTraceStages();
        stages.record("start", 0);
        stages.record("extract", 500);
        stages.record("load", 800);

        long[] deltas = stages.deltasMs();
        assertEquals(0, deltas[0]);
        assertEquals(500, deltas[1]);
        assertEquals(300, deltas[2]);
    }

    @Test
    public void theSlowestStagesComeBackSlowestFirst() {
        LaunchTraceStages stages = new LaunchTraceStages();
        stages.record("start", 0);
        stages.record("quick", 10);
        stages.record("slow", 1000);
        stages.record("medium", 1300);

        List<LaunchTraceStages.Stage> slowest = stages.slowest(2);
        assertEquals(2, slowest.size());
        assertEquals("slow", slowest.get(0).name);
        assertEquals(990, slowest.get(0).deltaMs);
        assertEquals("medium", slowest.get(1).name);
        assertEquals(300, slowest.get(1).deltaMs);
    }

    @Test
    public void theRecordIsBoundedAndKeepsTheNewestStages() {
        LaunchTraceStages stages = new LaunchTraceStages();
        for (int i = 0; i < LaunchTraceStages.MAX_STAGES + 20; i++) {
            stages.record("stage" + i, i);
        }
        assertEquals(LaunchTraceStages.MAX_STAGES, stages.size());
        // The oldest entries were dropped, so the first recorded name is the one just past the drop.
        assertEquals("stage20", stages.nameFor(0));
        assertEquals("stage" + (LaunchTraceStages.MAX_STAGES + 19), stages.nameFor(stages.size() - 1));
    }

    @Test
    public void aBlankNameIsIgnored() {
        LaunchTraceStages stages = new LaunchTraceStages();
        stages.record(null, 5);
        stages.record("   ", 6);
        stages.record("real", 7);
        assertEquals(1, stages.size());
        assertEquals("real", stages.nameFor(0));
    }

    @Test
    public void aBackwardsClockNeverProducesANegativeDelta() {
        LaunchTraceStages stages = new LaunchTraceStages();
        stages.record("a", 100);
        stages.record("b", 50);
        long[] deltas = stages.deltasMs();
        assertEquals(100, deltas[0]);
        assertEquals(0, deltas[1]);
    }

    @Test
    public void anEmptyRecordDescribesItselfWithoutThrowing() {
        LaunchTraceStages stages = new LaunchTraceStages();
        assertTrue(stages.slowest(3).isEmpty());
        assertEquals("(no launch stages recorded)", stages.describeSlowest(3));
    }

    @Test
    public void describeNamesTheSlowestStagesOnTheirOwnLines() {
        LaunchTraceStages stages = new LaunchTraceStages();
        stages.record("start", 0);
        stages.record("extract", 4200);
        String text = stages.describeSlowest(3);
        assertTrue(text.contains("extract"));
        assertTrue(text.contains("4200"));
    }
}

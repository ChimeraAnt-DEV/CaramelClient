package org.chimeramc.client.core.startup;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The startup phases, pinned without a device.
 *
 * <p>Every step must be in exactly one phase: a step in neither list would silently never run, and a
 * step in both would run twice. The critical path must stay small, because everything on it delays
 * the first frame.
 */
public class StartupPhasesTest {

    @Test
    public void everyStepIsInExactlyOnePhase() {
        assertTrue("a step in neither list never runs; one in both runs twice",
                StartupPhases.everyStepIsClassified());
    }

    @Test
    public void theCriticalPathIsSmallAndTheRestIsDeferred() {
        int critical = StartupPhases.criticalPath().size();
        int deferred = StartupPhases.deferred().size();
        assertTrue("the critical path must stay small: everything on it delays the first frame",
                critical <= 4);
        assertTrue("most startup work is deferrable", deferred > critical);
        assertTrue("the two phases must cover every step",
                critical + deferred == StartupPhases.Step.values().length);
    }

    @Test
    public void theHeavyNativeAndDiskWorkIsDeferred() {
        // These are the steps that made the start slow: native geometry publishing and disk passes.
        assertFalse("native cosmetics publishing must not block the first frame",
                StartupPhases.isCritical(StartupPhases.Step.NATIVE_COSMETICS));
        assertFalse("the optifine reconcile disk pass must not block the first frame",
                StartupPhases.isCritical(StartupPhases.Step.OPTIFINE_RECONCILE));
        assertFalse("the storage migration disk pass must not block the first frame",
                StartupPhases.isCritical(StartupPhases.Step.STORAGE_MIGRATION));
        assertFalse("hook probes are read after the first frame",
                StartupPhases.isCritical(StartupPhases.Step.HOOK_AVAILABILITY));
    }

    @Test
    public void theLoggerSettingsAndCrashHandlerAreCritical() {
        assertTrue(StartupPhases.isCritical(StartupPhases.Step.APP_LOG));
        assertTrue(StartupPhases.isCritical(StartupPhases.Step.SETTINGS));
        assertTrue(StartupPhases.isCritical(StartupPhases.Step.CRASH_REPORTER));
        assertFalse(StartupPhases.isCritical(null));
    }
}

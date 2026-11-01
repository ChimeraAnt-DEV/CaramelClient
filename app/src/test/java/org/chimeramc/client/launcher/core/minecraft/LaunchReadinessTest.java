package org.chimeramc.client.launcher.core.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The named failure-state precedence (U14). Pure: no device, no Context.
 */
public class LaunchReadinessTest {

    private static LaunchReadiness.Facts allGood() {
        return new LaunchReadiness.Facts()
                .versionSelected(true)
                .gameFilesPresent(true)
                .abiIncompatible(false)
                .extractionStale(false)
                .signatureRulesCoverBuild(true)
                .minecraftInstalled(true)
                .signInRequired(false)
                .signedIn(true);
    }

    @Test
    public void everythingPresentIsReady() {
        assertEquals(LaunchReadiness.State.READY, LaunchReadiness.evaluate(allGood()));
    }

    @Test
    public void noVersionWinsOverEverythingElse() {
        LaunchReadiness.Facts facts = allGood()
                .versionSelected(false)
                .gameFilesPresent(false)
                .abiIncompatible(true)
                .minecraftInstalled(false);
        assertEquals(LaunchReadiness.State.NO_VERSION_SELECTED, LaunchReadiness.evaluate(facts));
    }

    @Test
    public void missingGameFilesBeatsAnAbiProblem() {
        // There is nothing to load, so the ABI question is moot; reporting it would be a red herring.
        LaunchReadiness.Facts facts = allGood().gameFilesPresent(false).abiIncompatible(true);
        assertEquals(LaunchReadiness.State.GAME_FILES_MISSING, LaunchReadiness.evaluate(facts));
    }

    @Test
    public void abiIncompatibleBeatsAStaleCache() {
        // Re-extracting cannot make a 32-bit library load in a 64-bit process.
        LaunchReadiness.Facts facts = allGood().abiIncompatible(true).extractionStale(true);
        assertEquals(LaunchReadiness.State.ABI_INCOMPATIBLE, LaunchReadiness.evaluate(facts));
    }

    @Test
    public void theRemainingStatesAreReachedInOrder() {
        assertEquals(LaunchReadiness.State.STALE_NATIVE_LIBS,
                LaunchReadiness.evaluate(allGood().extractionStale(true)));
        assertEquals(LaunchReadiness.State.SIGNATURE_RULES_MISSING,
                LaunchReadiness.evaluate(allGood().signatureRulesCoverBuild(false)));
        assertEquals(LaunchReadiness.State.GAME_NOT_INSTALLED,
                LaunchReadiness.evaluate(allGood().minecraftInstalled(false)));
        assertEquals(LaunchReadiness.State.SIGN_IN_REQUIRED,
                LaunchReadiness.evaluate(allGood().signInRequired(true).signedIn(false)));
    }

    @Test
    public void aBuildNewerThanTheRulesIsAWarningNotAGap() {
        // The two uncovered cases read differently: a newer game is "wait for an update", an
        // uncovered build is "this exact build has no rule". Both still launch.
        LaunchReadiness.Facts newer = allGood()
                .signatureRulesCoverBuild(false)
                .signatureRulesNewerBuild(true);
        assertEquals(LaunchReadiness.State.GAME_NEWER_THAN_RULES, LaunchReadiness.evaluate(newer));
        assertFalse(LaunchReadiness.blocksLaunch(LaunchReadiness.State.GAME_NEWER_THAN_RULES));

        // The newer flag is only consulted when coverage is actually missing.
        LaunchReadiness.Facts covered = allGood().signatureRulesNewerBuild(true);
        assertEquals(LaunchReadiness.State.READY, LaunchReadiness.evaluate(covered));
    }

    @Test
    public void signInIsNotRequiredWhenTheFeatureIsOff() {
        assertEquals(LaunchReadiness.State.READY,
                LaunchReadiness.evaluate(allGood().signInRequired(false).signedIn(false)));
    }

    @Test
    public void onlyGenuineBlockersBlock() {
        assertFalse(LaunchReadiness.blocksLaunch(LaunchReadiness.State.READY));
        // Both of these are repairable or degrade only optional hooks, so the launch proceeds.
        assertFalse(LaunchReadiness.blocksLaunch(LaunchReadiness.State.STALE_NATIVE_LIBS));
        assertFalse(LaunchReadiness.blocksLaunch(LaunchReadiness.State.SIGNATURE_RULES_MISSING));
        assertFalse(LaunchReadiness.blocksLaunch(LaunchReadiness.State.GAME_NEWER_THAN_RULES));

        assertTrue(LaunchReadiness.blocksLaunch(LaunchReadiness.State.NO_VERSION_SELECTED));
        assertTrue(LaunchReadiness.blocksLaunch(LaunchReadiness.State.PLAY_STORE_PACKAGE));
        assertTrue(LaunchReadiness.blocksLaunch(LaunchReadiness.State.GAME_FILES_MISSING));
        assertTrue(LaunchReadiness.blocksLaunch(LaunchReadiness.State.ABI_INCOMPATIBLE));
        assertTrue(LaunchReadiness.blocksLaunch(LaunchReadiness.State.GAME_NOT_INSTALLED));
        assertTrue(LaunchReadiness.blocksLaunch(LaunchReadiness.State.SIGN_IN_REQUIRED));
    }

    @Test
    public void everyStateHasAKey() {
        for (LaunchReadiness.State state : LaunchReadiness.State.values()) {
            String key = LaunchReadiness.key(state);
            assertTrue(key, key.length() > 0);
            assertEquals(key, key.toLowerCase(java.util.Locale.ROOT));
        }
        assertEquals("unknown", LaunchReadiness.key(null));
    }

    @Test
    public void nullFactsReadAsNoVersion() {
        assertEquals(LaunchReadiness.State.NO_VERSION_SELECTED, LaunchReadiness.evaluate(null));
    }
}

package org.chimeramc.client.ui.compose.launch;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * The hero's two presentation rules, pinned without a device.
 *
 * <p>Both were previously buried in the activity: the version line was hidden when it repeated the
 * instance name, and the ABI badge was chosen from the version's abi list. A regression in either is
 * invisible to a build machine, so they are asserted here.
 */
public class LaunchHeroStateTest {

    @Test
    public void theVersionLineIsHiddenWhenItRepeatsTheName() {
        assertEquals("", LaunchHeroState.versionOrEmpty("26.51", "26.51"));
        assertEquals("26.51", LaunchHeroState.versionOrEmpty("My world", "26.51"));
        assertEquals("", LaunchHeroState.versionOrEmpty("26.51", ""));
        assertEquals("", LaunchHeroState.versionOrEmpty("26.51", null));
    }

    @Test
    public void theAbiBadgeFollowsTheAbiList() {
        assertEquals(LaunchHeroState.AbiKind.BIT_32, LaunchHeroState.abiKind("armeabi-v7a"));
        assertEquals(LaunchHeroState.AbiKind.BIT_32, LaunchHeroState.abiKind("x86"));
        assertEquals(LaunchHeroState.AbiKind.BIT_64, LaunchHeroState.abiKind("arm64-v8a"));
        assertEquals(LaunchHeroState.AbiKind.BIT_64, LaunchHeroState.abiKind("x86_64"));
        assertEquals(LaunchHeroState.AbiKind.NONE, LaunchHeroState.abiKind(""));
        assertEquals(LaunchHeroState.AbiKind.NONE, LaunchHeroState.abiKind(null));
    }

    @Test
    public void theEmptyStateHasNoVersionOrAbi() {
        LaunchHeroState s = LaunchHeroState.empty("Last played", "Tap to play", "No version", "desc");
        assertEquals("", s.version);
        assertEquals(LaunchHeroState.AbiKind.NONE, s.abi);
        assertEquals(0, s.instancesCount);
        assertEquals(0, s.modsCount);
        assertEquals("No version", s.name);
    }
}

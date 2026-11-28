package org.chimeramc.client.core.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The Bedrock version the launcher's own chain reports.
 *
 * <p>{@code BedrockAuthManager} hands this string to the PlayFab session start, and it used to be
 * the literal {@code "1.21.110"} for every build, so a 26.60 launch described a client the game is
 * not. The preparer now sets the instance's own code; these pin the default and the normalisation.
 */
public class MsftAuthManagerGameVersionTest {

    @Test
    public void theDefaultNamesARecentReleaseAndNotTheOldLiteral() {
        assertEquals("1.26.66", MsftAuthManager.DEFAULT_GAME_VERSION);
        assertEquals("1.26.66",
                MsftAuthManager.normalizeGameVersion(MsftAuthManager.DEFAULT_GAME_VERSION));
    }

    @Test
    public void aShortLabelGainsTheOnePrefix() {
        assertEquals("1.26.60", MsftAuthManager.normalizeGameVersion("26.60"));
        assertEquals("1.26.70.26", MsftAuthManager.normalizeGameVersion("26.70.26"));
        assertEquals("1.26.60.4", MsftAuthManager.normalizeGameVersion(" 1.26.60.4 "));
    }

    @Test
    public void aChannelSuffixIsDropped() {
        assertEquals("1.26.60.4", MsftAuthManager.normalizeGameVersion("1.26.60.4-beta"));
        assertEquals("1.26.60.30", MsftAuthManager.normalizeGameVersion("1.26.60.30_RC3"));
        assertEquals("1.26.70.26", MsftAuthManager.normalizeGameVersion("26.70.26-beta"));
        assertEquals("1.26.66", MsftAuthManager.normalizeGameVersion("1.26.66."));
    }

    @Test
    public void aPlaceholderIsNotAVersion() {
        assertEquals("", MsftAuthManager.normalizeGameVersion(null));
        assertEquals("", MsftAuthManager.normalizeGameVersion(""));
        assertEquals("", MsftAuthManager.normalizeGameVersion("   "));
        assertEquals("", MsftAuthManager.normalizeGameVersion("latest"));
        assertEquals("", MsftAuthManager.normalizeGameVersion("26"));
        assertEquals("", MsftAuthManager.normalizeGameVersion("-beta"));
    }

    @Test
    public void settingTheActiveVersionIgnoresJunkAndKeepsTheLastGoodOne() {
        String original = MsftAuthManager.activeGameVersion();
        try {
            MsftAuthManager.setActiveGameVersion("26.66");
            assertEquals("1.26.66", MsftAuthManager.activeGameVersion());
            MsftAuthManager.setActiveGameVersion("latest");
            assertEquals("1.26.66", MsftAuthManager.activeGameVersion());
            MsftAuthManager.setActiveGameVersion("1.26.70.26");
            assertEquals("1.26.70.26", MsftAuthManager.activeGameVersion());
        } finally {
            MsftAuthManager.setActiveGameVersion(original);
        }
        assertTrue(MsftAuthManager.activeGameVersion().startsWith("1."));
    }
}

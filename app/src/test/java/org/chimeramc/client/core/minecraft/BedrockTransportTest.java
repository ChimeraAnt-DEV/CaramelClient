package org.chimeramc.client.core.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The transport boundary. Every launcher feature that has to behave differently on 26.60+ reads
 * {@link BedrockTransport#forVersion}, so a wrong answer here moves users onto the wrong transport
 * silently: a 26.52 instance would try NetherNet (and never join), and a 26.60 instance would keep
 * pinging RakNet (and read "offline" for a server that is up).
 */
public class BedrockTransportTest {

    @Test
    public void buildsBeforeTheReleaseStayOnRakNet() {
        assertEquals(BedrockTransport.Transport.RAKNET, BedrockTransport.forVersion("1.26.52"));
        assertEquals(BedrockTransport.Transport.RAKNET, BedrockTransport.forVersion("1.26.59.3"));
        assertEquals(BedrockTransport.Transport.RAKNET, BedrockTransport.forVersion("1.26.59.99-beta"));
        assertEquals(BedrockTransport.Transport.RAKNET, BedrockTransport.forVersion("1.21.132.1"));
        assertEquals(BedrockTransport.Transport.RAKNET, BedrockTransport.forVersion("26.59.10"));
    }

    @Test
    public void theReleaseAndLaterAreNetherNet() {
        assertEquals(BedrockTransport.Transport.NETHERNET, BedrockTransport.forVersion("1.26.60"));
        assertEquals(BedrockTransport.Transport.NETHERNET, BedrockTransport.forVersion("1.26.60.22"));
        assertEquals(BedrockTransport.Transport.NETHERNET, BedrockTransport.forVersion("1.26.60.28"));
        assertEquals(BedrockTransport.Transport.NETHERNET, BedrockTransport.forVersion("1.26.61"));
        assertEquals(BedrockTransport.Transport.NETHERNET, BedrockTransport.forVersion("1.27.0.4"));
    }

    @Test
    public void theReleasesAfterTheBoundaryStayOnNetherNet() {
        // mcpedl.org on 2026-11-19: 26.63, 26.66 (latest release) and 26.70.26 (latest beta). A
        // newer build must not fall back to RakNet, or every server reads "offline" on it.
        for (String version : new String[]{
                "1.26.63", "1.26.63.1", "1.26.66", "1.26.70.20", "1.26.70.26", "26.66"}) {
            assertEquals(version, BedrockTransport.Transport.NETHERNET,
                    BedrockTransport.forVersion(version));
        }
    }

    @Test
    public void theShortSpellingReadsAsTheSameBuild() {
        assertEquals(BedrockTransport.Transport.NETHERNET, BedrockTransport.forVersion("26.60"));
        assertEquals(BedrockTransport.Transport.NETHERNET, BedrockTransport.forVersion("26.60.28"));
        assertEquals(BedrockTransport.Transport.RAKNET, BedrockTransport.forVersion("26.59.20"));
        assertEquals("1.26.60.28", BedrockTransport.canonicalVersion("26.60.28"));
        assertEquals("1.26.60.28", BedrockTransport.canonicalVersion("1.26.60.28"));
    }

    @Test
    public void channelSuffixesDoNotChangeTheAnswer() {
        assertEquals(BedrockTransport.Transport.NETHERNET, BedrockTransport.forVersion("1.26.60.20-beta"));
        assertEquals(BedrockTransport.Transport.NETHERNET, BedrockTransport.forVersion("1.26.60.28_RC3"));
        assertEquals(BedrockTransport.Transport.RAKNET, BedrockTransport.forVersion("1.26.59.5-beta"));
        assertTrue(BedrockTransport.isBeta("1.26.60.20-beta"));
        assertFalse(BedrockTransport.isBeta("1.26.60.20"));
    }

    @Test
    public void unreadableCodesFailToTheKnownGoodTransport() {
        assertEquals(BedrockTransport.Transport.RAKNET, BedrockTransport.forVersion(null));
        assertEquals(BedrockTransport.Transport.RAKNET, BedrockTransport.forVersion(""));
        assertEquals(BedrockTransport.Transport.RAKNET, BedrockTransport.forVersion("   "));
        assertEquals(BedrockTransport.Transport.RAKNET, BedrockTransport.forVersion("latest"));
        assertEquals(BedrockTransport.Transport.RAKNET, BedrockTransport.forVersion("26"));
        assertEquals(BedrockTransport.Transport.RAKNET, BedrockTransport.forVersion("v"));
        assertEquals("", BedrockTransport.canonicalVersion("not a version"));
    }
}

package org.chimeramc.client.core.replay;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * A replay is routed by its container magic, not its file name.
 *
 * <p>The two replay formats share names once a BerryAPI capture is exported as a launcher file (or
 * the reverse), and routing by extension sent each to the other engine's reader, which reported
 * "bad replay" as if the file were corrupt. The magic is the format's own declaration, so it decides.
 */
public class ReplayContainerKindTest {

    private static byte[] berryHead() {
        // "BRPL" then a version byte, as BerryAPI writes it.
        return new byte[] {'B', 'R', 'P', 'L', 1, 0, 0, 0};
    }

    private static byte[] glowHead() {
        return "{\"format\":\"glowreplay\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Test
    public void theBerryMagicIsRecognised() {
        assertEquals(ReplayContainerKind.Kind.BERRY, ReplayContainerKind.of(berryHead()));
        assertEquals(ReplayContainerKind.Kind.BERRY,
                ReplayContainerKind.of("anything.glowreplay", berryHead()));
        assertEquals("BRPL", ReplayContainerKind.magicText());
    }

    @Test
    public void aJsonDocumentIsTheLauncherContainer() {
        assertEquals(ReplayContainerKind.Kind.GLOWREPLAY, ReplayContainerKind.of(glowHead()));
        // Leading whitespace is still JSON.
        byte[] padded = "  \n {\"format\":\"glowreplay\"}"
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(ReplayContainerKind.Kind.GLOWREPLAY, ReplayContainerKind.of(padded));
    }

    @Test
    public void theMagicWinsOverTheName() {
        // A BerryAPI file named like a launcher one is still BerryAPI: the bytes decide.
        assertEquals(ReplayContainerKind.Kind.BERRY,
                ReplayContainerKind.of("clip.glowreplay", berryHead()));
        // And a launcher document named like a BerryAPI one is still the launcher's.
        assertEquals(ReplayContainerKind.Kind.GLOWREPLAY,
                ReplayContainerKind.of("clip.berryreplay", glowHead()));
    }

    @Test
    public void anUnreadableHeadFallsBackToTheName() {
        // No bytes at all: the name is the only signal left.
        assertEquals(ReplayContainerKind.Kind.BERRY,
                ReplayContainerKind.of("clip.breplay", null));
        assertEquals(ReplayContainerKind.Kind.GLOWREPLAY,
                ReplayContainerKind.of("clip.glowreplay", null));
        assertEquals(ReplayContainerKind.Kind.UNKNOWN, ReplayContainerKind.of("notes.txt", null));
    }

    @Test
    public void anUnknownOrShortBufferIsUnknown() {
        assertEquals(ReplayContainerKind.Kind.UNKNOWN, ReplayContainerKind.of((byte[]) null));
        assertEquals(ReplayContainerKind.Kind.UNKNOWN, ReplayContainerKind.of(new byte[0]));
        assertEquals(ReplayContainerKind.Kind.UNKNOWN, ReplayContainerKind.of(new byte[] {'B', 'R'}));
        assertEquals(ReplayContainerKind.Kind.UNKNOWN,
                ReplayContainerKind.of(new byte[] {(byte) 0x89, 'P', 'N', 'G'}));
    }
}

package org.chimeramc.client.core.replay;

import java.nio.charset.StandardCharsets;

/**
 * Which replay engine owns a file, decided by its container magic rather than its name.
 *
 * <p>There are two replay formats in the app. The launcher's own is {@code .glowreplay}, a JSON
 * document (see {@link ReplayDocument}); BerryAPI's is a binary container whose first four bytes are
 * {@code BRPL}. The panel used to route by file extension, which is wrong the moment the two share a
 * name: a BerryAPI capture exported to {@code .glowreplay} was handed to the launcher's JSON reader,
 * and a launcher document renamed to {@code .breplay} was handed to BerryAPI. Both fail as "bad
 * replay", which looks like a corrupt file rather than a routing bug.
 *
 * <p>The magic is the honest test: it is the format's own declaration, not a hint from the name. The
 * check is pure and reads only the first few bytes, so it is unit-tested without a device and costs
 * nothing on a large file.
 */
public final class ReplayContainerKind {

    /** BerryAPI's replay magic, {@code BRPL} (Berry ReP Lay), from its {@code replay_format.h}. */
    public static final byte[] BERRY_MAGIC = {'B', 'R', 'P', 'L'};

    /** Which engine can play a file. */
    public enum Kind {
        /** The launcher's own JSON {@code .glowreplay} container. */
        GLOWREPLAY,
        /** BerryAPI's binary replay container ({@code BRPL}). */
        BERRY,
        /** Neither magic matched: not a replay this build can read. */
        UNKNOWN,
    }

    private ReplayContainerKind() {
    }

    /**
     * The kind of a replay from its leading bytes.
     *
     * <p>A BerryAPI container is recognised by its four-byte magic; anything else that looks like a
     * JSON object is the launcher's own; everything else is unknown. A null or too-short buffer is
     * unknown rather than guessed.
     */
    public static Kind of(byte[] head) {
        if (head == null) return Kind.UNKNOWN;
        if (startsWith(head, BERRY_MAGIC)) return Kind.BERRY;
        if (looksLikeJson(head)) return Kind.GLOWREPLAY;
        return Kind.UNKNOWN;
    }

    /**
     * The kind of a replay from its file name and leading bytes.
     *
     * <p>The magic wins when it is present. Only when the bytes are inconclusive does the name
     * decide, so a BerryAPI file with a launcher name (or the reverse) is still routed correctly,
     * and a file whose header could not be read at least lands somewhere sensible.
     */
    public static Kind of(String fileName, byte[] head) {
        Kind byMagic = of(head);
        if (byMagic != Kind.UNKNOWN) return byMagic;
        return byName(fileName);
    }

    /** The kind a file name alone implies, for a caller with no bytes to read. */
    public static Kind byName(String fileName) {
        if (fileName == null) return Kind.UNKNOWN;
        String name = fileName.toLowerCase(java.util.Locale.ROOT);
        if (name.endsWith(".berryreplay") || name.endsWith(".breplay")) return Kind.BERRY;
        if (name.endsWith(ReplayDocument.EXTENSION)) return Kind.GLOWREPLAY;
        return Kind.UNKNOWN;
    }

    private static boolean startsWith(byte[] head, byte[] magic) {
        if (head.length < magic.length) return false;
        for (int i = 0; i < magic.length; i++) {
            if (head[i] != magic[i]) return false;
        }
        return true;
    }

    /** True when the head looks like a JSON object: optional whitespace then '{'. */
    private static boolean looksLikeJson(byte[] head) {
        int i = 0;
        while (i < head.length && (head[i] == ' ' || head[i] == '\t'
                || head[i] == '\n' || head[i] == '\r')) {
            i++;
        }
        return i < head.length && head[i] == '{';
    }

    /** The magic as text, for a status line. */
    public static String magicText() {
        return new String(BERRY_MAGIC, StandardCharsets.US_ASCII);
    }
}

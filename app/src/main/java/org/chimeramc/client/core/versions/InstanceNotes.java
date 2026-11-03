package org.chimeramc.client.core.versions;

/**
 * The rule for a per-instance note.
 *
 * <p>A note is free-form user text that is persisted into the instance's {@code profile.json}, so it
 * is normalised in one place: trimmed, stripped of control characters (a pasted newline or NUL in a
 * JSON string is a way to break the file), and capped at {@link #MAX_LENGTH} so a paste cannot grow
 * the metadata without bound. An empty result is stored as {@code null} rather than {@code ""}, so
 * "no note" has one representation.
 *
 * <p>Pure and Android-free, so the rules are unit-tested without a device.
 */
public final class InstanceNotes {

    /** The longest note kept. Generous for a note, small enough that the metadata stays tiny. */
    public static final int MAX_LENGTH = 500;

    private InstanceNotes() {
    }

    /**
     * Normalises a note for storage.
     *
     * @return the cleaned text, or {@code null} when there is nothing left to store.
     */
    public static String normalize(String raw) {
        if (raw == null) return null;
        StringBuilder sb = new StringBuilder(Math.min(raw.length(), MAX_LENGTH));
        for (int i = 0; i < raw.length() && sb.length() < MAX_LENGTH; i++) {
            char c = raw.charAt(i);
            // Keep ordinary whitespace as a single space; drop the rest of the control range.
            if (c == '\n' || c == '\r' || c == '\t') {
                sb.append(' ');
            } else if (c < 0x20 || c == 0x7F) {
                // control character: dropped
            } else {
                sb.append(c);
            }
        }
        String trimmed = sb.toString().trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Whether the note has anything to show.
     *
     * <p>Used to hide the card's note line rather than render an empty row.
     */
    public static boolean isPresent(String note) {
        return normalize(note) != null;
    }
}

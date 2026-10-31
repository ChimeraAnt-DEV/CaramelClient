package org.chimeramc.client.core.privacy;

/**
 * The client's privacy modes as one rule (item 272).
 *
 * <p>A mode exists to remove something from view, and it is easy to get half-right: a guest mode
 * that hides the Mods tab but leaves the mod-hub row on the launch screen. So the decisions live
 * here, in one Android-free class, and every surface asks it rather than deciding for itself.
 *
 * <p>Guest mode is for a shared or borrowed device: mods, servers and voice chat are hidden, so a
 * guest cannot see or change what the owner has set up. It never hides the game itself -- a
 * launcher that refused to launch would be a broken launcher, not a private one. Everything here
 * is a presentation decision on top of features that keep working.
 */
public final class PrivacyMode {

    /** What a caller is about to show, so the answer is about that thing. */
    public enum Surface {
        /** The Mods destination and anything that opens it. */
        MODS,
        /** The server list and join-by-address. */
        SERVERS,
        /** Proximity voice chat and the relay. */
        VOICE
    }

    private final boolean guestMode;

    public PrivacyMode(boolean guestMode) {
        this.guestMode = guestMode;
    }

    /** Guest mode off: every surface is shown. */
    public static PrivacyMode open() {
        return new PrivacyMode(false);
    }

    public boolean isGuestMode() {
        return guestMode;
    }

    /**
     * Whether {@code surface} is hidden.
     *
     * <p>A single question for every surface, so a new caller cannot decide for itself: every
     * surface guest mode owns is hidden by the one flag.
     */
    public boolean hides(Surface surface) {
        if (surface == null) return false;
        switch (surface) {
            case MODS:
            case SERVERS:
            case VOICE:
                return guestMode;
            default:
                return false;
        }
    }

    /** True when the Mods destination should not be reachable. */
    public boolean hidesMods() {
        return hides(Surface.MODS);
    }

    /** True when the server list should not be shown. */
    public boolean hidesServers() {
        return hides(Surface.SERVERS);
    }

    /** True when voice chat should not be offered. */
    public boolean hidesVoice() {
        return hides(Surface.VOICE);
    }

    /** Whether guest mode is on, so a screen can show a single "guest mode" banner. */
    public boolean isAnyActive() {
        return guestMode;
    }
}

package org.chimeramc.client.core.privacy;

/**
 * The client's two privacy modes as one rule (items 272, 277).
 *
 * <p>Both modes exist to remove something from view, and both are easy to get half-right: a guest
 * mode that hides the Mods tab but leaves the mod-hub row on the launch screen, or a streaming-safe
 * mode that masks coordinates in the debug overlay but not in the log a streamer pastes into chat.
 * So the decisions live here, in one Android-free class, and every surface asks it rather than
 * deciding for itself.
 *
 * <ul>
 *   <li><b>Guest mode</b> is for a shared or borrowed device: mods, servers and voice chat are
 *       hidden, so a guest cannot see or change what the owner has set up.
 *   <li><b>Streaming-safe mode</b> is for recording or streaming: account names, coordinates and
 *       server addresses are masked in overlays and in any text that leaves the app.
 * </ul>
 *
 * <p>The two are independent, and neither ever hides the game itself: a launcher that refused to
 * launch would be a broken launcher, not a private one. Everything here is a presentation decision
 * on top of features that keep working.
 */
public final class PrivacyMode {

    /** What a caller is about to show or export, so the answer is about that thing. */
    public enum Surface {
        /** The Mods destination and anything that opens it. */
        MODS,
        /** The server list and join-by-address. */
        SERVERS,
        /** Proximity voice chat and the relay. */
        VOICE,
        /** The player's world position in an overlay or the debug screen. */
        COORDINATES,
        /** A gamertag or account name. */
        IDENTITY,
        /** A server host and port. */
        SERVER_ADDRESS
    }

    private final boolean guestMode;
    private final boolean streamingSafeMode;

    public PrivacyMode(boolean guestMode, boolean streamingSafeMode) {
        this.guestMode = guestMode;
        this.streamingSafeMode = streamingSafeMode;
    }

    /** Neither mode: every surface is shown. */
    public static PrivacyMode open() {
        return new PrivacyMode(false, false);
    }

    public boolean isGuestMode() {
        return guestMode;
    }

    public boolean isStreamingSafeMode() {
        return streamingSafeMode;
    }

    /**
     * Whether {@code surface} is hidden or masked.
     *
     * <p>A single question for every surface, so a new caller cannot pick the wrong mode: guest mode
     * owns the three feature surfaces, streaming-safe mode owns the three pieces of identifying
     * data, and a surface belongs to exactly one of them.
     */
    public boolean hides(Surface surface) {
        if (surface == null) return false;
        switch (surface) {
            case MODS:
            case SERVERS:
            case VOICE:
                return guestMode;
            case COORDINATES:
            case IDENTITY:
            case SERVER_ADDRESS:
                return streamingSafeMode;
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

    /** True when a world position must be masked before it is drawn or logged. */
    public boolean hidesCoordinates() {
        return hides(Surface.COORDINATES);
    }

    /** True when a gamertag or account name must be masked. */
    public boolean hidesIdentity() {
        return hides(Surface.IDENTITY);
    }

    /** True when a server host or address must be masked. */
    public boolean hidesServerAddress() {
        return hides(Surface.SERVER_ADDRESS);
    }

    /** Whether any mode is on, so a screen can show a single "private mode" banner. */
    public boolean isAnyActive() {
        return guestMode || streamingSafeMode;
    }
}

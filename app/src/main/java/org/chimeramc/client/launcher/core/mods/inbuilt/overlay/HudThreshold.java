package org.chimeramc.client.core.mods.inbuilt.overlay;

/**
 * The colour-blind-safe rule for a HUD reading.
 *
 * <p>A HUD that says "good" or "bad" with colour alone is unreadable to a user with a colour
 * vision deficiency, and on an OLED phone in daylight the difference between a green and a red
 * number is often invisible to everyone. So a level here is always carried by two things at once:
 * a colour from a palette that stays distinguishable under deuteranopia and protanopia (a
 * blue-to-orange ramp, not a green-to-red one) and a {@link #cue(Level)} glyph that says the same
 * thing without hue.
 *
 * <p>Pure, so the thresholds and the palette are unit-tested rather than tuned by eye on a device.
 */
public final class HudThreshold {

    /** How a reading compares with what the device can do. */
    public enum Level {
        /** At or above {@link #GOOD_FPS}: the game is running at its target. */
        GOOD,
        /** Between {@link #POOR_FPS} and {@link #GOOD_FPS}: playable but short of the target. */
        FAIR,
        /** Below {@link #POOR_FPS}: the frame rate is visibly rough. */
        POOR,
        /** No reading yet. Renders as the em dash, never as a level. */
        UNKNOWN
    }

    /** At or above this the readout is GOOD. 55 leaves room under a 60Hz panel's 60fps target. */
    public static final int GOOD_FPS = 55;

    /** Below this the readout is POOR. 30 is the point most players notice stutter. */
    public static final int POOR_FPS = 30;

    /**
     * The good colour: a cyan-blue. Chosen for the blue end of the blue/orange axis, which stays
     * distinct from the fair and poor colours under the common red-green deficiencies.
     */
    public static final int COLOUR_GOOD = 0xFF4FC3F7;

    /** The fair colour: a neutral amber, the middle of the axis. */
    public static final int COLOUR_FAIR = 0xFFFFB74D;

    /** The poor colour: a warm orange-red, the far end of the axis. */
    public static final int COLOUR_POOR = 0xFFE64A19;

    /** The unknown colour: the muted overlay text, so an absent reading is not a level. */
    public static final int COLOUR_UNKNOWN = 0xFFAAAAAA;

    private HudThreshold() {
    }

    /** The level for an FPS reading; a non-positive reading is {@link Level#UNKNOWN}. */
    public static Level levelForFps(int fps) {
        if (fps <= 0) return Level.UNKNOWN;
        if (fps >= GOOD_FPS) return Level.GOOD;
        if (fps >= POOR_FPS) return Level.FAIR;
        return Level.POOR;
    }

    /** The colour for a level, from the blue-to-orange ramp. */
    public static int colourFor(Level level) {
        if (level == null) return COLOUR_UNKNOWN;
        switch (level) {
            case GOOD: return COLOUR_GOOD;
            case FAIR: return COLOUR_FAIR;
            case POOR: return COLOUR_POOR;
            default: return COLOUR_UNKNOWN;
        }
    }

    /**
     * The non-colour cue for a level: a shape that reads the same in any palette.
     *
     * <p>An up triangle, a square and a down triangle are distinguishable by silhouette alone, so
     * the level survives a monochrome screen, a colour-blind user and a bright day. They are
     * deliberately geometric-shape code points rather than a check and a cross: those are in the
     * dingbat block, which this project bans as emoji presentation ({@code NoEmojiTest}).
     */
    public static String cue(Level level) {
        if (level == null) return "";
        switch (level) {
            case GOOD: return "\u25B2";
            case FAIR: return "\u25A0";
            case POOR: return "\u25BC";
            default: return "";
        }
    }

    /**
     * A full readout: the cue, the number and the unit, e.g. {@code "\u25B2 60 FPS"}.
     *
     * <p>Used by a surface that wants the level and the value in one string; a surface that draws
     * the cue in a separate colour can call {@link #cue(Level)} and {@link #colourFor(Level)}.
     */
    public static String readout(int fps) {
        if (fps <= 0) return ModStatsFormatter.NO_READING;
        Level level = levelForFps(fps);
        String cue = cue(level);
        return cue.isEmpty() ? fps + " FPS" : cue + " " + fps + " FPS";
    }
}

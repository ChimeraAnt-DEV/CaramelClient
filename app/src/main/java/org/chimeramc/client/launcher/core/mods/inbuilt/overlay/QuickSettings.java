package org.chimeramc.client.core.mods.inbuilt.overlay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The in-game quick settings model.
 *
 * <p>Two settings a player wants to change without leaving a world: the render resolution scale and
 * the frame-rate cap. Both are modelled here as a closed set of choices with the clamping rules, so
 * the panel, the options writer and the tests all read one definition rather than three.
 *
 * <p>Honesty is the reason each setting carries a {@link Kind}: the resolution scale is a game
 * option the running world has already read, so it applies on the next load, while the frame cap can
 * be applied to the window immediately. The panel says which is which instead of implying both are
 * live.
 */
public final class QuickSettings {

    /** How a setting takes effect, which the panel states rather than implying "immediately". */
    public enum Kind {
        /** Applies at the next world load: the running world has already read the option. */
        NEXT_LOAD,
        /** Applies to the window at once. */
        IMMEDIATE
    }

    /** The Bedrock option key for the render resolution scale, as a percentage. */
    public static final String OPTION_RESOLUTION_SCALE = "gfx_resolution_scaling";

    /** The resolution scale choices, as percentages. */
    public static final int[] RESOLUTION_SCALES = {50, 75, 100};

    /** The frame cap choices in Hz; {@link #CAP_UNLIMITED} means "leave the panel alone". */
    public static final int[] REFRESH_CAPS = {30, 60, 90, 120, 0};

    /** The sentinel cap meaning no cap. */
    public static final int CAP_UNLIMITED = 0;

    /** The default resolution scale, matching the game's own default. */
    public static final int DEFAULT_RESOLUTION_SCALE = 100;

    private QuickSettings() {
    }

    /** One choice row. */
    public static final class Choice {
        public final int value;
        public final int labelRes;

        Choice(int value, int labelRes) {
            this.value = value;
            this.labelRes = labelRes;
        }
    }

    /** The resolution-scale choices, with their labels. */
    public static List<Choice> resolutionChoices(int[] labels) {
        return build(RESOLUTION_SCALES, labels);
    }

    /** The frame-cap choices, with their labels. */
    public static List<Choice> refreshChoices(int[] labels) {
        return build(REFRESH_CAPS, labels);
    }

    private static List<Choice> build(int[] values, int[] labels) {
        List<Choice> out = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            int labelRes = labels != null && i < labels.length ? labels[i] : 0;
            out.add(new Choice(values[i], labelRes));
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * Clamps a resolution scale to the nearest offered choice.
     *
     * <p>A stored or restored value outside the set would render no chip selected, which reads as
     * "nothing applied"; snapping to the nearest choice keeps the panel's state legible.
     */
    public static int clampResolutionScale(int scale) {
        return nearest(RESOLUTION_SCALES, scale, DEFAULT_RESOLUTION_SCALE);
    }

    /** Clamps a frame cap to the nearest offered choice. */
    public static int clampRefreshCap(int capHz) {
        return nearest(REFRESH_CAPS, capHz, CAP_UNLIMITED);
    }

    /** Whether the resolution-scale set contains this value. */
    public static boolean isResolutionScale(int scale) {
        for (int value : RESOLUTION_SCALES) {
            if (value == scale) return true;
        }
        return false;
    }

    /** Whether the frame-cap set contains this value. */
    public static boolean isRefreshCap(int capHz) {
        for (int value : REFRESH_CAPS) {
            if (value == capHz) return true;
        }
        return false;
    }

    private static int nearest(int[] values, int target, int fallback) {
        if (values.length == 0) return fallback;
        int best = values[0];
        int bestDelta = Math.abs(values[0] - target);
        for (int value : values) {
            int delta = Math.abs(value - target);
            if (delta < bestDelta) {
                bestDelta = delta;
                best = value;
            }
        }
        return best;
    }

    /** The option value to write for a resolution scale, as the game stores it. */
    public static String resolutionOptionValue(int scale) {
        return String.valueOf(clampResolutionScale(scale));
    }
}

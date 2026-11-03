package org.chimeramc.client.core.mods.inbuilt;

import java.util.List;

/**
 * The control a schema node turns into, derived as pure rules.
 *
 * <p>{@link RuntimeConfigView} already builds a control per node, but the parameters that decide
 * <em>what</em> it builds were inline literals spread through the view: the 0..100 int default, the
 * 0..1 float default, the step of 1, the clamp, and the "four or fewer options means segmented"
 * choice rule. A rule inside a View cannot be unit-tested, and the failure it produces is quiet -
 * a slider whose bounds disagree with the schema writes a value the mod then clamps or ignores.
 *
 * <p>This holds those rules. The view still owns the widgets; it asks this what to build.
 */
public final class RuntimeConfigControl {

    /** An integer slider's usable bounds and current value. */
    public static final class IntRange {
        public final int min;
        public final int max;
        public final int step;
        public final int value;

        IntRange(int min, int max, int step, int value) {
            this.min = min;
            this.max = max;
            this.step = step;
            this.value = value;
        }

        /** How many steps the track has, at least 1 so a degenerate range still renders. */
        public int steps() {
            int span = max - min;
            if (span <= 0) return 1;
            return Math.max(1, (span + step - 1) / step);
        }

        /** The value for a track position, clamped to the range. */
        public int valueAt(int position) {
            int clamped = Math.max(0, Math.min(steps(), position));
            return Math.min(max, min + clamped * step);
        }

        /** The track position for a value, clamped to the range. */
        public int positionOf(int v) {
            int clamped = Math.max(min, Math.min(max, v));
            return Math.min(steps(), Math.round((clamped - min) / (float) step));
        }
    }

    /** A float slider's usable bounds and current value. */
    public static final class FloatRange {
        public final float min;
        public final float max;
        public final float value;

        FloatRange(float min, float max, float value) {
            this.min = min;
            this.max = max;
            this.value = value;
        }
    }

    private RuntimeConfigControl() {
    }

    /**
     * The int slider's bounds, step and value.
     *
     * <p>A malformed or absent bound falls back rather than throwing: a schema is external data, and
     * a slider that cannot be built is worse than one with a sane default. A step below 1 would make
     * {@link IntRange#steps()} divide by zero.
     */
    public static IntRange intRange(RuntimeConfigSchema.Node node, String currentValue) {
        int min = parseInt(node.minValue, 0);
        int max = Math.max(min, parseInt(node.maxValue, 100));
        int step = Math.max(1, parseInt(node.step, 1));
        String raw = currentValue != null && !currentValue.isEmpty()
                ? currentValue : node.defaultValue;
        int value = clamp(parseInt(raw, min), min, max);
        return new IntRange(min, max, step, value);
    }

    /** The float slider's bounds and value, with a malformed bound falling back to 0..1. */
    public static FloatRange floatRange(RuntimeConfigSchema.Node node, String currentValue) {
        float min = parseFloat(node.minValue, 0f);
        float max = parseFloat(node.maxValue, 1f);
        if (max < min) max = min;
        String raw = currentValue != null && !currentValue.isEmpty()
                ? currentValue : node.defaultValue;
        float value = clamp(parseFloat(raw, min), min, max);
        return new FloatRange(min, max, value);
    }

    /**
     * The choice control's style: an explicit style wins, otherwise four or fewer options render as
     * a segmented row and more as a dropdown.
     */
    public static String choiceStyle(RuntimeConfigSchema.Node node) {
        if (node.style != null && !node.style.isEmpty() && !"auto".equals(node.style)) {
            return node.style;
        }
        return node.options != null && node.options.size() <= 4 ? "segmented" : "dropdown";
    }

    /** The index of the option matching the current value, or 0 when nothing matches. */
    public static int choiceIndex(List<RuntimeConfigSchema.Option> options, String currentValue) {
        if (options == null) return 0;
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).value != null && options.get(i).value.equals(currentValue)) return i;
        }
        return 0;
    }

    /** The input length cap for a text node, or 0 when the schema sets none. */
    public static int maxLength(RuntimeConfigSchema.Node node) {
        return Math.max(0, node.maxLength);
    }

    /** Whether a text node should render as a multi-line field. */
    public static boolean multiline(RuntimeConfigSchema.Node node) {
        return "multiline_text".equals(node.type);
    }

    /** The value a toggle node represents, read from a stored string. */
    public static boolean toggleValue(String stored) {
        return "true".equalsIgnoreCase(stored) || "1".equals(stored)
                || "yes".equalsIgnoreCase(stored) || "on".equalsIgnoreCase(stored);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static float parseFloat(String value, float fallback) {
        try {
            return Float.parseFloat(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}

package org.chimeramc.client.ui.compose.theme;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Compose spacing, radius and size tokens, mirroring {@code res/values/dimens.xml}.
 *
 * <p>The Compose screens need the same numbers the View screens use, but a Compose layout cannot read
 * a {@code @dimen} as a {@code Dp} without a resource lookup. Rather than keep two hand-maintained
 * lists that drift, this class holds the values and {@link GlowDimensTest} asserts they equal the
 * real {@code dimens.xml} — so a change to one that is not made to the other fails the build.
 *
 * <p>Only the plain {@code Ndp} entries are mirrored. An alias such as {@code radius_card =
 * @dimen/radius_large} is resolved to its target by the test, so the class can carry the concrete
 * value a Compose layout needs.
 */
public final class GlowDimens {

    private GlowDimens() {
    }

    // --- Spacing (the U9 scale) --------------------------------------------------------------

    public static final int SPACE_1 = 4;
    public static final int SPACE_2 = 8;
    public static final int SPACE_3 = 12;
    public static final int SPACE_4 = 16;
    public static final int SPACE_6 = 24;
    public static final int SPACE_8 = 32;
    public static final int SPACE_HAIRLINE = 2;
    public static final int SPACE_DIVIDER = 1;
    public static final int SPACE_ALIGN_INSET = 52;
    public static final int SPACE_DOCK_CLEARANCE = 168;

    // --- Radius (the U10 scale) --------------------------------------------------------------

    public static final int RADIUS_SMALL = 8;
    public static final int RADIUS_MEDIUM = 12;
    public static final int RADIUS_LARGE = 16;
    public static final int RADIUS_XLARGE = 28;
    public static final int RADIUS_FIELD = 8;
    public static final int RADIUS_TOGGLE = 999;
    public static final int RADIUS_TAG = 4;
    public static final int RADIUS_BADGE = 6;
    public static final int RADIUS_CHIP_SMALL = 10;
    public static final int RADIUS_CIRCLE = 24;
    public static final int RADIUS_ACCENT_BAR = 2;
    public static final int RADIUS_SQUARE = 0;

    // --- Cards -------------------------------------------------------------------------------

    public static final int CARD_RADIUS = 16;
    public static final int CARD_RADIUS_SMALL = 12;
    public static final int CARD_BORDER_WIDTH = 1;
    public static final int CARD_ELEVATION = 2;
    public static final int CARD_ELEVATION_GLOW = 6;
    public static final int CARD_PADDING = 16;
    public static final int CARD_PADDING_COMPACT = 12;
    public static final int SCREEN_PADDING = 16;
    public static final int CARD_GUTTER = 8;

    // --- Navigation --------------------------------------------------------------------------

    public static final int NAV_TAB_PADDING = 14;
    public static final int NAV_RAIL_WIDTH = 72;
    public static final int NAV_RAIL_TAB_HEIGHT = 48;
    public static final int NAV_TAB_DOT_SIZE = 8;

    // --- Touch -------------------------------------------------------------------------------

    public static final int MIN_TOUCH_TARGET = 48;

    /**
     * The mirrored tokens as {@code dimen name -> dp value}, so the drift test can compare every one
     * against {@code dimens.xml} without reflecting over fields.
     */
    public static Map<String, Integer> asMap() {
        Map<String, Integer> out = new LinkedHashMap<>();
        out.put("space_1", SPACE_1);
        out.put("space_2", SPACE_2);
        out.put("space_3", SPACE_3);
        out.put("space_4", SPACE_4);
        out.put("space_6", SPACE_6);
        out.put("space_8", SPACE_8);
        out.put("space_hairline", SPACE_HAIRLINE);
        out.put("space_divider", SPACE_DIVIDER);
        out.put("space_align_inset", SPACE_ALIGN_INSET);
        out.put("space_dock_clearance", SPACE_DOCK_CLEARANCE);
        out.put("radius_small", RADIUS_SMALL);
        out.put("radius_medium", RADIUS_MEDIUM);
        out.put("radius_large", RADIUS_LARGE);
        out.put("radius_xlarge", RADIUS_XLARGE);
        out.put("radius_field", RADIUS_FIELD);
        out.put("radius_toggle", RADIUS_TOGGLE);
        out.put("radius_tag", RADIUS_TAG);
        out.put("radius_badge", RADIUS_BADGE);
        out.put("radius_chip_small", RADIUS_CHIP_SMALL);
        out.put("radius_circle", RADIUS_CIRCLE);
        out.put("radius_accent_bar", RADIUS_ACCENT_BAR);
        out.put("radius_square", RADIUS_SQUARE);
        out.put("chimera_card_radius", CARD_RADIUS);
        out.put("chimera_card_radius_small", CARD_RADIUS_SMALL);
        out.put("chimera_card_border_width", CARD_BORDER_WIDTH);
        out.put("chimera_card_elevation", CARD_ELEVATION);
        out.put("chimera_card_elevation_glow", CARD_ELEVATION_GLOW);
        out.put("chimera_card_padding", CARD_PADDING);
        out.put("chimera_card_padding_compact", CARD_PADDING_COMPACT);
        out.put("chimera_screen_padding", SCREEN_PADDING);
        out.put("chimera_card_gutter", CARD_GUTTER);
        out.put("nav_tab_padding", NAV_TAB_PADDING);
        out.put("nav_rail_width", NAV_RAIL_WIDTH);
        out.put("nav_rail_tab_height", NAV_RAIL_TAB_HEIGHT);
        out.put("nav_tab_dot_size", NAV_TAB_DOT_SIZE);
        out.put("min_touch_target", MIN_TOUCH_TARGET);
        return out;
    }

    /** The {@code dimens.xml} values as {@code name -> dp}, resolving {@code @dimen/...} aliases. */
    public static Map<String, Integer> readDimensXml() throws IOException {
        String xml = new String(Files.readAllBytes(resolve()), StandardCharsets.UTF_8);
        Map<String, String> raw = new LinkedHashMap<>();
        Matcher m = Pattern.compile("<dimen name=\"([^\"]+)\">([^<]+)</dimen>").matcher(xml);
        while (m.find()) raw.put(m.group(1), m.group(2).trim());

        Map<String, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : raw.entrySet()) {
            Integer dp = resolveDp(e.getValue(), raw);
            if (dp != null) out.put(e.getKey(), dp);
        }
        return out;
    }

    private static Integer resolveDp(String value, Map<String, String> raw) {
        String v = value;
        int guard = 0;
        while (v.startsWith("@dimen/") && guard++ < 8) {
            v = raw.getOrDefault(v.substring("@dimen/".length()), "");
            if (v.isEmpty()) return null;
        }
        if (!v.endsWith("dp")) return null;
        try {
            return Integer.valueOf(v.substring(0, v.length() - 2));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Path resolve() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            Path candidate = dir.resolve("app/src/main/res/values/dimens.xml");
            if (Files.exists(candidate)) return candidate;
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find app/src/main/res/values/dimens.xml");
    }
}

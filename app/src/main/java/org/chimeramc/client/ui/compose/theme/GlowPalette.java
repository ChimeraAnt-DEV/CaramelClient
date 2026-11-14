package org.chimeramc.client.ui.compose.theme;

/**
 * The Compose palette, derived purely from the existing {@link
 * org.chimeramc.client.ui.components.UiTokens} scale.
 *
 * <p>The theme is navy, and the toggles are snow blue. Both are expressed here as plain integer
 * maths so the values can be unit-tested without a device: the Compose theme object is a thin
 * wrapper that only turns these numbers into {@code Color}s.
 *
 * <p>Nothing here is a new source of truth. The window, surface, text and status colours are
 * {@code UiTokens}; the accent still resolves through {@code UiTokens.resolveAccent} so a wallpaper
 * accent keeps working. What this class adds is the two things a navy theme needs and {@code
 * UiTokens} never had: a deeper window and surface ramp, and the snow-blue toggle colours.
 */
public final class GlowPalette {

    private GlowPalette() {
    }

    // --- Navy ramp ---------------------------------------------------------------------------

    /**
     * The window: the pack's {@code Abyss}. The deepest step, a near-black navy — never pure black,
     * because on an LCD "OLED harsh" is a defect rather than a feature.
     */
    public static final int NAVY_WINDOW = 0xFF060B1A;
    /** A panel or card surface, one step above the window: the pack's {@code Navy}. */
    public static final int NAVY_SURFACE = 0xFF0A1330;
    /** A raised surface (a row inside a card, a hovered panel): the pack's {@code DeepBlue}. */
    public static final int NAVY_SURFACE_RAISED = 0xFF0F1B44;
    /** The hairline that separates two navy surfaces. */
    public static final int NAVY_STROKE = 0xFF22305A;

    /** The AMOLED window: true black, for the burn-in-conscious theme. */
    public static final int AMOLED_WINDOW = 0xFF000000;
    public static final int AMOLED_SURFACE = 0xFF070A11;
    public static final int AMOLED_SURFACE_RAISED = 0xFF101724;

    // --- Blue (the accent family) ------------------------------------------------------------

    /**
     * The pack's {@code Blue}: the toggle track when on, the primary action, the selected state.
     *
     * <p>The pack's toggle-on is its green, not its blue, but the launcher's toggle is the accent —
     * one accent per screen is the rule the mod menu's palette states, and the launch toggles are
     * the accent surface. The pack's blue is still the accent the rest of the UI uses.
     */
    public static final int SNOW_BLUE = 0xFF4A78FF;
    /** A step lighter, for hover and the toggle thumb: the pack's {@code BlueLight}. */
    public static final int SNOW_BLUE_LIGHT = 0xFF628CFF;
    /** A step deeper, for pressed and for text on a blue fill. */
    public static final int SNOW_BLUE_DEEP = 0xFF3458E6;
    /** The washed fill behind a selected row. */
    public static final int SNOW_BLUE_WASH = 0xFF152252;
    /** The toggle track when off: a muted navy, never grey. */
    public static final int TOGGLE_TRACK_OFF = 0xFF232C40;

    // --- Text --------------------------------------------------------------------------------

    /** The pack's {@code Text}. */
    public static final int TEXT_PRIMARY = 0xFFEAF0FF;
    /** The pack's {@code Text2}. */
    public static final int TEXT_SECONDARY = 0xFF9AA8D0;
    /** The pack's {@code Text3}. */
    public static final int TEXT_DISABLED = 0xFF66749E;
    /** Text drawn on a blue fill. */
    public static final int TEXT_ON_SNOW = 0xFF060B1A;

    // --- Status ------------------------------------------------------------------------------

    /** The pack's {@code Green}. */
    public static final int SUCCESS = 0xFF2FD36B;
    /** The pack's {@code Orange}. */
    public static final int WARNING = 0xFFFF9F2E;
    /** The pack's {@code Red}. */
    public static final int DANGER = 0xFFFF5468;

    // --- Glass (the shared card material) ----------------------------------------------------

    /**
     * The glass surface's light, mirroring {@code res/drawable/glass_card.xml} so a Compose card and
     * a View card read as the same material. A flat fill with a hairline reads as a grey rectangle;
     * what makes a surface read as glass is a brighter top edge, a sheen that falls off downward and
     * a rim that is brighter at the top than the bottom.
     */
    public static final int GLASS_RIM_TOP = 0x66FFFFFF;
    public static final int GLASS_RIM_BOTTOM = 0x1FFFFFFF;
    public static final int GLASS_SHEEN = 0x1AFFFFFF;
    public static final int GLASS_SHEEN_END = 0x00FFFFFF;

    // --- Derived: the three schemes ----------------------------------------------------------

    /** One scheme's window, surface, raised surface and stroke. */
    public static final class Scheme {
        public final int window;
        public final int surface;
        public final int surfaceRaised;
        public final int stroke;

        Scheme(int window, int surface, int surfaceRaised, int stroke) {
            this.window = window;
            this.surface = surface;
            this.surfaceRaised = surfaceRaised;
            this.stroke = stroke;
        }
    }

    private static final Scheme DARK_NAVY =
            new Scheme(NAVY_WINDOW, NAVY_SURFACE, NAVY_SURFACE_RAISED, NAVY_STROKE);
    private static final Scheme AMOLED =
            new Scheme(AMOLED_WINDOW, AMOLED_SURFACE, AMOLED_SURFACE_RAISED, NAVY_STROKE);
    private static final Scheme LIGHT =
            new Scheme(0xFFF3F6FB, 0xFFFFFFFF, 0xFFE8EEF8, 0xFFD3DEEE);

    /**
     * The scheme for a theme.
     *
     * @param amoled true for the true-black theme; ignored when {@code light} is set, because a
     *     light theme is never AMOLED.
     */
    public static Scheme scheme(boolean light, boolean amoled) {
        if (light) return LIGHT;
        return amoled ? AMOLED : DARK_NAVY;
    }

    /** The accent to use: the user's wallpaper accent, or snow blue when there is none. */
    public static int accent(Integer resolvedAccent) {
        return resolvedAccent == null ? SNOW_BLUE : resolvedAccent;
    }

    /**
     * The toggle's on-track colour: snow blue, mixed toward the accent so a wallpaper accent still
     * shows while the toggle stays recognisably snow blue.
     */
    public static int toggleOnTrack(int accent) {
        return mix(SNOW_BLUE, accent, 0.35f);
    }

    /** The toggle thumb colour, so it stays legible on both the on and off tracks. */
    public static int toggleThumb(boolean on) {
        return on ? SNOW_BLUE_LIGHT : TEXT_DISABLED;
    }

    /** The toggle track colour for a state. */
    public static int toggleTrack(boolean on, int accent) {
        return on ? toggleOnTrack(accent) : TOGGLE_TRACK_OFF;
    }

    /**
     * Mixes {@code b} into {@code a} by {@code amount} (0..1), keeping the alpha of {@code a}.
     * Pure integer maths, so it is unit-testable without {@code android.graphics.Color}.
     */
    public static int mix(int a, int b, float amount) {
        float t = Math.max(0f, Math.min(1f, amount));
        int ar = (a >>> 16) & 0xFF, ag = (a >>> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >>> 16) & 0xFF, bg = (b >>> 8) & 0xFF, bb = b & 0xFF;
        int r = (int) (ar + (br - ar) * t);
        int g = (int) (ag + (bg - ag) * t);
        int bl = (int) (ab + (bb - ab) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }
}

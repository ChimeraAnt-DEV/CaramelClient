package org.chimeramc.client.ui.components;

import org.chimeramc.client.util.AppLog;

import android.content.Context;

import org.chimeramc.client.util.PersonalizationManager;

/**
 * The single source of truth for the CaramelClient UI's colours, shape and type.
 *
 * <p>This replaces the retired violet/magenta brand palette and the two separate mod-menu themes
 * ({@code ModMenuTheme} and {@code VipTheme}). Every screen — the radial wheel, the one mod menu, the
 * Closet and the Launch tab — reads from here, so a screen cannot drift onto its own colours.
 *
 * <p>The values are the {@code CaramelClient-UI-pack}'s navy-and-glass palette, adopted verbatim:
 * the pack is the design reference, so its numbers are the ones the client draws. That means a
 * deep-navy window ramp ({@code Abyss #060B1A} → {@code Navy #0A1330} → {@code DeepBlue #0F1B44}),
 * the pack's brighter blue accent ({@code #4A78FF}), its caramel ({@code #F5A742}) and its three-step
 * text ramp. The old "blueberry" ramp ({@code #17191D} on {@code #0F5BE4}) was a grey-on-blue
 * palette; the pack's is navy-on-blue, which is what makes a surface read as glass rather than as a
 * dark rectangle.
 *
 * <p>The user's selectable accent still applies (the mod menus and the wheel draw themselves, so
 * they resolve it here), but <b>the pack's blue is the default</b> — {@link #resolveAccent} falls
 * back to {@link #ACCENT_500} whenever no custom accent has been chosen.
 *
 * <p>Android-free apart from {@link Context} for the accent preference; the constants and the pure
 * derivations are unit-tested without a device.
 */
public final class UiTokens {

    private UiTokens() {
    }

    // --- Colours: window and surfaces --------------------------------------------------------

    /** The window panel fill (alpha 0.96) over the pack's {@code Abyss}. */
    public static final int BG_WINDOW = 0xF5060B1A;
    /** A panel surface, the pack's {@code Navy}. */
    public static final int BG_SURFACE = 0xFF0A1330;
    /** A raised surface, the pack's {@code DeepBlue}. */
    public static final int BG_SURFACE_HOVER = 0xFF0F1B44;
    /** The titlebar: one step under {@link #BG_SURFACE}, so it reads as a recessed rail. */
    public static final int BG_TITLEBAR = 0xFF081026;

    /** 1dp borders and dividers. */
    public static final int STROKE_HAIRLINE = 0x12FFFFFF;  // rgba(255,255,255,0.07)

    // --- Colours: accent ---------------------------------------------------------------------

    /** Toggle ON, selected rail icon, primary button. The pack's {@code Blue}. */
    public static final int ACCENT_500 = 0xFF4A78FF;
    /** Hover/pressed lighter, gradient end. The pack's {@code BlueLight}. */
    public static final int ACCENT_400 = 0xFF628CFF;
    /** Icons on dark, focus ring. A step lighter still, for a ring that must be unmistakable. */
    public static final int ACCENT_300 = 0xFF7FA0FF;
    /** Tinted selected-row background. The pack's {@code DeepBlue} family, darkened for a wash. */
    public static final int ACCENT_900 = 0xFF152252;
    /** Section tint. */
    public static final int NAVY_TINT = 0xFF16224A;

    // --- Colours: text -----------------------------------------------------------------------

    /** The pack's {@code Text}. */
    public static final int TEXT_PRIMARY = 0xFFEAF0FF;
    /** The pack's {@code Text2}. */
    public static final int TEXT_SECONDARY = 0xFF9AA8D0;
    /** The pack's {@code Text3}. */
    public static final int TEXT_DISABLED = 0xFF66749E;

    public static final int TOGGLE_OFF_TRACK = 0xFF232C40;

    // --- Colours: status (status only) -------------------------------------------------------

    /** The pack's {@code Green}. */
    public static final int SUCCESS = 0xFF2FD36B;
    /** The pack's {@code Orange}. */
    public static final int WARNING = 0xFFFF9F2E;
    /** The pack's {@code Red}. */
    public static final int DANGER = 0xFFFF5468;

    // --- Translucent, over the game -----------------------------------------------------------

    public static final int CHIP_BG = 0xC7101823;                 // rgba(16,24,35,0.78)
    public static final int CHIP_BORDER = 0x1AFFFFFF;             // rgba(255,255,255,0.10)

    // --- Shape (dp) --------------------------------------------------------------------------

    /**
     * The single radius scale (U10): 8 / 12 / 16 / 28dp. Every surface picks one of the four —
     * a bespoke radius is how two cards that should match end up one pixel apart.
     */
    public static final int RADIUS_SMALL = 8;
    public static final int RADIUS_MEDIUM = 12;
    public static final int RADIUS_LARGE = 16;
    public static final int RADIUS_XLARGE = 28;

    public static final int RADIUS_WINDOW = RADIUS_XLARGE;
    public static final int RADIUS_CARD = RADIUS_LARGE;
    public static final int RADIUS_CHIP = RADIUS_MEDIUM;
    public static final int RADIUS_BUTTON = RADIUS_LARGE;
    /** A full pill: half the height, so the drawable clamps it to a stadium. */
    public static final int RADIUS_TOGGLE = 999;

    /**
     * The single spacing scale (U9): 4 / 8 / 12 / 16 / 24 / 32dp. Any margin or padding in a
     * Caramel surface is one of these, so vertical rhythm is consistent by construction.
     */
    public static final int SPACE_1 = 4;
    public static final int SPACE_2 = 8;
    public static final int SPACE_3 = 12;
    public static final int SPACE_4 = 16;
    public static final int SPACE_6 = 24;
    public static final int SPACE_8 = 32;

    // --- Elevation ---------------------------------------------------------------------------

    public static final float WINDOW_SHADOW_RADIUS = 24f;
    public static final float WINDOW_SHADOW_ALPHA = 0.35f;

    // --- Type (sp) ---------------------------------------------------------------------------

    public static final float TEXT_TITLE = 16f;
    public static final int TITLE_WEIGHT = 600;
    public static final float TEXT_ROW_TITLE = 13f;
    public static final int ROW_TITLE_WEIGHT = 500;
    public static final float TEXT_CAPTION = 11f;
    public static final int CAPTION_WEIGHT = 400;
    /** The UI's one variable family (U6/U8): weights are axis values, not NORMAL/BOLD. */
    public static final float TEXT_SECTION = 10f;
    public static final int SECTION_WEIGHT = 600;
    public static final float SECTION_LETTER_SPACING = 0.08f;

    /**
     * The five type roles (U8), in descending size: display, title, body, label, caption. A
     * surface picks a role, never a raw sp value, so two screens showing the same kind of text
     * cannot drift apart.
     */
    public static final float TEXT_DISPLAY = 22f;
    public static final int DISPLAY_WEIGHT = 600;
    /** Alias of {@link #TEXT_TITLE} — the role name is what new code should use. */
    public static final float TEXT_TITLE_ROLE = TEXT_TITLE;
    public static final float TEXT_BODY = 14f;
    public static final int BODY_WEIGHT = 400;
    /** Alias of {@link #TEXT_ROW_TITLE}. */
    public static final float TEXT_LABEL = TEXT_ROW_TITLE;

    // --- Motion ------------------------------------------------------------------------------

    public static final long MOTION_DEFAULT_MS = 160L;
    public static final long MOTION_PRESS_MS = 80L;
    public static final float MOTION_OPEN_OVERSHOOT = 0.6f;
    public static final long MOTION_MAX_MS = 240L;

    /**
     * The three durations the whole UI animates on.
     *
     * <p>A decorative transition used to be a literal at the call site, so the same "fade a panel
     * in" ran at 140ms here, 180ms there and 220ms somewhere else, and nothing said which was
     * intended. Three steps - a quick state change, a standard transition and a slow reveal - are
     * enough to express every case and few enough that a new surface picks one instead of inventing
     * a number. A duration that is not one of these is a deliberate exception and should say so in
     * a comment, which is the point: the scale makes the exceptions visible.
     */
    public static final long MOTION_FAST_MS = 100L;
    public static final long MOTION_STANDARD_MS = 200L;
    public static final long MOTION_SLOW_MS = 300L;

    /**
     * The three spring presets.
     *
     * <p>They are the shape of a spring, not a duration: a spring settles on its own, so the
     * damping ratio is what decides whether it bounces. Naming the three uses (a press that must
     * not wobble, a move that may, an arrival that should) means a surface asks for the intent
     * rather than picking a ratio, and the reduced-motion scheme can flatten all three in one
     * place.
     */
    public static final class Springs {
        /**
         * A press or hover: no bounce, high stiffness, so the response is immediate.
         *
         * <p>The values are the ones this codebase already animated on, named so a caller asks for
         * "a press" rather than picking a ratio: damping 1.0 is
         * {@code SpringForce.DAMPING_RATIO_NO_BOUNCY} and stiffness 10000 is
         * {@code SpringForce.STIFFNESS_HIGH}. They are written as literals rather than as references
         * so this class stays free of any Android type, which is what keeps it unit-testable.
         */
        public static final float PRESS_DAMPING = 1.0f;
        public static final float PRESS_STIFFNESS = 10000f;
        /** A move or slide: {@code SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY}. */
        public static final float MOVE_DAMPING = 0.5f;
        /** An arrival: a softer bounce, so it reads as a settle rather than a snap. */
        public static final float ARRIVE_DAMPING = 0.72f;

        private Springs() {
        }
    }

    /**
     * The two motion schemes (U11). Every animated surface reads its duration and easing from
     * here rather than hard-coding a spring, so "Reduce motion" is one switch instead of a hunt
     * through the codebase. {@link #REDUCED} is a short linear fade — still a transition, so a
     * screen does not pop, but with no travel and no overshoot.
     */
    public static final class Motion {
        public final long durationMs;
        public final float damping;
        public final float stiffness;
        public final boolean linear;

        private Motion(long durationMs, float damping, float stiffness, boolean linear) {
            this.durationMs = durationMs;
            this.damping = damping;
            this.stiffness = stiffness;
            this.linear = linear;
        }

        /** Springy, expressive motion (the Material 3 Expressive default). */
        public static final Motion EXPRESSIVE =
                new Motion(MOTION_DEFAULT_MS, 0.78f, 380f, false);
        /** Short, flat motion for Reduce motion / Reduce transparency. */
        public static final Motion REDUCED =
                new Motion(120L, 1f, 0f, true);
    }

    /**
     * The motion scheme to use, honouring the app's Reduce motion switch and the platform's
     * "remove animations" developer/accessibility setting.
     */
    public static Motion motionScheme(boolean reduceMotion, boolean platformAnimationsOff) {
        return (reduceMotion || platformAnimationsOff) ? Motion.REDUCED : Motion.EXPRESSIVE;
    }

    // --- Component geometry (dp) -------------------------------------------------------------

    public static final int TOGGLE_WIDTH = 36;
    public static final int TOGGLE_HEIGHT = 20;
    public static final int TOGGLE_THUMB = 16;
    public static final int TOGGLE_THUMB_INSET = 2;
    public static final int TOGGLE_SLIDE_MS = 140;

    public static final int ROW_HEIGHT = 44;
    public static final int RAIL_WIDTH = 56;
    public static final int RAIL_BUTTON = 40;
    public static final int TITLEBAR_HEIGHT = 48;
    public static final int MIN_TOUCH_TARGET = 48;

    public static final int WINDOW_MAX_WIDTH_DP = 720;
    public static final float WINDOW_WIDTH_FRACTION = 0.66f;
    public static final float WINDOW_HEIGHT_FRACTION = 0.78f;

    /**
     * The accent to draw with: the theme's primary (dynamic colour when the wallpaper provides
     * one, the brand colour otherwise). Falls back to {@link #ACCENT_500} with no context.
     */
    public static int resolveAccent(Context context) {
        if (context == null) return ACCENT_500;
        try {
            int accent = new PersonalizationManager(context).getAccentColor();
            if (accent != 0) return accent;
        } catch (Throwable ignored) {
            AppLog.warnOnce("CaramelClient", "UiTokens:260", "Ignored a failure at UiTokens:260", ignored);
        }
        return ACCENT_500;
    }

    /** The accent for the pressed/hover state (a lighter tint of the accent). */
    public static int accentHover(int accent) {
        return blend(accent, 0xFFFFFFFF, 0.18f);
    }

    /** The accent blended toward the surface, for a selected/enabled fill. */
    public static int accentWash(int accent, float amount) {
        return blend(BG_SURFACE, accent, amount);
    }

    /** The accent at an explicit alpha. Pure ARGB packing (no android.graphics.Color). */
    public static int withAlpha(int color, int alpha) {
        int a = Math.max(0, Math.min(255, alpha));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    /** Linear blend of two colours; {@code amount} is clamped to 0..1. Pure integer maths. */
    public static int blend(int base, int over, float amount) {
        float a = Math.max(0f, Math.min(1f, amount));
        int br = (base >>> 16) & 0xFF, bg = (base >>> 8) & 0xFF, bb = base & 0xFF;
        int or = (over >>> 16) & 0xFF, og = (over >>> 8) & 0xFF, ob = over & 0xFF;
        int r = (int) (br * (1 - a) + or * a);
        int g = (int) (bg * (1 - a) + og * a);
        int b = (int) (bb * (1 - a) + ob * a);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** Relative luminance per WCAG, for the contrast-ratio tests. */
    public static double luminance(int color) {
        double r = channel((color >>> 16) & 0xFF);
        double g = channel((color >>> 8) & 0xFF);
        double b = channel(color & 0xFF);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double channel(int c) {
        double s = c / 255.0;
        return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
    }

    /** The WCAG contrast ratio between two opaque colours. */
    public static double contrastRatio(int a, int b) {
        double la = luminance(a);
        double lb = luminance(b);
        double lighter = Math.max(la, lb);
        double darker = Math.min(la, lb);
        return (lighter + 0.05) / (darker + 0.05);
    }

    /**
     * Whether a surface colour is navy (blue-dominant) rather than a neutral grey.
     *
     * <p>The pack's whole look rests on this: a surface whose blue channel is not the strongest is a
     * grey rectangle, which is the "unfinished" look the navy ramp exists to replace. The check is
     * deliberately strict — blue must be at least as strong as both other channels — so a colour that
     * merely leans slightly blue is not mistaken for navy.
     */
    public static boolean isBlueDominant(int color) {
        int r = (color >>> 16) & 0xFF;
        int g = (color >>> 8) & 0xFF;
        int b = color & 0xFF;
        return b >= r && b >= g;
    }
}

package org.chimeramc.client.ui.components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * The shared primary/secondary button: a 14dp-radius rounded rectangle, the primary one an accent
 * gradient with a soft glow, the secondary a surface with a hairline. Heights and touch targets are
 * pure so a test can assert the 48dp minimum.
 */
public final class UiButton {

    private UiButton() {
    }

    public enum Style { PRIMARY, SECONDARY }

    /**
     * How far the slab face sits above its darker edge, in dp.
     *
     * <p>Matches the {@code bg_dock_play} drawable, which draws the edge 3dp proud at the bottom. The
     * constant lives here so the code-side press geometry and the drawable cannot drift: the pressed
     * face drops by exactly this, so the edge vanishes under it and the button reads as depressed.
     */
    public static final float EDGE_DEPTH_DP = 3f;

    /**
     * How far the face drops on press, in px.
     *
     * <p>Zero at rest and the edge depth when pressed. A negative edge depth (a bad caller) clamps to
     * zero so the face is never lifted off the button.
     */
    public static float faceOffsetPx(boolean pressed, float edgeDepthPx) {
        if (!pressed) return 0f;
        return Math.max(0f, edgeDepthPx);
    }

    /** The edge still visible under the face at a press state, in px. Never negative. */
    public static float edgeVisiblePx(boolean pressed, float edgeDepthPx) {
        float depth = Math.max(0f, edgeDepthPx);
        return pressed ? 0f : depth;
    }

    /**
     * The pressed fill for a resting colour: a little lighter, so the face catches the light as it
     * goes down. The alpha is preserved, so a translucent button stays translucent.
     */
    public static int pressFill(int resting) {
        return pressFill(resting, true);
    }

    /** The pressed fill, or the resting colour unchanged when the button is disabled. */
    public static int pressFill(int resting, boolean enabled) {
        if (!enabled) return resting;
        return UiTokens.blend(resting, 0xFFFFFFFF, 0.12f);
    }

    /**
     * Draws the focus ring around a button.
     *
     * <p>The app is driven by a controller and a keyboard as well as touch, so a focused button must
     * be unmistakable. A 2dp white ring inset by the stroke width, drawn outside the fill so it reads
     * on any accent. No-op when not focused.
     *
     * @param focusColor the ring colour (the caller supplies the token)
     */
    public static void drawFocusRing(Canvas c, Paint paint, RectF b, float radius, boolean focused,
                                     int focusColor) {
        if (!focused) return;
        paint.reset();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        float stroke = Math.max(2f, radius / 7f);
        paint.setStrokeWidth(stroke);
        paint.setColor(focusColor);
        float inset = stroke / 2f;
        c.drawRoundRect(new RectF(b.left + inset, b.top + inset, b.right - inset, b.bottom - inset),
                radius, radius, paint);
    }

    /** Draws the button. {@code radius} and {@code glowRadius} are px. */
    public static void draw(Canvas c, Paint paint, RectF b, float radius, int accent, Style style,
                            boolean pressed, boolean enabled) {
        if (style == Style.PRIMARY) {
            int start = enabled ? accent : UiTokens.blend(accent, UiTokens.BG_SURFACE, 0.6f);
            int end = enabled ? UiTokens.accentHover(accent)
                    : UiTokens.blend(accent, UiTokens.BG_SURFACE, 0.6f);
            if (pressed && enabled) {
                start = UiTokens.blend(start, 0xFFFFFFFF, 0.12f);
                end = UiTokens.blend(end, 0xFFFFFFFF, 0.12f);
            }
            UiDraw.gradientRect(c, paint, b.left, b.top, b.right, b.bottom, radius, start, end);
        } else {
            paint.reset();
            paint.setAntiAlias(true);
            paint.setColor(pressed ? UiTokens.BG_SURFACE_HOVER : UiTokens.BG_SURFACE);
            c.drawRoundRect(b, radius, radius, paint);
            UiDraw.hairline(c, paint, b.left, b.top, b.right, b.bottom, radius,
                    Math.max(1f, radius / 14f), UiTokens.STROKE_HAIRLINE);
        }
    }

    /** A soft accent glow behind the primary button (a translucent rounded rect). */
    public static void drawGlow(Canvas c, Paint paint, RectF b, float radius, int accent,
                                float blurRadius, float alpha) {
        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(UiTokens.withAlpha(accent, (int) (alpha * 255)));
        paint.setShadowLayer(blurRadius, 0f, 0f, UiTokens.withAlpha(accent, (int) (alpha * 255)));
        c.drawRoundRect(new RectF(b.left, b.top, b.right, b.bottom), radius, radius, paint);
        paint.clearShadowLayer();
    }
}

package org.chimeramc.client.ui.navigation;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

/**
 * Draws the laid-out bounds of the nav entries over the nav itself (item 80).
 *
 * <p>It is a {@link Drawable} added to the nav's {@link android.view.ViewOverlay}, so it paints over
 * the chrome without becoming a child of it: no layout is affected, nothing is measured differently,
 * and removing it is one call. That matters because the whole point is to inspect the real geometry,
 * and a debug aid that changes the geometry it is inspecting is worthless.
 *
 * <p>The rectangle is the view's bounds in its parent's coordinates, offset by the view's
 * translation, so it matches what is actually on screen. A clipped entry (see
 * {@link NavDebugBounds#looksClipped}) is outlined in red and the rest in a translucent green, so
 * the failing one is obvious at a glance rather than needing the log.
 */
public final class NavBoundsOverlay extends Drawable {

    private final View[] targets;
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect scratch = new Rect();

    public NavBoundsOverlay(View[] targets) {
        this.targets = targets == null ? new View[0] : targets;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(2f);
    }

    @Override
    public void draw(Canvas canvas) {
        for (View view : targets) {
            if (view == null || view.getVisibility() != View.VISIBLE) continue;
            scratch.set(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
            scratch.offset((int) view.getTranslationX(), (int) view.getTranslationY());
            NavDebugBounds.Entry entry = new NavDebugBounds.Entry(
                    "", scratch.left, scratch.top, scratch.right, scratch.bottom,
                    view.getMeasuredWidth(), true);
            stroke.setColor(NavDebugBounds.looksClipped(entry)
                    ? Color.argb(220, 255, 64, 64)
                    : Color.argb(160, 64, 220, 128));
            canvas.drawRect(scratch, stroke);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        stroke.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(android.graphics.ColorFilter colorFilter) {
        stroke.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return android.graphics.PixelFormat.TRANSLUCENT;
    }
}

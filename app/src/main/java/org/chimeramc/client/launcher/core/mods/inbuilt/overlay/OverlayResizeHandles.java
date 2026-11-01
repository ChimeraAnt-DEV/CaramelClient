package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.view.View;

/**
 * The four corner handles drawn over the selected overlay in the HUD editor (item 221).
 *
 * <p>It is a {@link Drawable} installed as the widget's foreground rather than a sibling view. The
 * overlays live in their own windows, so a layer added to the activity's content root would have to
 * translate between window coordinate spaces and would drift; a foreground is drawn in the widget's
 * own bounds and is therefore always aligned, at any position, in both the window and the
 * content-root fallback path.
 *
 * <p>The geometry comes from {@link OverlayResizeHandle}; this class only paints, and allocates
 * nothing in {@code draw}.
 */
final class OverlayResizeHandles extends Drawable {

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float sizePx;

    OverlayResizeHandles(float density, int accentColor) {
        sizePx = Math.max(6f, OverlayResizeHandle.HIT_DP * density * 0.3f);
        fill.setColor(accentColor);
        stroke.setColor(0xFFFFFFFF);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(Math.max(1f, density));
    }

    /** Installs the handles on the widget. Idempotent. */
    static void attach(View view, float density, int accentColor) {
        if (view == null) return;
        if (view.getForeground() instanceof OverlayResizeHandles) return;
        view.setForeground(new OverlayResizeHandles(density, accentColor));
    }

    /** Removes the handles from the widget. */
    static void detach(View view) {
        if (view == null) return;
        if (view.getForeground() instanceof OverlayResizeHandles) {
            view.setForeground(null);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        int width = getBounds().width();
        int height = getBounds().height();
        float left = getBounds().left;
        float top = getBounds().top;
        drawHandle(canvas, left, top);
        drawHandle(canvas, left + width - sizePx, top);
        drawHandle(canvas, left, top + height - sizePx);
        drawHandle(canvas, left + width - sizePx, top + height - sizePx);
    }

    private void drawHandle(Canvas canvas, float x, float y) {
        canvas.drawRect(x, y, x + sizePx, y + sizePx, fill);
        canvas.drawRect(x, y, x + sizePx, y + sizePx, stroke);
    }

    @Override
    public void setAlpha(int alpha) {
        fill.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        fill.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}

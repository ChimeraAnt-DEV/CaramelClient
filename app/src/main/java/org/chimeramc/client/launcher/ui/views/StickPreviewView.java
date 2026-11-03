package org.chimeramc.client.launcher.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import org.chimeramc.client.launcher.controller.StickResponsePreview;

/**
 * Draws where a stick physically is and what the profile makes of it.
 *
 * <p>The raw reading is a hollow ring; the filtered output is a filled dot. When the two coincide
 * the profile is passing the reading through; when the dot stays at the centre while the ring moves,
 * the player can see their own dead zone swallow the travel, which is the whole point of showing it
 * rather than a bare slider.
 *
 * <p>The geometry is a circle and two points, so the view owns no state: it reads a
 * {@link StickResponsePreview} and paints. Nothing is allocated in {@code onDraw}.
 */
public class StickPreviewView extends View {

    /** How many concentric guides to draw, so deflection is readable without a grid. */
    private static final int RING_COUNT = 3;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private StickResponsePreview preview;
    private int accentColor = 0xFF3E6FF0;
    private int mutedColor = 0xFF5C6B7E;
    private int deadZoneColor = 0x33FFFFFF;

    public StickPreviewView(Context context) {
        super(context);
        init();
    }

    public StickPreviewView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StickPreviewView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dp(1.5f));
        paint.setStyle(Paint.Style.FILL);
    }

    /** The reading to draw. A null preview paints an empty pad. */
    public void setPreview(StickResponsePreview preview) {
        this.preview = preview;
        invalidate();
    }

    /** Colours follow the theme so the preview matches the rest of the screen. */
    public void setColors(int accent, int muted, int deadZone) {
        accentColor = accent;
        mutedColor = muted;
        deadZoneColor = deadZone;
        invalidate();
    }

    /** Repaints after the caller feeds a new raw reading. */
    public void refresh() {
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        float cx = width / 2f;
        float cy = height / 2f;
        float radius = Math.min(cx, cy) - dp(6f);
        if (radius <= 0f) return;

        strokePaint.setColor(mutedColor);
        for (int i = 1; i <= RING_COUNT; i++) {
            float r = radius * i / RING_COUNT;
            canvas.drawCircle(cx, cy, r, strokePaint);
        }

        float deadZone = preview == null ? 0f : preview.deadZoneRadius();
        if (deadZone > 0f) {
            float r = deadZone * radius;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(deadZoneColor);
            canvas.drawCircle(cx, cy, r, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        if (preview == null) {
            return;
        }

        // Screen y grows downward, so the stick's +y (forward) is drawn upward.
        float rawPx = cx + preview.rawX() * radius;
        float rawPy = cy - preview.rawY() * radius;
        float outPx = cx + preview.filteredX() * radius;
        float outPy = cy - preview.filteredY() * radius;

        strokePaint.setColor(mutedColor);
        strokePaint.setStrokeWidth(dp(2f));
        canvas.drawCircle(rawPx, rawPy, dp(9f), strokePaint);
        strokePaint.setStrokeWidth(dp(1.5f));

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(preview.isOutsideDeadZone() ? accentColor : mutedColor);
        canvas.drawCircle(outPx, outPy, dp(7f), paint);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}

package org.chimeramc.client.launcher.ui.splash;

import org.chimeramc.client.util.ApiLevels;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * The splash's loader: a smooth circle spinning in the bottom-right corner.
 *
 * <p>This replaces the 3D scene (floating island, pickaxe, block break). That scene drew a
 * particle system, a terrain mesh and an animated pickaxe every frame on the cold-start path, for a
 * screen the user looks at for a couple of seconds. One arc rotating on a hardware-accelerated
 * canvas costs a single draw call.
 *
 * <p>The rotation is driven by the view's own animation clock, so it stops with the view and never
 * keeps a timer alive after the splash is gone.
 */
public final class SplashSpinner extends View {

    /** One full turn, in milliseconds. */
    private static final long TURN_MS = 900L;

    /** The arc's sweep, in degrees: a three-quarter ring with a visible gap. */
    private static final float SWEEP_DEGREES = 280f;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private float angleDegrees;
    private long lastFrameMs;

    public SplashSpinner(Context context) {
        this(context, null);
    }

    public SplashSpinner(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        trackPaint.setColor(Color.argb(56, 255, 255, 255));

        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);
        // A snow-blue accent that reads on every background we ship.
        arcPaint.setColor(Color.rgb(127, 178, 229));

        setWillNotDraw(false);
    }

    /** The colour of the moving arc, so the splash can follow the user's accent. */
    public void setAccentColor(int color) {
        arcPaint.setColor(color);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        final float stroke = Math.max(3f, Math.min(w, h) * 0.11f);
        trackPaint.setStrokeWidth(stroke);
        arcPaint.setStrokeWidth(stroke);
        final float inset = stroke / 2f;
        bounds.set(inset, inset, w - inset, h - inset);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bounds.isEmpty()) return;

        canvas.drawArc(bounds, 0f, 360f, false, trackPaint);
        canvas.drawArc(bounds, angleDegrees, SWEEP_DEGREES, false, arcPaint);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        lastFrameMs = 0L;
        // The ticker is driven by the choreographer, so it advances with the display and stops when
        // the view is detached.
        if (ApiLevels.atLeastP()) {
            postOnAnimation(ticker);
        } else {
            postDelayed(ticker, 16L);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        removeCallbacks(ticker);
        super.onDetachedFromWindow();
    }

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            final long now = android.os.SystemClock.uptimeMillis();
            if (lastFrameMs != 0L) {
                final long delta = Math.min(now - lastFrameMs, 100L);
                angleDegrees = (angleDegrees + 360f * delta / TURN_MS) % 360f;
                invalidate();
            }
            lastFrameMs = now;
            if (ApiLevels.atLeastP()) {
                postOnAnimation(this);
            } else {
                postDelayed(this, 16L);
            }
        }
    };
}

package org.chimeramc.client.launcher.ui.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import org.chimeramc.client.util.PersonalizationManager;

/**
 * The launch screen's rotating status ring.
 *
 * <p>A thin ring that turns steadily and whose colour follows the launch state, so the screen reads
 * as "working" without a spinner and without a wall of log text. The rotation is a single
 * {@link ValueAnimator} on the view's phase; the colour comes from {@link LaunchRingPalette}, which
 * owns the state mapping. Both the animation and the colour respect Reduce motion: with motion
 * reduced the ring holds still and only its colour changes.
 *
 * <p>Everything it needs is one call to {@link #setState(boolean, boolean, int)}; the view never
 * reads the launch state itself.
 */
public class LaunchRingView extends View {

    private static final float PERIOD_MS = 1400f;
    private static final float STROKE_DP = 3.5f;
    private static final float SIZE_DP = 56f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();

    private LaunchRingPalette.Phase phase = LaunchRingPalette.Phase.PREPARING;
    private float rotationDegrees;
    private ValueAnimator spin;
    private boolean animationsEnabled = true;

    public LaunchRingView(Context context) {
        this(context, null);
    }

    public LaunchRingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        try {
            animationsEnabled = new PersonalizationManager(context).isShowAnimations();
        } catch (Throwable t) {
            animationsEnabled = true;
        }
    }

    /** Sets the ring's state: a failure wins, then readiness, then progress drives the phase. */
    public void setState(boolean failed, boolean ready, int progress) {
        LaunchRingPalette.Phase next = LaunchRingPalette.phaseFor(failed, ready, progress);
        if (next != phase) {
            phase = next;
            updateShader();
            invalidate();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startSpinIfNeeded();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopSpin();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) {
            startSpinIfNeeded();
        } else {
            stopSpin();
        }
    }

    private void startSpinIfNeeded() {
        if (!animationsEnabled || spin != null) return;
        spin = ValueAnimator.ofFloat(0f, 360f);
        spin.setDuration((long) PERIOD_MS);
        spin.setRepeatCount(ValueAnimator.INFINITE);
        spin.setInterpolator(new LinearInterpolator());
        spin.addUpdateListener(a -> {
            rotationDegrees = (float) a.getAnimatedValue();
            invalidate();
        });
        spin.start();
    }

    private void stopSpin() {
        if (spin != null) {
            spin.cancel();
            spin = null;
        }
    }

    private void updateShader() {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        if (cx <= 0f || cy <= 0f) return;
        // A sweep from the trailing colour to the leading colour and back, so the ring has a
        // direction of travel rather than a single flat tint.
        int near = LaunchRingPalette.nearColor(phase);
        int far = LaunchRingPalette.farColor(phase);
        paint.setShader(new SweepGradient(cx, cy, new int[]{near, far, near}, new float[]{0f, 0.5f, 1f}));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float stroke = STROKE_DP * getResources().getDisplayMetrics().density;
        paint.setStrokeWidth(stroke);
        float inset = stroke / 2f + 1f;
        bounds.set(inset, inset, w - inset, h - inset);
        updateShader();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = (int) (SIZE_DP * getResources().getDisplayMetrics().density);
        setMeasuredDimension(
                resolveSize(size, widthMeasureSpec),
                resolveSize(size, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bounds.isEmpty()) return;
        canvas.save();
        canvas.rotate(rotationDegrees, getWidth() / 2f, getHeight() / 2f);
        canvas.drawArc(bounds, 0f, 300f, false, paint);
        canvas.restore();
    }
}

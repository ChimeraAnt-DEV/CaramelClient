package org.chimeramc.pojavcontrols;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

/**
 * The calibration target for item 223.
 *
 * <p>A horizontal track of a known length is drawn across the view; the player drags from the left
 * marker to the right one, and the distance their finger actually travelled is measured. The
 * measurement and the resulting multiplier live in {@link PointerCalibration}, so this view only
 * draws and forwards the numbers — the rule that decides whether a drag counts is unit tested.
 *
 * <p>The measured distance is the straight-line distance from the down point to the up point, not
 * the path length: a player who overshoots and comes back has still told us how far their thumb
 * needs to move, and summing every jitter of the path would report a wildly inflated distance.
 */
public final class PointerCalibrationView extends View {

    /** Called with the measured multiplier, or with the current value if the drag was a miss. */
    public interface Listener {
        void onCalibrated(float multiplier);
    }

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dragPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Listener listener;

    private float startX = Float.NaN;
    private float startY = Float.NaN;
    private float currentX = Float.NaN;
    private float currentY = Float.NaN;
    private boolean dragging;
    private float lastMultiplier = PointerCalibration.DEFAULT_MULTIPLIER;

    public PointerCalibrationView(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        float density = getResources().getDisplayMetrics().density;
        trackPaint.setColor(0xFF2A323C);
        trackPaint.setStrokeWidth(6f * density);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        markerPaint.setColor(0xFF4AE0A0);
        markerPaint.setStrokeWidth(4f * density);
        markerPaint.setStyle(Paint.Style.STROKE);
        dragPaint.setColor(0xFFFFC24A);
        dragPaint.setStrokeWidth(4f * density);
        dragPaint.setStrokeCap(Paint.Cap.ROUND);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(14f * density);
        setBackgroundColor(0xCC101316);
    }

    /** The length the player is asked to drag across, in pixels: the gap between the two markers. */
    float targetDistancePx() {
        float margin = 20f * getResources().getDisplayMetrics().density;
        return Math.max(1f, getWidth() - 2f * margin);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float margin = 20f * density;
        float left = margin;
        float right = Math.max(left + 1f, getWidth() - margin);
        float centerY = getHeight() / 2f;

        canvas.drawLine(left, centerY, right, centerY, trackPaint);
        canvas.drawCircle(left, centerY, 14f * density, markerPaint);
        canvas.drawCircle(right, centerY, 14f * density, markerPaint);

        if (dragging && !Float.isNaN(startX) && !Float.isNaN(currentX)) {
            canvas.drawLine(startX, startY, currentX, currentY, dragPaint);
        }

        String label = getContext().getString(R.string.pojav_controls_calibration_hint);
        canvas.drawText(label, margin, margin + textPaint.getTextSize(), textPaint);
        String value = getContext().getString(R.string.pojav_controls_calibration_value,
                Math.round(lastMultiplier * 100f));
        canvas.drawText(value, margin, getHeight() - margin, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                startY = event.getY();
                currentX = startX;
                currentY = startY;
                dragging = true;
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                currentX = event.getX();
                currentY = event.getY();
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                dragging = false;
                float measured = (float) Math.hypot(event.getX() - startX, event.getY() - startY);
                lastMultiplier = PointerCalibration.multiplierFor(targetDistancePx(), measured);
                if (listener != null) listener.onCalibrated(lastMultiplier);
                invalidate();
                return true;
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                invalidate();
                return true;
            default:
                return true;
        }
    }
}

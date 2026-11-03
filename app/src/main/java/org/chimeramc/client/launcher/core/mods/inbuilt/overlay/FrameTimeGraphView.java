package org.chimeramc.client.launcher.core.mods.inbuilt.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import org.chimeramc.client.core.mods.inbuilt.overlay.FrameTimeGraph;
import org.chimeramc.client.core.mods.inbuilt.overlay.FrameTimeHistory;

/**
 * Draws the FPS overlay's frame-time graph: a polyline of the last
 * {@link FrameTimeHistory#CAPACITY} frame intervals, oldest on the left.
 *
 * <p>The mapping lives in the pure {@link FrameTimeGraph}; this view only strokes the result. Every
 * paint, path and scratch array is a field so {@link #onDraw} allocates nothing — the overlay shares
 * a frame budget with the game, and an allocation there is a stutter.
 */
public final class FrameTimeGraphView extends View {

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final float[] xs = new float[FrameTimeHistory.CAPACITY];
    private final float[] ys = new float[FrameTimeHistory.CAPACITY];

    private FrameTimeHistory history;

    public FrameTimeGraphView(Context context) {
        this(context, null);
    }

    public FrameTimeGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(context.getResources().getDisplayMetrics().density * 1.2f);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        fillPaint.setStyle(Paint.Style.FILL);
    }

    /** Sets the history to draw and repaints. A null history draws nothing. */
    public void setHistory(FrameTimeHistory history) {
        this.history = history;
        invalidate();
    }

    public void setLineColor(int color) {
        linePaint.setColor(color);
        fillPaint.setColor(color);
        invalidate();
    }

    /** Repaints from the current history; call on the refresh tick, not every frame. */
    public void refresh() {
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (history == null || history.isEmpty()) return;
        int width = getWidth() - getPaddingLeft() - getPaddingRight();
        int height = getHeight() - getPaddingTop() - getPaddingBottom();
        if (width <= 0 || height <= 0) return;

        int count = FrameTimeGraph.points(history, width, height, xs, ys);
        if (count <= 0) return;

        canvas.save();
        canvas.translate(getPaddingLeft(), getPaddingTop());
        path.rewind();
        path.moveTo(xs[0], ys[0]);
        for (int i = 1; i < count; i++) {
            path.lineTo(xs[i], ys[i]);
        }
        canvas.drawPath(path, linePaint);
        canvas.restore();
    }
}

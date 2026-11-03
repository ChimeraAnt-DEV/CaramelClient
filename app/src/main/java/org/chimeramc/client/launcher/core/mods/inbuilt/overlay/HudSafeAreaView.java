package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

import java.util.List;

/**
 * Draws the HUD editor's safe-area frame, cutout and grid.
 *
 * <p>Without it the editor lets a widget be dropped under a notch or the gesture bar, where it is
 * clipped and the user has no way to know why. The frame shows exactly the box the editor clamps
 * into, so "why will it not go further left" is answered on screen rather than by trial.
 *
 * <p>It is a single overlay drawn over the editor, not a view per widget: one {@code onDraw}, no
 * per-frame allocation (the paints and the grid {@link android.graphics.Path} are built once), and
 * it is created with {@code FLAG_NOT_TOUCHABLE} by its host so it can never eat a drag.
 */
public final class HudSafeAreaView extends View {

    private final Paint framePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cutoutPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF frameRect = new RectF();

    private OverlaySafeArea.Rect safe;
    private List<Rect> cutouts = java.util.Collections.emptyList();
    private int gridPx;

    public HudSafeAreaView(Context context) {
        this(context, null);
    }

    public HudSafeAreaView(Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
        framePaint.setStyle(Paint.Style.STROKE);
        framePaint.setStrokeWidth(context.getResources().getDisplayMetrics().density * 1.5f);
        framePaint.setColor(0x66FFFFFF);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setColor(0x22FFFFFF);
        cutoutPaint.setStyle(Paint.Style.FILL);
        cutoutPaint.setColor(0xCC000000);
        setWillNotDraw(false);
    }

    /** Updates the frame; a null safe rect hides everything. */
    public void setGeometry(OverlaySafeArea.Rect safe, List<Rect> cutouts, int gridPx) {
        this.safe = safe;
        this.cutouts = cutouts == null ? java.util.Collections.emptyList() : cutouts;
        this.gridPx = gridPx;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        OverlaySafeArea.Rect area = safe;
        if (area == null || area.width() <= 0 || area.height() <= 0) return;

        if (gridPx > 0) {
            for (int x = area.left + gridPx; x < area.right; x += gridPx) {
                canvas.drawLine(x, area.top, x, area.bottom, gridPaint);
            }
            for (int y = area.top + gridPx; y < area.bottom; y += gridPx) {
                canvas.drawLine(area.left, y, area.right, y, gridPaint);
            }
        }

        frameRect.set(area.left, area.top, area.right, area.bottom);
        canvas.drawRect(frameRect, framePaint);

        for (Rect cutout : cutouts) {
            if (cutout == null) continue;
            canvas.drawRect(cutout, cutoutPaint);
        }
    }
}

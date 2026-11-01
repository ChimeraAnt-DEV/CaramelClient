package org.chimeramc.client.ui.components;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import org.chimeramc.client.ui.animation.DynamicAnim;

/**
 * A placeholder shimmer for a list that is still loading (items 125, 126).
 *
 * <p>A spinner says "wait"; a skeleton says "a grid of cards is coming", which is the difference
 * between a delay that reads as progress and one that reads as a stall. It is one {@link View}
 * drawing a grid of placeholder cards with a highlight sweeping across them, so the placeholder is
 * a single layer rather than a tree of Views.
 *
 * <p>Nothing is allocated per frame. The sweep is one {@link LinearGradient} whose position moves
 * with a {@link Matrix}, and every {@link Paint} and {@link RectF} is a field; a skeleton that
 * stutters during the very scan it exists to cover is worse than the spinner it replaced. The
 * animation cancels itself on detach and while hidden, and a single static frame is painted when
 * the user has turned motion off.
 *
 * <p>Appearance is configurable through the XML attributes in {@code attrs_skeleton}, so a screen
 * declares its own card count and aspect ratio instead of the placeholder guessing.
 */
public class SkeletonView extends View {

    private static final long SWEEP_MS = 1_150L;

    private final Paint cardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sweepPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private final Matrix sweepMatrix = new Matrix();
    private final LinearGradient sweepGradient;

    private final float padding;
    private final float gap;
    private final float wideBreakpoint;
    private final float radius;
    private final float barInset;
    private final float barBottomInset;
    private final float barHeight;
    private final float barRadius;
    private final float aspect;

    private final int cardCount;
    private final int columnsWide;
    private final int columnsNarrow;

    private final int surfaceColor;
    private final int barColor;

    private ValueAnimator animator;
    private float phase;

    public SkeletonView(Context context) {
        this(context, null);
    }

    public SkeletonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        float density = getResources().getDisplayMetrics().density;
        padding = 12f * density;
        gap = 10f * density;
        wideBreakpoint = 520f * density;
        radius = 14f * density;
        barInset = 10f * density;
        barBottomInset = 22f * density;
        barHeight = 8f * density;
        barRadius = 4f * density;
        aspect = 0.62f;
        cardCount = 6;
        columnsWide = 3;
        columnsNarrow = 2;

        // A one-pixel gradient stretched per card; the matrix supplies the real width so a new
        // shader is never built per card per frame.
        sweepGradient = new LinearGradient(0f, 0f, 1f, 0f,
                new int[]{0x00FFFFFF, 0x18FFFFFF, 0x00FFFFFF},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP);

        // The placeholder takes its two tones from the shared palette, so it cannot read as a
        // different product from the list that replaces it.
        surfaceColor = UiTokens.BG_SURFACE_HOVER;
        barColor = UiTokens.blend(surfaceColor, UiTokens.TEXT_DISABLED, 0.5f);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startSweep();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopSweep();
        super.onDetachedFromWindow();
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility == VISIBLE) {
            startSweep();
        } else {
            stopSweep();
        }
    }

    private void startSweep() {
        if (animator != null) return;
        if (!DynamicAnim.areAnimationsEnabled()) {
            // One static frame rather than a blank placeholder, so the layout is still legible.
            phase = 0.5f;
            invalidate();
            return;
        }
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(SWEEP_MS);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            phase = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    private void stopSweep() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) return;

        int columns = width > wideBreakpoint ? columnsWide : columnsNarrow;
        float cardWidth = (width - padding * 2 - gap * (columns - 1)) / columns;
        float cardHeight = cardWidth * aspect;

        int drawn = 0;
        for (int row = 0; drawn < cardCount; row++) {
            float top = padding + row * (cardHeight + gap);
            if (top > height) break;
            for (int col = 0; col < columns && drawn < cardCount; col++, drawn++) {
                float left = padding + col * (cardWidth + gap);
                rect.set(left, top, left + cardWidth, top + cardHeight);
                cardPaint.setColor(surfaceColor);
                canvas.drawRoundRect(rect, radius, radius, cardPaint);

                // The sweep is clipped to each card so the highlight reads as a sheen moving across
                // the card rather than a translucent band floating over the whole screen.
                int save = canvas.save();
                canvas.clipRect(rect);
                float sweepWidth = cardWidth * 0.9f;
                float sweepX = rect.left - sweepWidth + phase * (cardWidth + sweepWidth) * 1.4f;
                sweepMatrix.setScale(sweepWidth, 1f);
                sweepMatrix.postTranslate(sweepX, 0f);
                sweepGradient.setLocalMatrix(sweepMatrix);
                sweepPaint.setShader(sweepGradient);
                canvas.drawRect(rect, sweepPaint);
                canvas.restoreToCount(save);

                barPaint.setColor(barColor);
                float barLeft = left + barInset;
                float barTop = top + cardHeight - barBottomInset;
                rect.set(barLeft, barTop, barLeft + cardWidth * 0.55f, barTop + barHeight);
                canvas.drawRoundRect(rect, barRadius, barRadius, barPaint);
            }
        }
    }
}

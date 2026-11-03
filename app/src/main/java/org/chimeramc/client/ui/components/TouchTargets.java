package org.chimeramc.client.ui.components;

/**
 * Touch-target geometry.
 *
 * <p>A 27dp icon button is the right visual size and the wrong touch size: Material's minimum is
 * 48dp, and anything under it is a target a user misses. The fix is not to inflate the button —
 * that would push the row apart — but to keep the visual and enlarge only the region that accepts
 * the touch, which is what a {@code TouchDelegate} does.
 *
 * <p>The arithmetic of that is pure and is kept here so it can be tested without a device: given
 * the view's bounds and its container's, work out the rectangle to hand the delegate. The rule is
 * simple — grow the view symmetrically to the minimum, then clamp to the container so the delegate
 * never claims space outside its parent (a delegate whose rect escapes the parent simply does not
 * receive the events, so the button would look enlarged and behave as before).
 */
public final class TouchTargets {

    /** Material's minimum interactive size. */
    public static final int MIN_TARGET_DP = 48;

    private TouchTargets() {
    }

    /**
     * The rectangle to pass to a {@code TouchDelegate}, as {@code [left, top, right, bottom]}.
     *
     * <p>Returns the view's own bounds when they already meet the minimum, so the caller can skip
     * installing a delegate at all.
     *
     * @param viewLeft   the view's left edge within its parent
     * @param viewTop    the view's top edge within its parent
     * @param viewRight  the view's right edge within its parent
     * @param viewBottom the view's bottom edge within its parent
     * @param parentWidth  the parent's width, or 0 to skip the horizontal clamp
     * @param parentHeight the parent's height, or 0 to skip the vertical clamp
     * @param minSize    the minimum target edge in pixels
     */
    public static int[] delegateBounds(int viewLeft, int viewTop, int viewRight, int viewBottom,
                                       int parentWidth, int parentHeight, int minSize) {
        int width = viewRight - viewLeft;
        int height = viewBottom - viewTop;
        int growX = Math.max(0, minSize - width);
        int growY = Math.max(0, minSize - height);
        int left = viewLeft - growX / 2;
        int top = viewTop - growY / 2;
        int right = viewRight + (growX - growX / 2);
        int bottom = viewBottom + (growY - growY / 2);

        if (parentWidth > 0) {
            // Clamp rather than shift: a target that hangs off one edge must give the space back on
            // the other, or a button flush against the start of its parent would lose half of it.
            if (left < 0) {
                right = Math.min(parentWidth, right - left);
                left = 0;
            }
            if (right > parentWidth) {
                left = Math.max(0, left - (right - parentWidth));
                right = parentWidth;
            }
        }
        if (parentHeight > 0) {
            if (top < 0) {
                bottom = Math.min(parentHeight, bottom - top);
                top = 0;
            }
            if (bottom > parentHeight) {
                top = Math.max(0, top - (bottom - parentHeight));
                bottom = parentHeight;
            }
        }
        return new int[]{left, top, right, bottom};
    }

    /** True when the view's own bounds already meet the minimum in both directions. */
    public static boolean meetsMinimum(int width, int height, int minSize) {
        return width >= minSize && height >= minSize;
    }

    /**
     * Widens the touch area of every small interactive view under {@code root}.
     *
     * <p>A parent can hold only one {@link android.view.TouchDelegate}, so several small siblings
     * are grouped: the parent gets a single {@link CompositeTouchDelegate} that holds one entry per
     * child. Installing them one by one would silently keep only the last, leaving the earlier
     * buttons looking enlarged and behaving as before.
     *
     * <p>Runs after layout, because the bounds are not known before then.
     */
    public static void expandTouchTargetsRecursively(android.view.View root) {
        if (root == null) return;
        root.post(() -> expandUnder(root));
    }

    private static void expandUnder(android.view.View root) {
        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) root;
            groupSmallChildren(group);
            for (int i = 0; i < group.getChildCount(); i++) {
                expandUnder(group.getChildAt(i));
            }
        }
    }

    /**
     * Gives {@code parent} one delegate covering each child whose touch area is under the minimum.
     *
     * <p>A child that is already big enough is skipped, and a parent with no small children gets no
     * delegate at all, so the common case adds nothing.
     */
    private static void groupSmallChildren(android.view.ViewGroup parent) {
        if (parent.getWidth() <= 0 || parent.getHeight() <= 0) return;
        final int minPx = Math.round(MIN_TARGET_DP
                * parent.getResources().getDisplayMetrics().density);
        java.util.List<android.view.View> small = new java.util.ArrayList<>(2);
        for (int i = 0; i < parent.getChildCount(); i++) {
            android.view.View child = parent.getChildAt(i);
            if (child == null || !child.isClickable()
                    || child.getVisibility() != android.view.View.VISIBLE) {
                continue;
            }
            if (needsWidening(child.getLeft(), child.getTop(), child.getRight(), child.getBottom(),
                    minPx)) {
                small.add(child);
            }
        }
        if (small.isEmpty()) return;
        // The parent delegate's rectangle must cover every child target, or the parent rejects an
        // event before the composite is consulted. That union is computed purely (and tested).
        int[][] rects = new int[small.size()][];
        for (int i = 0; i < small.size(); i++) {
            android.view.View child = small.get(i);
            rects[i] = delegateBounds(child.getLeft(), child.getTop(), child.getRight(),
                    child.getBottom(), parent.getWidth(), parent.getHeight(), minPx);
        }
        int[] union = unionOf(rects);
        CompositeTouchDelegate composite = new CompositeTouchDelegate(
                new android.graphics.Rect(union[0], union[1], union[2], union[3]), parent);
        for (int i = 0; i < small.size(); i++) {
            android.view.View child = small.get(i);
            composite.add(new android.view.TouchDelegate(
                    new android.graphics.Rect(rects[i][0], rects[i][1], rects[i][2], rects[i][3]),
                    child));
        }
        parent.setTouchDelegate(composite);
    }

    /** True when a view's own bounds are under the minimum and so need a delegate. */
    public static boolean needsWidening(int left, int top, int right, int bottom, int minSize) {
        return (right - left) < minSize || (bottom - top) < minSize;
    }

    /**
     * The smallest rectangle covering every input rectangle, as {@code [left, top, right, bottom]}.
     *
     * <p>Pure, so the composite delegate's bounds rule is unit-tested: a union that fails to cover a
     * child makes the parent reject that child's events, which is the silent half of the
     * one-delegate-per-parent problem.
     *
     * @throws IllegalArgumentException when {@code rects} is null or empty
     */
    public static int[] unionOf(int[][] rects) {
        if (rects == null || rects.length == 0) {
            throw new IllegalArgumentException("unionOf needs at least one rectangle");
        }
        int left = Integer.MAX_VALUE;
        int top = Integer.MAX_VALUE;
        int right = Integer.MIN_VALUE;
        int bottom = Integer.MIN_VALUE;
        for (int[] r : rects) {
            if (r == null || r.length < 4) continue;
            left = Math.min(left, r[0]);
            top = Math.min(top, r[1]);
            right = Math.max(right, r[2]);
            bottom = Math.max(bottom, r[3]);
        }
        return new int[]{left, top, right, bottom};
    }
}

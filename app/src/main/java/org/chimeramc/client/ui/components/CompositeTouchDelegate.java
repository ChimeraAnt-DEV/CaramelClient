package org.chimeramc.client.ui.components;

import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.TouchDelegate;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link TouchDelegate} that forwards to several delegates (item 339).
 *
 * <p>A parent view can hold only <em>one</em> touch delegate, so widening three 32dp icon buttons
 * in one toolbar the obvious way silently keeps only the last: the first two look enlarged and
 * behave exactly as before. The fix is to hand the parent a single delegate that tries each
 * child's rectangle in turn.
 *
 * <p>Each attempt gets its own copy of the event, because {@code TouchDelegate.onTouchEvent}
 * rewrites the event's coordinates in place before dispatching. Reusing one event across attempts
 * would let the first delegate's offset leak into the second, so a tap meant for the second button
 * would land somewhere between them.
 */
public final class CompositeTouchDelegate extends TouchDelegate {

    private final List<TouchDelegate> delegates = new ArrayList<>(2);

    /** The smallest rectangle covering every child, which is what the parent's delegate is given. */
    public CompositeTouchDelegate(Rect bounds, View delegateView) {
        super(bounds, delegateView);
    }

    /** Adds a child delegate. Order is the order they are tried; the first hit wins. */
    public void add(TouchDelegate delegate) {
        if (delegate != null) delegates.add(delegate);
    }

    /** How many child delegates are installed; used by the tests and by the debug overlay. */
    public int size() {
        return delegates.size();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event == null) return false;
        for (int i = 0; i < delegates.size(); i++) {
            MotionEvent copy = MotionEvent.obtain(event);
            try {
                if (delegates.get(i).onTouchEvent(copy)) return true;
            } finally {
                copy.recycle();
            }
        }
        return false;
    }
}

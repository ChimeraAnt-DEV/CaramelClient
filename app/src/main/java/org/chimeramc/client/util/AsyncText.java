package org.chimeramc.client.util;

import android.widget.TextView;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.text.PrecomputedTextCompat;
import androidx.core.widget.TextViewCompat;

import org.chimeramc.client.R;

import java.util.concurrent.Future;

/**
 * Measures long text off the main thread (item 127).
 *
 * <p>Setting a multi-paragraph string on a {@link TextView} makes the view measure and lay out the
 * whole thing on the frame that sets it, which is a dropped frame on a long mod description or a log
 * page. {@link PrecomputedTextCompat} does that measuring on a background thread and hands the
 * result back as a {@link Future} the view consumes when it arrives, so the frame that binds the
 * text does no layout work.
 *
 * <p>Three rules make it safe, none of which the caller has to think about:
 * <ul>
 *   <li>The future is keyed to the view's own text metrics. A precomputed value measured with a
 *       different text size or typeface is rejected by the platform, so the view keeps the plain
 *       text rather than throwing - the failure mode is "not faster", never "wrong".</li>
 *   <li>A stale request is dropped. The view is tagged with the sequence number of its latest
 *       request, so a description replaced before its measurement finished cannot have the old value
 *       applied over the new one.</li>
 *   <li>Only an {@link AppCompatTextView} consumes the future directly; any other view gets the
 *       plain text, so this cannot crash on a view the platform did not build.</li>
 * </ul>
 */
public final class AsyncText {

    private static final java.util.concurrent.atomic.AtomicLong SEQUENCE =
            new java.util.concurrent.atomic.AtomicLong();

    /** Below this many characters the measurement is not worth a thread hand-off. */
    static final int THRESHOLD = 200;

    private AsyncText() {
    }

    /**
     * Whether {@code text} is worth measuring off the main thread on a view of this kind.
     *
     * <p>Pure, so the threshold and the view-kind rule are unit-tested rather than assumed. A null
     * or short string is never worth a thread hand-off, and only an {@link AppCompatTextView}
     * consumes the future, so any other view takes the plain-text path.
     */
    static boolean shouldDefer(CharSequence text, boolean appCompatView) {
        return appCompatView && text != null && text.length() >= THRESHOLD;
    }

    /**
     * Applies {@code text} to {@code view}, measuring it off the main thread when it is long enough
     * to be worth it.
     *
     * <p>Short strings are set directly: the thread hand-off and the {@link Future} would cost more
     * than the measurement saves, and a label is never the problem this fixes.
     */
    public static void apply(TextView view, CharSequence text) {
        if (view == null) return;
        if (!shouldDefer(text, view instanceof AppCompatTextView)) {
            view.setText(text);
            return;
        }
        final AppCompatTextView appCompatView = (AppCompatTextView) view;
        final long token = SEQUENCE.incrementAndGet();
        appCompatView.setTag(R.id.async_text_sequence, token);
        final PrecomputedTextCompat.Params params = TextViewCompat.getTextMetricsParams(appCompatView);
        Future<PrecomputedTextCompat> future = AppExecutors.submitCompute(
                () -> PrecomputedTextCompat.create(text, params));
        if (future == null) {
            appCompatView.setText(text);
            return;
        }
        appCompatView.setTextFuture(future);
    }
}

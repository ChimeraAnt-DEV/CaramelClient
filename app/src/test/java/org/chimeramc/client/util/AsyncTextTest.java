package org.chimeramc.client.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pins the rule that decides when text is measured off the main thread (item 127).
 *
 * <p>The rule is the whole safety of the async path: a future whose metrics do not match is
 * rejected by the platform, so deferring a short string or deferring on a view that cannot consume
 * a future is wasted work at best. Both halves are asserted here rather than assumed.
 */
public class AsyncTextTest {

    @Test
    public void aLongStringOnAnAppCompatViewIsDeferred() {
        String longText = repeat('x', AsyncText.THRESHOLD);
        assertTrue(AsyncText.shouldDefer(longText, true));
    }

    @Test
    public void aStringJustBelowTheThresholdIsNotDeferred() {
        String shortText = repeat('x', AsyncText.THRESHOLD - 1);
        assertFalse(AsyncText.shouldDefer(shortText, true));
    }

    @Test
    public void anEmptyOrNullStringIsNeverDeferred() {
        assertFalse(AsyncText.shouldDefer("", true));
        assertFalse(AsyncText.shouldDefer(null, true));
    }

    @Test
    public void aViewThatCannotConsumeTheFutureTakesThePlainPath() {
        // A plain TextView has no setTextFuture; handing it one would throw.
        assertFalse(AsyncText.shouldDefer(repeat('x', AsyncText.THRESHOLD * 4), false));
    }

    @Test
    public void theThresholdIsLargeEnoughThatALabelNeverPaysForAThreadHop() {
        // A settings row label or a button caption is well under this; if the threshold were small
        // every ordinary bind would cross a thread for nothing.
        assertTrue(AsyncText.THRESHOLD >= 100);
    }

    private static String repeat(char c, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) builder.append(c);
        return builder.toString();
    }
}

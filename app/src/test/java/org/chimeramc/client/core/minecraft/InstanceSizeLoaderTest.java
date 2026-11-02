package org.chimeramc.client.core.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pins {@link InstanceSizeLoader}: one walk per instance even under a rebinding storm, a null result
 * never cached, and "not measured yet" distinct from a measured zero.
 */
public class InstanceSizeLoaderTest {

    /** Runs the dispatched work inline, so the test is deterministic and needs no thread. */
    private static final class InlineDispatcher implements InstanceSizeLoader.Dispatcher {
        int count;

        @Override
        public void dispatch(Runnable work) {
            count++;
            work.run();
        }
    }

    @Test
    public void aRequestedSizeIsMeasuredOnceAndThenCached() {
        AtomicInteger measures = new AtomicInteger();
        InlineDispatcher dispatcher = new InlineDispatcher();
        InstanceSizeLoader loader = new InstanceSizeLoader(key -> {
            measures.incrementAndGet();
            return 1234L;
        }, dispatcher);

        List<String> notified = new ArrayList<>();
        loader.request("instance-a", notified::add);
        assertEquals(1, measures.get());
        assertEquals(Long.valueOf(1234L), loader.cached("instance-a"));
        assertEquals(1, notified.size());

        // A second request for the same key is served from the cache: no second walk.
        loader.request("instance-a", notified::add);
        assertEquals(1, measures.get());
    }

    @Test
    public void aFailedMeasurementIsNotCachedAndIsNotReportedAsZero() {
        AtomicInteger measures = new AtomicInteger();
        InstanceSizeLoader loader = new InstanceSizeLoader(key -> {
            measures.incrementAndGet();
            return -1L;
        }, new InlineDispatcher());

        loader.request("instance-b", key -> {});
        assertEquals(1, measures.get());
        assertNull(loader.cached("instance-b"));
    }

    @Test
    public void aMeasuredZeroIsARealReadingAndIsCached() {
        InstanceSizeLoader loader = new InstanceSizeLoader(key -> 0L, new InlineDispatcher());
        loader.request("empty-instance", key -> {});
        assertEquals(Long.valueOf(0L), loader.cached("empty-instance"));
    }

    @Test
    public void aNullOrEmptyKeyIsANoOp() {
        AtomicInteger measures = new AtomicInteger();
        InlineDispatcher dispatcher = new InlineDispatcher();
        InstanceSizeLoader loader = new InstanceSizeLoader(key -> {
            measures.incrementAndGet();
            return 1L;
        }, dispatcher);

        loader.request(null, key -> {});
        loader.request("", key -> {});
        assertEquals(0, measures.get());
        assertEquals(0, dispatcher.count);
    }

    @Test
    public void invalidateForcesTheNextRequestToReMeasure() {
        AtomicInteger measures = new AtomicInteger();
        InstanceSizeLoader loader = new InstanceSizeLoader(key -> {
            measures.incrementAndGet();
            return 5L;
        }, new InlineDispatcher());

        loader.request("k", key -> {});
        assertEquals(1, measures.get());
        loader.invalidate("k");
        assertNull(loader.cached("k"));
        loader.request("k", key -> {});
        assertEquals(2, measures.get());
    }

    @Test
    public void clearForgetsEveryFigure() {
        InstanceSizeLoader loader = new InstanceSizeLoader(key -> 9L, new InlineDispatcher());
        loader.request("a", key -> {});
        loader.request("b", key -> {});
        loader.clear();
        assertNull(loader.cached("a"));
        assertNull(loader.cached("b"));
    }

    @Test
    public void aNullListenerDoesNotBreakTheWalk() {
        InstanceSizeLoader loader = new InstanceSizeLoader(key -> 42L, new InlineDispatcher());
        loader.request("x", null);
        assertEquals(Long.valueOf(42L), loader.cached("x"));
    }

    @Test
    public void theWorkIsDispatchedRatherThanRunOnTheCallingThread() {
        List<Runnable> pending = new ArrayList<>();
        InstanceSizeLoader loader = new InstanceSizeLoader(key -> 1L, pending::add);
        loader.request("y", key -> {});
        // Nothing measured yet: the work was handed to the dispatcher, not run inline.
        assertEquals(1, pending.size());
        assertNull(loader.cached("y"));
        pending.get(0).run();
        assertTrue(loader.cached("y") != null);
    }
}

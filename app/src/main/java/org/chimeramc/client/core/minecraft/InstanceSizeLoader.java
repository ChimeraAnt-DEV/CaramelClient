package org.chimeramc.client.core.minecraft;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Measures each instance's disk use off the main thread and remembers the answer.
 *
 * <p>Walking an instance's files touches thousands of entries, so it cannot run while a card is
 * being bound. The loader keeps a cache keyed by instance directory: a card that is bound with a
 * cached figure shows it immediately, and a card whose instance has not been measured yet schedules
 * one measurement and is repainted through the callback when it finishes.
 *
 * <p>In-flight requests are tracked so a fast scroll that rebinds the same card twenty times
 * schedules one walk, not twenty. The cache is process-lifetime only: a size is a cheap thing to
 * recompute and a stale one after an uninstall would be worse than a moment of "…".
 *
 * <p>The measuring itself is supplied by the caller, so the class is Android-free and unit-testable
 * without a device or a real filesystem.
 */
public final class InstanceSizeLoader {

    /** Runs the actual measurement; must be safe to call off the main thread. */
    public interface Measurer {
        /** The total bytes the instance occupies, or a negative value when it cannot be measured. */
        long measureBytes(String instanceKey);
    }

    /** Schedules a runnable off the main thread. */
    public interface Dispatcher {
        void dispatch(Runnable work);
    }

    /** Notified on the caller's thread when a measurement finishes, with the key that finished. */
    public interface Listener {
        void onMeasured(String instanceKey);
    }

    private final Measurer measurer;
    private final Dispatcher dispatcher;
    private final Map<String, Long> cache = new HashMap<>();
    private final Set<String> inFlight = new HashSet<>();

    public InstanceSizeLoader(Measurer measurer, Dispatcher dispatcher) {
        this.measurer = measurer;
        this.dispatcher = dispatcher;
    }

    /**
     * The cached size for {@code instanceKey}, or null when it has not been measured yet.
     *
     * <p>Null rather than 0, because 0 is a real size (an empty instance) and the card must be able
     * to tell "not known yet" from "nothing there".
     */
    public Long cached(String instanceKey) {
        if (instanceKey == null) return null;
        return cache.get(instanceKey);
    }

    /**
     * Requests a measurement for {@code instanceKey} unless it is already cached or in flight.
     *
     * <p>A null or empty key is a no-op: a card with no directory identity cannot be measured and
     * must not spin up a walk that would never match it back.
     */
    public void request(String instanceKey, Listener listener) {
        if (instanceKey == null || instanceKey.isEmpty()) return;
        if (cache.containsKey(instanceKey) || inFlight.contains(instanceKey)) return;
        inFlight.add(instanceKey);
        dispatcher.dispatch(() -> {
            long bytes = measurer.measureBytes(instanceKey);
            synchronized (InstanceSizeLoader.this) {
                inFlight.remove(instanceKey);
                if (bytes >= 0L) {
                    cache.put(instanceKey, bytes);
                }
            }
            if (bytes >= 0L && listener != null) {
                listener.onMeasured(instanceKey);
            }
        });
    }

    /** Forgets one instance's figure, so the next request re-measures it (a cache clear). */
    public synchronized void invalidate(String instanceKey) {
        if (instanceKey == null) return;
        cache.remove(instanceKey);
    }

    /** Forgets every figure. */
    public synchronized void clear() {
        cache.clear();
    }
}

package org.chimeramc.client.core.cosmetics;

import java.util.concurrent.atomic.AtomicReference;

/**
 * A one-at-a-time guard for the account-skin fetch, keyed by the account it is fetching.
 *
 * <p>The service used a single {@code AtomicBoolean} to stop overlapping fetches. That is right for
 * one account and wrong the moment the account changes: a fetch for the previous account is still in
 * flight, so the fetch for the account that just signed in is <em>dropped</em>. The cache is then
 * still stamped with the previous account's key, {@code isValidFor} rejects it, and the preview
 * resolves to Steve — the "I signed in and got a random default skin" report. The skin does arrive
 * eventually on a later launch, which is why it looked random rather than permanently broken.
 *
 * <p>This guard allows exactly one fetch per key. A request for a different key is not dropped: it
 * supersedes the in-flight one, so the account that is signed in now always wins. Superseding is
 * correct rather than merely convenient, because the fetch writes to a cache keyed by account and
 * the newest request is by definition the one whose result the user is waiting to see.
 *
 * <p>Pure apart from one atomic reference, so the supersede/allow/drop rules are unit-tested without
 * a device or a network.
 */
final class SkinFetchGuard {

    /** The key currently being fetched, or null when nothing is in flight. */
    private final AtomicReference<String> inFlight = new AtomicReference<>(null);

    /**
     * Tries to claim the fetch for {@code key}.
     *
     * @return true when the caller should perform the fetch. False only when a fetch for
     *         <em>this same key</em> is already running, because then a second one would be
     *         duplicate work for a result that is already coming.
     */
    boolean tryStart(String key) {
        if (key == null || key.isEmpty()) return false;
        while (true) {
            final String running = inFlight.get();
            if (key.equals(running)) {
                // The same account is already being fetched: a second request is duplicate work.
                return false;
            }
            // Either nothing is in flight, or a *different* account is. In both cases this request
            // should proceed: the newest account is the one whose skin the user is waiting to see,
            // and the cache is keyed by account so a superseded result cannot be mis-attributed.
            if (inFlight.compareAndSet(running, key)) return true;
            // Lost the race with another thread; re-read and decide again.
        }
    }

    /**
     * Marks the fetch for {@code key} finished.
     *
     * <p>Only clears the slot when the finishing fetch is the one that owns it. A superseded fetch
     * that finishes late must not clear the newer fetch's claim, or two fetches for the new key
     * could run at once.
     */
    void finish(String key) {
        if (key == null) return;
        inFlight.compareAndSet(key, null);
    }

    /** The key currently in flight, or null. For diagnostics and tests. */
    String currentKey() {
        return inFlight.get();
    }

    /** True when a fetch is running for any key. */
    boolean isRunning() {
        return inFlight.get() != null;
    }
}

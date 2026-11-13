package org.chimeramc.client.core.nethernet

import org.chimeramc.client.launcher.util.DebugBundle
import java.util.ArrayDeque

/**
 * A bounded, in-memory record of what the connection path did.
 *
 * This exists because the failure it is built for is the one nobody can reproduce: the launcher hung
 * for a user, they sent a screenshot, and there was nothing to look at. A ring buffer keeps the last
 * few minutes of connection events in memory so the answer is available at the moment it is asked
 * for.
 *
 * Three rules, each a deliberate limit rather than an oversight:
 *
 * * **In memory only.** Nothing here is ever written to disk. A connection log is exactly the kind of
 *   file that ends up in a bug report, and this one is not worth the risk; if it is lost when the app
 *   is killed, that is the intended trade.
 * * **Redacted on the way in, not on the way out.** [record] runs the text through the launcher's
 *   existing [DebugBundle.redact] before it is stored, so there is no window in which an unredacted
 *   token exists in this buffer and no code path that can forget to redact.
 * * **Bounded in both dimensions.** [maxEntries] caps the count and [MAX_DETAIL_CHARS] caps each
 *   entry, so a misbehaving caller cannot grow it without limit.
 *
 * Not thread-safe by construction: it is guarded by its own lock, so a caller on the network thread
 * and a caller on the UI thread both work without the callers coordinating.
 */
class ConnectionDiagnostics(
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
) {

    /** One recorded event. */
    data class Entry(
        /** Milliseconds since the buffer was created, so entries order without a clock. */
        val atMillis: Long,
        /** The stage, matching [NetherNetError.step]. */
        val step: String,
        /** What happened, already redacted and truncated. */
        val detail: String,
        /** Whether this event was a failure. */
        val failure: Boolean,
    )

    private val lock = Any()
    private val entries = ArrayDeque<Entry>()
    private val startedAt = System.nanoTime()

    /**
     * Records an event.
     *
     * [detail] is redacted and truncated here rather than by the caller, so there is exactly one
     * place that can get it wrong.
     */
    fun record(step: String, detail: String, failure: Boolean = false) {
        val safe = DebugBundle.redact(detail).take(MAX_DETAIL_CHARS)
        val entry = Entry(elapsedMillis(), step, safe, failure)
        synchronized(lock) {
            entries.addLast(entry)
            while (entries.size > maxEntries) entries.removeFirst()
        }
    }

    /** Records a failure from the taxonomy. The reason is already user-facing, and is still redacted. */
    fun record(error: NetherNetError) {
        record(error.step, error.reason, failure = true)
    }

    /** A snapshot of the buffer, oldest first. */
    fun snapshot(): List<Entry> = synchronized(lock) { ArrayList(entries) }

    /** The entries as newline-separated text, for a diagnostics report. */
    fun asText(): String = snapshot().joinToString("\n") { entry ->
        val mark = if (entry.failure) "FAIL" else "ok"
        "${entry.atMillis}ms [$mark] ${entry.step}: ${entry.detail}"
    }

    /** Forgets everything. Used by the "clear" affordance and by tests. */
    fun clear() = synchronized(lock) { entries.clear() }

    /** How many entries are held. */
    val size: Int
        get() = synchronized(lock) { entries.size }

    private fun elapsedMillis(): Long = (System.nanoTime() - startedAt) / 1_000_000L

    companion object {
        /** Enough to cover a slow join and the retry after it, without holding the whole session. */
        const val DEFAULT_MAX_ENTRIES = 200

        /** A single detail line longer than this is truncated; nothing useful lives past it. */
        const val MAX_DETAIL_CHARS = 512
    }
}

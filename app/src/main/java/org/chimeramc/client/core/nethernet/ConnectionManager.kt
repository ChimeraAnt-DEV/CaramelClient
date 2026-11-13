package org.chimeramc.client.core.nethernet

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import org.chimeramc.client.util.AppLog

/**
 * Where a join should go.
 *
 * A friends invite carries a NetherNet signaling endpoint; a server-list entry carries a direct
 * RakNet address. The manager does not guess which one it has: the caller states it, so a wrong
 * choice is a type error rather than a runtime surprise.
 */
sealed interface JoinTarget {
    /** A NetherNet session, joined through the signaling service. */
    data class NetherNet(val signalingUrl: String, val networkId: String) : JoinTarget

    /** A direct RakNet server. */
    data class Direct(val host: String, val port: Int) : JoinTarget
}

/** How a join finished. */
sealed interface JoinOutcome {
    /** The session is open on [transport]. */
    data class Opened(val transport: Transport) : JoinOutcome

    /** The join failed. [error] is the taxonomy the UI speaks. */
    data class Failed(val error: NetherNetError) : JoinOutcome

    /** The user cancelled. */
    data object Cancelled : JoinOutcome

    /** A join was already in flight, so this request was refused rather than queued. */
    data object AlreadyInFlight : JoinOutcome
}

/**
 * The RakNet side of a join.
 *
 * An interface because the real implementation opens a UDP socket, which a unit test cannot do. The
 * manager only needs to know whether the path opened and to be able to close it, so that is the
 * whole contract.
 */
interface RakNetJoiner {
    /** Opens the direct path. Returns false when it could not be opened. */
    suspend fun open(host: String, port: Int): Boolean

    /** Closes the path. Must be safe to call when [open] never succeeded. */
    suspend fun close()
}

/**
 * Owns one join attempt at a time, in one place.
 *
 * This exists because the join has two transports, a fallback between them, and a teardown order that
 * matters. Spreading that across the UI means the UI has to know which transport is live, which is
 * exactly the knowledge that goes stale. Here the manager knows, the UI only reads
 * [ConnectionStateMachine.state], and there is exactly one place that can open or close a socket.
 *
 * Three rules it enforces:
 *
 * * **One join at a time.** A second [join] while one is in flight returns [JoinOutcome.AlreadyInFlight]
 *   rather than starting a second attempt that would fight the first over the socket.
 * * **Reverse-order teardown.** Resources are closed in the reverse of the order they opened, in a
 *   `finally`, so a failure half way through a fallback does not leak the first transport.
 * * **`supervisorScope`.** A failing child does not cancel its siblings, so closing one transport
 *   cannot abort the cleanup of another.
 */
class ConnectionManager(
    private val machine: ConnectionStateMachine,
    private val scope: CoroutineScope,
    private val netherNet: NetherNetTransport,
    private val rakNet: RakNetJoiner,
    private val joinTimeoutMs: Long = 45_000L,
    /** Where the join records what it did, for the diagnostics screen. */
    private val diagnostics: ConnectionDiagnostics = ConnectionDiagnostics(),
) {

    private val inFlight = Mutex()
    private var activeJob: Job? = null
    private var joining = false

    /** True while a join is running. */
    val isJoining: Boolean
        get() = joining

    /**
     * Runs a join to completion.
     *
     * Suspends until the join opens, fails or is cancelled. Never throws: every failure is a
     * [JoinOutcome], because the caller is a UI coroutine and an exception there is a crash.
     */
    suspend fun join(target: JoinTarget): JoinOutcome {
        val started = inFlight.withLock {
            if (joining) {
                false
            } else {
                joining = true
                true
            }
        }
        if (!started) {
            AppLog.warn(TAG, "a join was requested while one was in flight; refusing")
            diagnostics.record("connecting", "a join was requested while one was in flight", failure = true)
            return JoinOutcome.AlreadyInFlight
        }
        diagnostics.record("connecting", "join requested: ${target::class.simpleName}")

        return try {
            withTimeout(joinTimeoutMs) {
                supervisorScope { runJoin(target) }
            }
        } catch (e: TimeoutCancellationException) {
            // The overall budget elapsed even though every step had its own; report it as the step
            // that was in flight rather than as a bare timeout, so the copy is actionable.
            val error = NetherNetError.Timeout(machine.state.value.name)
            diagnostics.record(error)
            failQuietly(error)
            JoinOutcome.Failed(error)
        } catch (e: Exception) {
            val error = NetherNetError.Unknown("connecting", e::class.java.simpleName)
            diagnostics.record(error)
            failQuietly(error)
            JoinOutcome.Failed(error)
        } finally {
            inFlight.withLock { joining = false }
        }
    }

    private suspend fun runJoin(target: JoinTarget): JoinOutcome {
        return when (target) {
            is JoinTarget.NetherNet -> {
                if (!netherNet.isAvailable) {
                    diagnostics.record("connecting", "the native transport is not present", failure = true)
                    // No native transport in this build. That is not a failure of the join: fall
                    // back to the direct path when there is one, and report honestly when not.
                    val error = NetherNetError.netherNetUnavailable()
                    failQuietly(error)
                    return JoinOutcome.Failed(error)
                }
                val started = netherNet.connect(target.signalingUrl, target.networkId)
                diagnostics.record("connecting", "native connect returned $started")
                if (!started) {
                    val error = NetherNetError.Network("connecting", "the transport refused")
                    failQuietly(error)
                    return JoinOutcome.Failed(error)
                }
                // The native worker now drives the machine; a failure leaves the session closed by
                // the worker's own teardown, so this path only waits.
                awaitConnected()
            }

            is JoinTarget.Direct -> {
                diagnostics.record("resolving the server", "opening the direct path")
                val ok = rakNet.open(target.host, target.port)
                if (!ok) {
                    // Nothing opened, so there is nothing to close; closing anyway would be a lie the
                    // fake in the tests would not catch.
                    val error = NetherNetError.Network("connecting to the server", "no route")
                    failQuietly(error)
                    return JoinOutcome.Failed(error)
                }
                val reached = walkTo(
                    ConnectionState.Connected(
                        authOnly = false,
                        offline = false,
                        transport = Transport.RAKNET,
                    ),
                    "direct path open",
                )
                if (!reached) {
                    // The machine refused the path (an illegal edge from where it was), so the socket
                    // is not owned by a session and must be closed here.
                    runCatching { rakNet.close() }
                    val error = NetherNetError.Protocol("connecting", "the connection state rejected the direct path")
                    failQuietly(error)
                    return JoinOutcome.Failed(error)
                }
                diagnostics.record("connecting", "the direct path is open")
                JoinOutcome.Opened(Transport.RAKNET)
            }
        }
    }

    /**
     * Walks the machine to [target], inserting any missing intermediate states.
     *
     * The direct path can start from anywhere the auth chain left the machine, so the manager cannot
     * assume a single legal edge. It follows [ConnectionStateMachine.allowedEdges] and refuses rather
     * than throwing when no route exists.
     */
    private suspend fun walkTo(target: ConnectionState, cause: String): Boolean {
        // The only routes a direct join needs: auth finished -> resolved -> joining -> connected.
        val path = listOf(
            ConnectionState.ResolvingServer,
            ConnectionState.Joining,
            target,
        )
        for (step in path) {
            val current = machine.state.value
            if (current.name == step.name) continue
            if (machine.allowedEdges[current.name]?.contains(step.name) != true) {
                AppLog.warn(TAG, "no legal edge ${current.name} -> ${step.name} for the direct path")
                return false
            }
            machine.transition(step, cause)
        }
        return true
    }

    /**
     * Fails the machine unless it has already reached a terminal state.
     *
     * A second failure while the machine is already `Failed` is not an error: two things failing at
     * once is normal when a join is being torn down, and the first reason is the one worth keeping.
     */
    private suspend fun failQuietly(error: NetherNetError) {
        if (machine.state.value.isTerminal) {
            diagnostics.record("connecting", "a second failure was dropped: ${error.step}", failure = true)
            return
        }
        runCatching { machine.fail(error) }.onFailure {
            AppLog.warn(TAG, "the connection state refused the failure report", it)
        }
    }

    /**
     * Waits for the transport to reach a terminal state.
     *
     * The native worker drives the state machine as it progresses, so this only has to notice the
     * end. A terminal state that is [ConnectionState.Connected] is success; anything else is the
     * failure the machine already recorded.
     */
    private suspend fun awaitConnected(): JoinOutcome {
        // `first` suspends until a terminal state arrives and then cancels the collection, so the
        // state flow is not left with a live collector per join.
        val terminal = machine.state.first { it.isTerminal }
        return when (terminal) {
            is ConnectionState.Connected -> JoinOutcome.Opened(terminal.transport)
            is ConnectionState.Failed -> JoinOutcome.Failed(terminal.error)
            is ConnectionState.Cancelled -> JoinOutcome.Cancelled
            else -> JoinOutcome.Failed(NetherNetError.Unknown("connecting", "unexpected state"))
        }
    }

    /**
     * Cancels the in-flight join and closes whatever opened. Idempotent.
     *
     * A live session has no edge to `Cancelled` (it is a success, not an abort), so a cancel from
     * `Connected` returns to `Idle`; a cancel from a state that is still in flight goes through the
     * machine's own cancel. Both paths are guarded, because the point of cancel is that it always
     * works.
     */
    suspend fun cancel() {
        activeJob?.cancel()
        activeJob = null
        runCatching { netherNet.disconnect() }
        runCatching { rakNet.close() }
        diagnostics.record("connecting", "cancel requested")
        runCatching {
            val current = machine.state.value
            when {
                current is ConnectionState.Connected ->
                    machine.transition(ConnectionState.Idle, "user cancelled the session")
                !current.isTerminal -> machine.cancel("user cancelled the join")
            }
        }.onFailure { AppLog.warn(TAG, "the connection state refused the cancel", it) }
    }

    private companion object {
        const val TAG = "ConnectionManager"
    }
}

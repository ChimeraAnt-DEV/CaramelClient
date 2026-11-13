package org.chimeramc.client.core.nethernet

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The single source of truth for where a Bedrock join has got to.
 *
 * Every network step in the Play path reports through [transition]. That is what removes the
 * "Microsoft connecting" hang: a step cannot simply block, because it must first enter a state, and
 * every state that can block has a hard timeout attached to it ([timeoutsMs]) plus a watchdog that
 * fires [ConnectionState.Failed] when it elapses. The UI reads [state] and [elapsedSeconds], so it
 * always has something to show and always has a reason to enable Cancel.
 *
 * Illegal edges are refused rather than followed: [transition] throws [IllegalTransition] when asked
 * to move along an edge that is not in [allowedEdges]. That is deliberate — a skipped step means a
 * precondition was never checked, and the failure it would cause later is much harder to diagnose
 * than an exception at the point of the mistake.
 *
 * @param scope the scope the watchdog and ticker run in; cancelled when the manager's lifetime ends.
 * @param nowMs a monotonic millisecond clock. Defaults to [System.nanoTime]; never wall-clock, so a
 *   device clock change or an NTP step cannot make a timeout fire early or never.
 */
class ConnectionStateMachine(
    private val scope: CoroutineScope,
    private val nowMs: () -> Long = { System.nanoTime() / 1_000_000L },
) {

    /** The timeout for each state that can block, in milliseconds. */
    val timeoutsMs: Map<String, Long> = mapOf(
        "AuthWaitingForUser" to 180_000L,
        "AuthExchanging" to 20_000L,
        "SigningInXboxLive" to 15_000L,
        "AuthorizingXsts" to 15_000L,
        "FetchingMinecraftToken" to 15_000L,
        "ResolvingServer" to 20_000L,
        "SignalingNetherNet" to 20_000L,
        "NegotiatingWebRtc" to 25_000L,
        "Joining" to 30_000L,
    )

    /**
     * The legal edges, as `from -> setOf(to)`.
     *
     * Read this as the connection's shape. Auth is a straight chain; the transport is a chain that
     * starts only once a token exists; [ConnectionState.Connected] is reachable only from
     * [ConnectionState.FetchingMinecraftToken] (auth-only) or [ConnectionState.Joining] (a live
     * transport), which is what makes "Idle straight to Connected" impossible.
     */
    val allowedEdges: Map<String, Set<String>> = mapOf(
        "Idle" to setOf("AuthStarting", "ResolvingServer", "FetchingMinecraftToken", "Failed"),
        "AuthStarting" to setOf("AuthWaitingForUser", "AuthExchanging", "Failed", "Cancelled"),
        "AuthWaitingForUser" to setOf("AuthExchanging", "Failed", "Cancelled"),
        "AuthExchanging" to setOf("SigningInXboxLive", "Failed", "Cancelled"),
        "SigningInXboxLive" to setOf("AuthorizingXsts", "Failed", "Cancelled"),
        "AuthorizingXsts" to setOf("FetchingMinecraftToken", "Failed", "Cancelled"),
        "FetchingMinecraftToken" to setOf("Connected", "ResolvingServer", "Failed", "Cancelled"),
        "ResolvingServer" to setOf("SignalingNetherNet", "Joining", "Failed", "Cancelled"),
        "SignalingNetherNet" to setOf("NegotiatingWebRtc", "Failed", "Cancelled"),
        "NegotiatingWebRtc" to setOf("Joining", "Failed", "Cancelled"),
        "Joining" to setOf("Connected", "Failed", "Cancelled"),
        "Connected" to setOf("Idle", "Failed"),
        "Failed" to setOf("Idle", "AuthStarting", "ResolvingServer", "FetchingMinecraftToken"),
        "Cancelled" to setOf("Idle", "AuthStarting", "ResolvingServer"),
    )

    private val mutex = Mutex()
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Idle)

    /** The current state. Collect this in the UI; it is the only thing the UI needs to read. */
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)

    /** Whole seconds since the current state was entered; drives the "— 6s" indicator. */
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _transitions = MutableSharedFlow<Transition>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Every accepted transition, for the diagnostics buffer and the "Details" list. */
    val transitions = _transitions.asSharedFlow()

    private var enteredAtMs: Long = nowMs()
    private var watchdog: Job? = null
    private var ticker: Job? = null

    /** The in-flight work of the current step, so [cancel] can abort it. */
    private var stepJob: Job? = null

    /** Raised when a caller asks for an edge that [allowedEdges] does not permit. */
    class IllegalTransition(from: String, to: String) :
        IllegalStateException("illegal connection transition $from -> $to")

    /**
     * Moves to [to], or throws [IllegalTransition].
     *
     * [cause] is a short, non-secret label for the reason ("user pressed play", "xsts ok"). It is
     * recorded in the transition log and must never carry a token.
     */
    suspend fun transition(to: ConnectionState, cause: String) {
        mutex.withLock {
            val from = _state.value
            if (!isAllowed(from.name, to.name)) throw IllegalTransition(from.name, to.name)
            val at = nowMs()
            val duration = at - enteredAtMs
            _transitions.tryEmit(Transition(from.name, to.name, cause, at, duration))
            enteredAtMs = at
            _state.value = to
            _elapsedSeconds.value = 0L
            restartWatchdog(to)
            restartTicker(to)
        }
    }

    /** True when [to] may follow [from]. Exposed so tests can assert the shape without a transition. */
    fun isAllowed(from: String, to: String): Boolean = allowedEdges[from]?.contains(to) == true

    /**
     * Cancels the current step and returns to [ConnectionState.Idle].
     *
     * Closes nothing itself — the caller's step job owns its sockets — but it does cancel that job
     * and then wait for it, so by the time this returns no step is still running. That is what makes
     * "no half-open connection" true: the state is Idle and nothing is in flight behind it.
     */
    suspend fun cancel(cause: String = "user cancelled") {
        val job = stepJob
        job?.cancel()
        job?.join()
        mutex.withLock {
            val from = _state.value
            watchdog?.cancel()
            ticker?.cancel()
            if (from !is ConnectionState.Cancelled && from !is ConnectionState.Idle) {
                val at = nowMs()
                _transitions.tryEmit(Transition(from.name, "Cancelled", cause, at, at - enteredAtMs))
                _state.value = ConnectionState.Cancelled
                enteredAtMs = at
            }
            _transitions.tryEmit(Transition("Cancelled", "Idle", "reset", nowMs(), 0L))
            _state.value = ConnectionState.Idle
            _elapsedSeconds.value = 0L
        }
    }

    /** Runs [block] as the current step, so [cancel] can abort it. Replaces any previous step. */
    fun launchStep(block: suspend CoroutineScope.() -> Unit) {
        stepJob = scope.launch(block = block)
    }

    /** Fails the connection from wherever it is; always a legal edge into [ConnectionState.Failed]. */
    suspend fun fail(error: NetherNetError, cause: String = "failed") {
        transition(ConnectionState.Failed(error), cause)
    }

    private fun restartWatchdog(state: ConnectionState) {
        watchdog?.cancel()
        if (!state.isBusy) return
        val timeout = timeoutsMs[state.name] ?: return
        watchdog = scope.launch {
            delay(timeout)
            if (!isActive) return@launch
            // Stop the step before publishing the failure, so nothing is still holding a socket when
            // the UI reacts to the timeout. The step job's own cleanup runs on cancellation.
            stepJob?.cancel()
            mutex.withLock {
                if (_state.value.name == state.name) {
                    val at = nowMs()
                    _transitions.tryEmit(
                        Transition(state.name, "Failed", "timeout", at, at - enteredAtMs),
                    )
                    enteredAtMs = at
                    _state.value = ConnectionState.Failed(NetherNetError.Timeout(stepLabel(state)))
                    _elapsedSeconds.value = 0L
                }
            }
        }
    }

    private fun restartTicker(state: ConnectionState) {
        ticker?.cancel()
        if (!state.isBusy) return
        ticker = scope.launch {
            while (isActive) {
                delay(1_000L)
                _elapsedSeconds.value = (nowMs() - enteredAtMs) / 1_000L
            }
        }
    }

    private companion object {
        /** A human phrase for the state, for the timeout copy: "signing in to Xbox Live". */
        fun stepLabel(state: ConnectionState): String = when (state.name) {
            "AuthWaitingForUser" -> "waiting for you to finish signing in"
            "AuthExchanging" -> "finishing sign-in"
            "SigningInXboxLive" -> "signing in to Xbox Live"
            "AuthorizingXsts" -> "authorizing with Xbox Live"
            "FetchingMinecraftToken" -> "checking your Minecraft account"
            "ResolvingServer" -> "finding the session"
            "SignalingNetherNet" -> "contacting the session"
            "NegotiatingWebRtc" -> "establishing a direct connection"
            "Joining" -> "joining the game"
            else -> "connecting"
        }
    }
}

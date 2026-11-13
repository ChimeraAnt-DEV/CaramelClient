package org.chimeramc.client.core.nethernet

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineScope
import org.chimeramc.client.util.AppLog

/**
 * The Kotlin face of the native NetherNet transport.
 *
 * The native side owns the socket and the protocol; this class owns the *reporting*. It polls the
 * native state on a dedicated dispatcher, maps the integer codes to [ConnectionState], and publishes
 * the result through the same [ConnectionStateMachine] the auth path uses, so the UI has one source
 * of truth regardless of which transport is live.
 *
 * Two rules make it safe to run next to the game thread:
 *
 * * **Nothing blocks the caller.** [connect] starts the native worker and returns; the polling loop
 *   lives on [dispatcher] and is cancelled by [disconnect].
 * * **A missing library is not an error.** When the native library is absent, [connect] reports
 *   `Unsupported` through the state machine and the caller falls back to RakNet. A device without
 *   the arm64 libs should lose a transport, not a feature.
 *
 * [pollIntervalMs] is injectable so a test can drive the loop without sleeping, and [native] is
 * injectable so the mapping logic is tested without the library.
 */
class NetherNetTransport(
    private val machine: ConnectionStateMachine,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val native: NativeNetherNetApi = NativeNetherNetApi.Default,
    private val pollIntervalMs: Long = 250L,
) {

    /**
     * The native calls this transport makes. An interface so a test can script the state sequence
     * without the .so, and so the mapping from codes to states is covered by a fast unit test.
     */
    interface NativeNetherNetApi {
        val isAvailable: Boolean
        fun connect(url: String, networkId: String): Boolean
        fun state(): Int
        fun errorTag(): String?
        fun errorMessage(): String?
        fun errorRetryable(): Boolean
        fun sessionId(): String?
        fun awaitState(timeoutMs: Int): Int
        fun takeCandidate(): String?
        fun disconnect()
        fun isCancelled(): Boolean
        fun stateBudgetMs(stateCode: Int): Int
        fun protocolVersion(): Int
        fun selfTest(): String?

        /** The production implementation, delegating to the JNI object. */
        object Default : NativeNetherNetApi {
            override val isAvailable: Boolean get() = NativeNetherNet.isAvailable
            override fun connect(url: String, networkId: String) =
                NativeNetherNet.nativeConnect(url, networkId)
            override fun state(): Int = NativeNetherNet.nativeState()
            override fun errorTag(): String? = NativeNetherNet.nativeErrorTag()
            override fun errorMessage(): String? = NativeNetherNet.nativeErrorMessage()
            override fun errorRetryable(): Boolean = NativeNetherNet.nativeErrorRetryable()
            override fun sessionId(): String? = NativeNetherNet.nativeSessionId()
            override fun awaitState(timeoutMs: Int): Int = NativeNetherNet.nativeAwaitState(timeoutMs)
            override fun takeCandidate(): String? = NativeNetherNet.nativeTakeCandidate()
            override fun disconnect() = NativeNetherNet.nativeDisconnect()
            override fun isCancelled(): Boolean = NativeNetherNet.nativeIsCancelled()
            override fun stateBudgetMs(stateCode: Int): Int =
                NativeNetherNet.nativeStateBudgetMs(stateCode)
            override fun protocolVersion(): Int = NativeNetherNet.nativeProtocolVersion()
            override fun selfTest(): String? = NativeNetherNet.nativeSelfTest()
        }
    }

    /** The candidate payloads the service relayed, for the ICE layer to consume. */
    private val _candidates = MutableStateFlow<List<String>>(emptyList())
    val candidates: StateFlow<List<String>> = _candidates.asStateFlow()

    /** True when the native library is present. */
    val isAvailable: Boolean get() = native.isAvailable

    private var pollJob: kotlinx.coroutines.Job? = null

    /**
     * Starts a connection and begins reporting.
     *
     * @return true when the native worker started. False means the caller should fall back to
     *   RakNet; the state machine already carries the reason.
     */
    suspend fun connect(url: String, networkId: String): Boolean {
        if (!native.isAvailable) {
            machine.fail(NetherNetError.TransportUnavailable())
            return false
        }
        val started = withContext(dispatcher) {
            runCatching { native.connect(url, networkId) }.getOrDefault(false)
        }
        if (!started) {
            machine.fail(NetherNetError.Network("opening the connection", "the transport refused"))
            return false
        }
        startPolling()
        return true
    }

    /** Stops the native worker and the polling loop. Idempotent. */
    suspend fun disconnect() {
        pollJob?.cancel()
        pollJob = null
        withContext(dispatcher) {
            runCatching { native.disconnect() }
        }
    }

    /**
     * The candidate payloads relayed so far, and clears them.
     *
     * Exposed as a drain rather than as a flow because the ICE layer consumes each candidate exactly
     * once; a flow would make "have I seen this one" the caller's problem.
     */
    fun drainCandidates(): List<String> {
        val drained = _candidates.value
        _candidates.value = emptyList()
        return drained
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = scope.launch(dispatcher) {
            var lastCode = -1
            while (isActive) {
                val code = runCatching { native.state() }.getOrDefault(TransportStateCode.IDLE)
                if (code != lastCode) {
                    lastCode = code
                    applyState(code)
                }
                val candidate = runCatching { native.takeCandidate() }.getOrNull()
                if (candidate != null) {
                    _candidates.value = _candidates.value + candidate
                }
                if (code == TransportStateCode.FAILED || code == TransportStateCode.CLOSED) {
                    return@launch
                }
                delay(pollIntervalMs)
            }
        }
    }

    /**
     * Maps a native state code into the shared state machine.
     *
     * Every code maps to exactly one state, and an unknown code is a protocol failure rather than a
     * silent no-op: a client that ignores a state it does not know is a client that hangs.
     */
    internal suspend fun applyState(code: Int) {
        // AppLog guards every path, so this is safe off-device in a unit test.
        AppLog.warn(TAG, "native transport state $code")
        when (code) {
            TransportStateCode.IDLE -> Unit
            TransportStateCode.SIGNALING -> advanceTo(ConnectionState.SignalingNetherNet, "signaling")
            TransportStateCode.AWAITING_WELCOME ->
                advanceTo(ConnectionState.SignalingNetherNet, "awaiting welcome")
            TransportStateCode.CONNECTING ->
                advanceTo(ConnectionState.NegotiatingWebRtc, "connecting")
            TransportStateCode.CONNECTED -> {
                advanceTo(ConnectionState.Joining, "session open")
                advanceTo(
                    ConnectionState.Connected(
                        authOnly = false,
                        offline = false,
                        transport = Transport.NETHERNET,
                    ),
                    "session open",
                )
            }
            TransportStateCode.CLOSED -> advanceTo(ConnectionState.Cancelled, "closed")
            TransportStateCode.FAILED -> machine.fail(nativeError())
            else -> machine.fail(NetherNetError.Protocol("connecting", "unknown transport state $code"))
        }
    }

    /**
     * The transport sub-path of the connection graph, in order.
     *
     * The native transport reports its own progress, which can outrun the auth chain and can report
     * a state the machine is already in. [advanceTo] walks any missing links in this chain rather
     * than throwing, because the two run independently; a state that is neither in the chain nor a
     * direct legal edge is logged and skipped instead of failing a session that is working.
     */
    private val transportChain = listOf(
        ConnectionState.SignalingNetherNet,
        ConnectionState.NegotiatingWebRtc,
        ConnectionState.Joining,
    )

    private suspend fun advanceTo(target: ConnectionState, cause: String) {
        val current = machine.state.value
        if (current.name == target.name) return
        val fromIndex = transportChain.indexOfFirst { it.name == current.name }
        val toIndex = transportChain.indexOfFirst { it.name == target.name }
        if (fromIndex >= 0 && toIndex > fromIndex) {
            for (i in fromIndex + 1..toIndex) {
                machine.transition(transportChain[i], cause)
            }
            return
        }
        if (machine.allowedEdges[current.name]?.contains(target.name) == true) {
            machine.transition(target, cause)
            return
        }
        AppLog.warn(TAG, "transport reported ${target.name} while in ${current.name}; skipping")
    }

    private fun nativeError(): NetherNetError {
        val tag = runCatching { native.errorTag() }.getOrNull()
        val message = runCatching { native.errorMessage() }.getOrNull()
        val retryable = runCatching { native.errorRetryable() }.getOrDefault(false)
        return NetherNetError.fromNativeTag(tag, message, retryable)
    }

    /** The state codes the native side publishes. Mirrors `TransportState::code` in Rust. */
    object TransportStateCode {
        const val IDLE = 0
        const val SIGNALING = 1
        const val AWAITING_WELCOME = 2
        const val CONNECTING = 3
        const val CONNECTED = 4
        const val CLOSED = 5
        const val FAILED = 6
    }

    private companion object {
        const val TAG = "NetherNetTransport"
    }
}

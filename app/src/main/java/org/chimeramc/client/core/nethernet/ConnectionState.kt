package org.chimeramc.client.core.nethernet

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The states a Bedrock join moves through, from the moment Play is pressed to the moment a data
 * channel is open.
 *
 * The launcher used to have no state at all: the Play path ran one suspending auth call and the UI
 * showed a fixed "Microsoft connecting" banner until it returned. When 26.60 started routing Xbox
 * Live traffic through NetherNet, a stall in that call left the banner up forever with no way out.
 * Making the states explicit is what lets each step carry its own timeout and its own cancel path.
 *
 * [Connected] carries flags rather than being three states: the UI has one "you may press Play"
 * condition, and the flags say which preconditions are met. [Failed] carries a typed [NetherNetError]
 * so the UI never has to read a raw code.
 */
sealed class ConnectionState {

    /** Nothing in flight. The default state, and where [ConnectionStateMachine.cancel] returns to. */
    object Idle : ConnectionState()

    /** The MSA request is being prepared and sent. */
    object AuthStarting : ConnectionState()

    /** Waiting for the user to approve the sign-in in a browser (device-code or Custom Tab). */
    object AuthWaitingForUser : ConnectionState()

    /** Exchanging the code/device-code for tokens. */
    object AuthExchanging : ConnectionState()

    /** Calling Xbox Live user authentication. */
    object SigningInXboxLive : ConnectionState()

    /** Calling XSTS. */
    object AuthorizingXsts : ConnectionState()

    /** Calling the Minecraft services token endpoint. */
    object FetchingMinecraftToken : ConnectionState()

    /** Resolving a target session id to a NetherNet host (signaling lookup). */
    object ResolvingServer : ConnectionState()

    /** Connected to the signaling WebSocket and authenticating the session. */
    object SignalingNetherNet : ConnectionState()

    /** Exchanging SDP/ICE and waiting for the data channel to open. */
    object NegotiatingWebRtc : ConnectionState()

    /** Data channel open; joining the game session. */
    object Joining : ConnectionState()

    /**
     * A terminal success.
     *
     * [authOnly] means the auth chain finished and the Play button may be enabled but no transport
     * is open yet. [offline] means the auth chain was skipped because a cached token and a LAN
     * target were enough. [transport] is what actually carried the session.
     */
    data class Connected(
        val authOnly: Boolean = false,
        val offline: Boolean = false,
        val transport: Transport = Transport.NONE,
    ) : ConnectionState()

    /** A terminal failure. [error] is the user-facing taxonomy, never a raw code. */
    data class Failed(val error: NetherNetError) : ConnectionState()

    /** The user cancelled. Terminal, and distinct from [Failed] so the UI can stay quiet about it. */
    object Cancelled : ConnectionState()

    /** The state's stable name, used by the diagnostics buffer and the version-matrix tests. */
    val name: String
        get() = when (this) {
            is Idle -> "Idle"
            is AuthStarting -> "AuthStarting"
            is AuthWaitingForUser -> "AuthWaitingForUser"
            is AuthExchanging -> "AuthExchanging"
            is SigningInXboxLive -> "SigningInXboxLive"
            is AuthorizingXsts -> "AuthorizingXsts"
            is FetchingMinecraftToken -> "FetchingMinecraftToken"
            is ResolvingServer -> "ResolvingServer"
            is SignalingNetherNet -> "SignalingNetherNet"
            is NegotiatingWebRtc -> "NegotiatingWebRtc"
            is Joining -> "Joining"
            is Connected -> "Connected"
            is Failed -> "Failed"
            is Cancelled -> "Cancelled"
        }

    /** True while a step is in flight; the UI shows the elapsed timer and the Cancel button then. */
    val isBusy: Boolean
        get() = this !is Idle && this !is Connected && this !is Failed && this !is Cancelled

    /** True for the three terminal states, where a fresh [ConnectionStateMachine.begin] is allowed. */
    val isTerminal: Boolean
        get() = this is Connected || this is Failed || this is Cancelled
}

/** Which transport carried, or will carry, the session. Shown as the UI's debug badge. */
enum class Transport { NONE, NETHERNET, RAKNET }

/** One state change, as the diagnostics buffer records it. */
data class Transition(
    val from: String,
    val to: String,
    val cause: String,
    /** Monotonic milliseconds since machine construction; never wall-clock. */
    val atMs: Long,
    val durationMs: Long,
)

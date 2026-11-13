package org.chimeramc.client.ui.compose.connect

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import org.chimeramc.client.core.nethernet.ConnectionState
import org.chimeramc.client.core.nethernet.ConnectionStateMachine
import org.chimeramc.client.ui.compose.theme.GlowTheme

/**
 * The Java-facing entry point for the connection status block.
 *
 * A `@Composable` function cannot be called from Java — the compiler threads a `Composer` parameter
 * whose type Java cannot name. This host is the seam: the activity keeps the state and hands over two
 * plain callbacks, and everything Compose-shaped stays on the Kotlin side.
 */
object ConnectionStatusHost {

    /** The mutable state the activity drives; a Kotlin [MutableState] so a set recomposes. */
    @JvmStatic
    fun state(): MutableState<ConnectionStatusState> = mutableStateOf(
        ConnectionStatusState(
            visible = false,
            stepLabel = "",
            elapsedSeconds = 0,
            failed = false,
            retryable = false,
            cancelled = false,
        ),
    )

    /**
     * Derives the render state from a machine snapshot.
     *
     * Pure, so the activity can call it from whatever observes the machine and a test can call it
     * directly. It is the one place the machine's states become UI copy, so a new state cannot ship
     * without a label.
     */
    @JvmStatic
    fun from(state: ConnectionState, elapsedSeconds: Long): ConnectionStatusState = when (state) {
        is ConnectionState.Idle -> ConnectionStatusState(
            visible = false,
            stepLabel = "",
            elapsedSeconds = 0,
            failed = false,
            retryable = false,
            cancelled = false,
        )
        is ConnectionState.Connected -> ConnectionStatusState(
            visible = false,
            stepLabel = "Connected",
            elapsedSeconds = elapsedSeconds,
            failed = false,
            retryable = false,
            cancelled = false,
        )
        is ConnectionState.Cancelled -> ConnectionStatusState(
            visible = true,
            stepLabel = "Cancelled",
            elapsedSeconds = elapsedSeconds,
            failed = false,
            retryable = false,
            cancelled = true,
        )
        is ConnectionState.Failed -> ConnectionStatusState(
            visible = true,
            stepLabel = state.error.step.replaceFirstChar { it.uppercase() },
            elapsedSeconds = elapsedSeconds,
            failed = true,
            retryable = state.error.retryable,
            cancelled = false,
            reason = state.error.reason,
        )
        else -> ConnectionStatusState(
            visible = true,
            stepLabel = stepLabel(state),
            elapsedSeconds = elapsedSeconds,
            failed = false,
            retryable = false,
            cancelled = false,
        )
    }

    /** The plain-language label for a busy state. */
    @JvmStatic
    fun stepLabel(state: ConnectionState): String = when (state) {
        is ConnectionState.AuthStarting -> "Starting sign-in"
        is ConnectionState.AuthWaitingForUser -> "Waiting for you to sign in"
        is ConnectionState.AuthExchanging -> "Finishing sign-in"
        is ConnectionState.SigningInXboxLive -> "Signing in to Xbox Live"
        is ConnectionState.AuthorizingXsts -> "Authorizing with Xbox Live"
        is ConnectionState.FetchingMinecraftToken -> "Checking Minecraft"
        is ConnectionState.ResolvingServer -> "Finding the world"
        is ConnectionState.SignalingNetherNet -> "Connecting to the session"
        is ConnectionState.NegotiatingWebRtc -> "Negotiating a direct connection"
        is ConnectionState.Joining -> "Joining the world"
        is ConnectionState.Connected -> "Connected"
        is ConnectionState.Failed -> state.error.step.replaceFirstChar { it.uppercase() }
        is ConnectionState.Cancelled -> "Cancelled"
        is ConnectionState.Idle -> ""
    }

    /** Mounts the status block into [composeView], wrapping it in the Caramel theme. */
    @JvmStatic
    fun setContent(
        composeView: ComposeView,
        state: MutableState<ConnectionStatusState>,
        onCancel: Runnable,
        onRetry: Runnable,
    ) {
        composeView.setContent {
            GlowTheme {
                ConnectionStatus(
                    state = state.value,
                    onCancel = { onCancel.run() },
                    onRetry = { onRetry.run() },
                )
            }
        }
    }

    /**
     * True when [state] should show the block at all.
     *
     * The activity uses this to decide whether to bother inflating the Compose view, so a fresh
     * launch does not pay for it.
     */
    @JvmStatic
    fun shouldShow(machine: ConnectionStateMachine): Boolean =
        from(machine.state.value, machine.elapsedSeconds.value).visible
}

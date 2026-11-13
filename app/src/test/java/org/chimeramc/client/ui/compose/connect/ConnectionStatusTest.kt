package org.chimeramc.client.ui.compose.connect

import org.chimeramc.client.core.nethernet.ConnectionState
import org.chimeramc.client.core.nethernet.NetherNetError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The connection status copy and affordances.
 *
 * This is the UI half of the original bug: the launcher hung with nothing to show and nothing to
 * press. So the rules pinned here are "there is always a label", "a busy step always offers Cancel"
 * and "Retry only appears when retrying could work" — the last one because a Retry button that cannot
 * succeed is worse than no button.
 */
class ConnectionStatusTest {

    private fun busy(elapsed: Long = 3) = ConnectionStatusState(
        visible = true,
        stepLabel = "Signing in to Xbox Live",
        elapsedSeconds = elapsed,
        failed = false,
        retryable = false,
        cancelled = false,
    )

    @Test
    fun `a busy step shows its label, an elapsed timer, a spinner and Cancel`() {
        val copy = busy().copy()
        assertEquals("Signing in to Xbox Live", copy.title)
        assertEquals("— 3s", copy.detail)
        assertTrue(copy.showSpinner)
        assertTrue(copy.showCancel)
        assertFalse("a busy step must not offer Retry", copy.showRetry)
    }

    @Test
    fun `an invisible state renders nothing`() {
        val copy = ConnectionStatusState(
            visible = false,
            stepLabel = "",
            elapsedSeconds = 0,
            failed = false,
            retryable = false,
            cancelled = false,
        ).copy()
        assertFalse(copy.visible)
        assertFalse(copy.showCancel)
        assertFalse(copy.showRetry)
        assertFalse(copy.showSpinner)
    }

    @Test
    fun `a retryable failure offers Retry and no spinner`() {
        val copy = ConnectionStatusState(
            visible = true,
            stepLabel = "Connecting",
            elapsedSeconds = 12,
            failed = true,
            retryable = true,
            cancelled = false,
            reason = "Could not reach the network.",
        ).copy()
        assertTrue(copy.showRetry)
        assertFalse(copy.showSpinner)
        assertFalse("a failed step is not cancellable", copy.showCancel)
        assertEquals("Could not reach the network.", copy.detail)
    }

    @Test
    fun `a non-retryable failure offers no buttons at all`() {
        val copy = ConnectionStatusState(
            visible = true,
            stepLabel = "Checking Minecraft",
            elapsedSeconds = 4,
            failed = true,
            retryable = false,
            cancelled = false,
            reason = "This account does not own Minecraft.",
        ).copy()
        assertFalse("Retry must not be offered when it cannot work", copy.showRetry)
        assertFalse(copy.showCancel)
        assertEquals("This account does not own Minecraft.", copy.detail)
    }

    @Test
    fun `a failure with no reason still says something`() {
        val copy = ConnectionStatusState(
            visible = true,
            stepLabel = "Connecting",
            elapsedSeconds = 1,
            failed = true,
            retryable = true,
            cancelled = false,
            reason = "",
        ).copy()
        assertTrue(copy.detail.isNotEmpty())
    }

    @Test
    fun `a cancelled join offers Retry and is not styled as a failure`() {
        val copy = ConnectionStatusState(
            visible = true,
            stepLabel = "Cancelled",
            elapsedSeconds = 2,
            failed = false,
            retryable = false,
            cancelled = true,
        ).copy()
        assertEquals("Cancelled", copy.title)
        assertTrue(copy.showRetry)
        assertFalse(copy.showCancel)
        assertFalse(copy.showSpinner)
    }

    @Test
    fun `every busy state has a label and a Cancel affordance`() {
        val busyStates = listOf(
            ConnectionState.AuthStarting,
            ConnectionState.AuthWaitingForUser,
            ConnectionState.AuthExchanging,
            ConnectionState.SigningInXboxLive,
            ConnectionState.AuthorizingXsts,
            ConnectionState.FetchingMinecraftToken,
            ConnectionState.ResolvingServer,
            ConnectionState.SignalingNetherNet,
            ConnectionState.NegotiatingWebRtc,
            ConnectionState.Joining,
        )
        for (state in busyStates) {
            val label = ConnectionStatusHost.stepLabel(state)
            assertTrue("$state has no label", label.isNotEmpty())
            val render = ConnectionStatusHost.from(state, 5)
            assertTrue("$state must be visible", render.visible)
            assertFalse("$state is not a failure", render.failed)
            assertTrue("$state must offer Cancel", render.copy().showCancel)
        }
    }

    @Test
    fun `every state maps to a render state without throwing`() {
        val states = listOf(
            ConnectionState.Idle,
            ConnectionState.AuthStarting,
            ConnectionState.AuthWaitingForUser,
            ConnectionState.AuthExchanging,
            ConnectionState.SigningInXboxLive,
            ConnectionState.AuthorizingXsts,
            ConnectionState.FetchingMinecraftToken,
            ConnectionState.ResolvingServer,
            ConnectionState.SignalingNetherNet,
            ConnectionState.NegotiatingWebRtc,
            ConnectionState.Joining,
            ConnectionState.Connected(authOnly = true),
            ConnectionState.Failed(NetherNetError.Timeout("connecting")),
            ConnectionState.Cancelled,
        )
        for (state in states) {
            val render = ConnectionStatusHost.from(state, 1)
            if (state is ConnectionState.Idle || state is ConnectionState.Connected) {
                assertFalse("$state must not show the block", render.visible)
            } else {
                assertTrue("$state must show the block", render.visible)
                assertTrue("$state must have a title", render.copy().title.isNotEmpty())
            }
        }
    }

    @Test
    fun `a failure carries the taxonomy reason and its retryability`() {
        val render = ConnectionStatusHost.from(
            ConnectionState.Failed(
                NetherNetError.Auth(
                    "checking Minecraft",
                    "This account does not own Minecraft.",
                    "Sign in with another account.",
                ),
            ),
            7,
        )
        assertTrue(render.failed)
        assertFalse(render.retryable)
        assertTrue(render.reason.contains("does not own Minecraft"))
        assertFalse("a non-retryable failure must not offer Retry", render.copy().showRetry)
    }

    @Test
    fun `the copy never leaks a raw code or a stack word`() {
        val errors = listOf(
            NetherNetError.Timeout("connecting"),
            NetherNetError.Network("connecting", "unreachable"),
            NetherNetError.Protocol("connecting", "bad frame"),
            NetherNetError.Signaling("connecting", "busy"),
            NetherNetError.Unknown("connecting", "IOException"),
            NetherNetError.TransportUnavailable(),
        )
        for (error in errors) {
            val render = ConnectionStatusHost.from(ConnectionState.Failed(error), 1)
            val text = render.copy().title + " " + render.copy().detail
            for (word in listOf("null", "panic", "Exception", "SIGSEGV", "0x")) {
                assertFalse("copy contains $word: $text", text.contains(word))
            }
        }
    }
}

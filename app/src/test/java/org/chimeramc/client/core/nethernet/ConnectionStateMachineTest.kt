package org.chimeramc.client.core.nethernet

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The state machine's two guarantees, pinned without a device or a network: no state can outlive its
 * timeout, and the shape of the graph cannot silently change.
 *
 * Virtual time is what makes this cheap. The 30-second Joining timeout is asserted in microseconds,
 * so a regression that removes the watchdog fails here rather than after a minute of real waiting.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionStateMachineTest {

    private var fakeClockMs = 0L

    private fun machine(scope: CoroutineScope) = ConnectionStateMachine(scope) { fakeClockMs }

    /** Walks the legal auth edges up to Xbox Live, so a test can start a step from a real state. */
    private suspend fun ConnectionStateMachine.reachSigningInXboxLive() {
        transition(ConnectionState.AuthStarting, "play")
        transition(ConnectionState.AuthExchanging, "code exchanged")
        transition(ConnectionState.SigningInXboxLive, "ms token")
    }

    @Test
    fun `a blocking state times out and fails with its own step`() = runTest {
        val m = machine(backgroundScope)
        m.reachSigningInXboxLive()

        advanceTimeBy(15_000L)
        runCurrent()

        val state = m.state.value
        assertTrue("the Xbox Live step must time out, was $state", state is ConnectionState.Failed)
        val error = (state as ConnectionState.Failed).error
        assertTrue("the timeout must name the step, was $error", error is NetherNetError.Timeout)
        assertEquals("signing in to Xbox Live", error.step)
    }

    @Test
    fun `a fast step does not time out`() = runTest {
        val m = machine(backgroundScope)
        m.transition(ConnectionState.AuthStarting, "play")
        advanceTimeBy(5_000L)
        runCurrent()
        assertTrue(m.state.value is ConnectionState.AuthStarting)
    }

    @Test
    fun `cancel resets to Idle from a blocked state`() = runTest {
        val m = machine(backgroundScope)
        m.transition(ConnectionState.AuthStarting, "play")
        m.transition(ConnectionState.AuthWaitingForUser, "device code shown")
        m.cancel()
        assertEquals(ConnectionState.Idle, m.state.value)
    }

    @Test
    fun `cancel returns to Idle even after the watchdog already failed`() = runTest {
        val m = machine(backgroundScope)
        m.reachSigningInXboxLive()
        advanceTimeBy(20_000L)
        runCurrent()
        assertTrue(m.state.value is ConnectionState.Failed)
        m.cancel()
        assertEquals(ConnectionState.Idle, m.state.value)
    }

    @Test
    fun `Idle cannot jump straight to Connected`() = runTest {
        val m = machine(backgroundScope)
        assertFalse(m.isAllowed("Idle", "Connected"))
        var threw = false
        try {
            m.transition(ConnectionState.Connected(authOnly = true), "cheat")
        } catch (expected: ConnectionStateMachine.IllegalTransition) {
            threw = true
        }
        assertTrue("skipping the auth chain must be refused", threw)
        assertEquals(ConnectionState.Idle, m.state.value)
    }

    @Test
    fun `the full happy path walks every edge`() = runTest {
        val m = machine(backgroundScope)
        val path = listOf(
            ConnectionState.AuthStarting to "play",
            ConnectionState.AuthWaitingForUser to "device code",
            ConnectionState.AuthExchanging to "approved",
            ConnectionState.SigningInXboxLive to "ms token",
            ConnectionState.AuthorizingXsts to "xbl ok",
            ConnectionState.FetchingMinecraftToken to "xsts ok",
            ConnectionState.ResolvingServer to "mc token",
            ConnectionState.SignalingNetherNet to "session found",
            ConnectionState.NegotiatingWebRtc to "signaling ok",
            ConnectionState.Joining to "ice ok",
            ConnectionState.Connected(transport = Transport.NETHERNET) to "joined",
        )
        for ((state, cause) in path) m.transition(state, cause)
        assertTrue(m.state.value is ConnectionState.Connected)
        assertEquals(Transport.NETHERNET, (m.state.value as ConnectionState.Connected).transport)
    }

    @Test
    fun `every busy state has a timeout and every timeout names a real state`() = runTest {
        val m = machine(backgroundScope)
        for ((state, _) in m.timeoutsMs) {
            assertTrue("timeout configured for unknown state $state", m.allowedEdges.containsKey(state))
        }
        assertTrue("AuthWaitingForUser must allow a long user wait",
            m.timeoutsMs.getValue("AuthWaitingForUser") >= 120_000L)
        assertTrue("Joining must have a bound", m.timeoutsMs.getValue("Joining") > 0L)
    }

    @Test
    fun `every non-terminal state can reach Failed or Cancelled`() = runTest {
        val m = machine(backgroundScope)
        val terminal = setOf("Connected", "Failed", "Cancelled")
        for ((from, edges) in m.allowedEdges) {
            if (from in terminal) continue
            assertTrue("$from must be escapable", edges.contains("Failed") || edges.contains("Cancelled"))
        }
    }

    @Test
    fun `the elapsed ticker reports seconds since the state was entered`() = runTest {
        val m = machine(backgroundScope)
        m.reachSigningInXboxLive()
        fakeClockMs = 3_500L
        advanceTimeBy(1_500L)
        runCurrent()
        assertEquals(3L, m.elapsedSeconds.value)
    }

    @Test
    fun `a transition records its cause and duration`() = runTest {
        val m = machine(backgroundScope)
        val seen = mutableListOf<Transition>()
        val collector = backgroundScope.launch {
            m.transitions.collect { seen.add(it) }
        }
        runCurrent()
        m.transition(ConnectionState.AuthStarting, "user pressed play")
        runCurrent()
        collector.cancel()
        assertEquals(1, seen.size)
        assertEquals("Idle", seen[0].from)
        assertEquals("AuthStarting", seen[0].to)
        assertEquals("user pressed play", seen[0].cause)
    }
}

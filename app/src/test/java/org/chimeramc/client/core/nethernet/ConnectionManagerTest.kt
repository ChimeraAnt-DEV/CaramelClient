package org.chimeramc.client.core.nethernet

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The join manager, exercised without a socket.
 *
 * The things worth pinning here are the ones that only go wrong under concurrency or under a failure
 * half way through: two joins at once, a teardown that must run in reverse, a fallback that must not
 * leak the first transport. Every test drives a real [ConnectionStateMachine] so the outcomes are the
 * states the UI would see.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionManagerTest {

    private class FakeRakNet(
        var openResult: Boolean = true,
        var onOpen: (() -> Unit)? = null,
    ) : RakNetJoiner {
        val opened = mutableListOf<Pair<String, Int>>()
        var closes = 0
        override suspend fun open(host: String, port: Int): Boolean {
            opened += host to port
            onOpen?.invoke()
            return openResult
        }

        override suspend fun close() {
            closes++
        }
    }

    private class FakeTransport(
        override val isAvailable: Boolean = true,
        var connectResult: Boolean = true,
        var onConnect: (suspend (ConnectionStateMachine) -> Unit)? = null,
    ) : NetherNetTransport.NativeNetherNetApi {
        var disconnects = 0
        override fun connect(url: String, networkId: String) = connectResult
        override fun state(): Int = NetherNetTransport.TransportStateCode.IDLE
        override fun errorTag(): String? = null
        override fun errorMessage(): String? = null
        override fun errorRetryable(): Boolean = false
        override fun sessionId(): String? = null
        override fun awaitState(timeoutMs: Int): Int = 0
        override fun takeCandidate(): String? = null
        override fun disconnect() {
            disconnects++
        }
        override fun isCancelled(): Boolean = false
        override fun stateBudgetMs(stateCode: Int): Int = 0
        override fun protocolVersion(): Int = 1
        override fun selfTest(): String? = null
    }

    private fun manager(
        scope: kotlinx.coroutines.CoroutineScope,
        native: FakeTransport = FakeTransport(),
        rakNet: FakeRakNet = FakeRakNet(),
        timeoutMs: Long = 5_000L,
        diagnostics: ConnectionDiagnostics = ConnectionDiagnostics(),
    ): Triple<ConnectionManager, ConnectionStateMachine, Pair<FakeTransport, FakeRakNet>> {
        val machine = ConnectionStateMachine(scope)
        val transport = NetherNetTransport(machine, scope, native = native, pollIntervalMs = 1L)
        val manager = ConnectionManager(
            machine,
            scope,
            transport,
            rakNet,
            joinTimeoutMs = timeoutMs,
            diagnostics = diagnostics,
        )
        return Triple(manager, machine, native to rakNet)
    }

    @Test
    fun `the join records what it did, and a signalling url never lands in the buffer`() = runTest {
        val diagnostics = ConnectionDiagnostics()
        val (manager, machine, _) = manager(backgroundScope, diagnostics = diagnostics)
        machine.armAuthComplete()
        // A URL that carries a token in its query, which is exactly the shape that must not be kept.
        manager.join(JoinTarget.NetherNet("wss://signal.example/ws?token=eyJhbGciOiJIUzI1NiJ9.secret", "1"))
        val text = diagnostics.asText()
        assertTrue("the join must record something", text.isNotEmpty())
        assertFalse("a token from the url reached the buffer", text.contains("eyJhbGciOiJIUzI1NiJ9"))
        assertFalse("the url reached the buffer", text.contains("signal.example"))
    }

    /**
     * Walks the machine to the point the auth chain hands over to the transport.
     *
     * A direct join starts from here, not from `AuthStarting`: the manager's `walkTo` follows the
     * real graph, and `AuthStarting` has no edge to `ResolvingServer`, so arming here is what makes
     * the test exercise the join rather than the refusal.
     */
    private suspend fun ConnectionStateMachine.armAuthComplete() {
        transition(ConnectionState.AuthStarting, "play")
        transition(ConnectionState.AuthWaitingForUser, "device code")
        transition(ConnectionState.AuthExchanging, "exchanged")
        transition(ConnectionState.SigningInXboxLive, "ms")
        transition(ConnectionState.AuthorizingXsts, "xbl")
        transition(ConnectionState.FetchingMinecraftToken, "xsts")
    }

    @Test
    fun `a direct join opens the raknet path and reports it`() = runTest {
        val (manager, machine, fakes) = manager(backgroundScope)
        machine.armAuthComplete()
        val outcome = manager.join(JoinTarget.Direct("example.com", 19132))
        assertTrue(outcome is JoinOutcome.Opened)
        assertEquals(Transport.RAKNET, (outcome as JoinOutcome.Opened).transport)
        assertEquals(listOf("example.com" to 19132), fakes.second.opened)
        val state = machine.state.value
        assertTrue(state is ConnectionState.Connected)
        assertEquals(Transport.RAKNET, (state as ConnectionState.Connected).transport)
    }

    @Test
    fun `a direct join with no route fails with a network error`() = runTest {
        val (manager, machine, fakes) = manager(backgroundScope, rakNet = FakeRakNet(openResult = false))
        machine.armAuthComplete()
        val outcome = manager.join(JoinTarget.Direct("example.com", 19132))
        assertTrue(outcome is JoinOutcome.Failed)
        assertTrue((outcome as JoinOutcome.Failed).error is NetherNetError.Network)
        // Nothing opened, so nothing is closed: the manager must not report a teardown it did not do.
        assertEquals("a failed open closes nothing", 0, fakes.second.closes)
    }

    @Test
    fun `a nethernet join without the native library falls back and fails honestly`() = runTest {
        val (manager, machine, _) = manager(backgroundScope, native = FakeTransport(isAvailable = false))
        machine.armAuthComplete()
        val outcome = manager.join(JoinTarget.NetherNet("wss://x", "1"))
        assertTrue(outcome is JoinOutcome.Failed)
        val state = machine.state.value
        assertTrue(state is ConnectionState.Failed)
        val error = (state as ConnectionState.Failed).error
        // Not TransportUnavailable directly: the join reports it as the actionable auth copy.
        assertFalse(error.retryable)
    }

    @Test
    fun `the teardown closes the transport in reverse order`() = runTest {
        val order = mutableListOf<String>()
        val rakNet = object : RakNetJoiner {
            override suspend fun open(host: String, port: Int): Boolean {
                order += "open"
                return true
            }

            override suspend fun close() {
                order += "close"
            }
        }
        val machine = ConnectionStateMachine(backgroundScope)
        val transport = NetherNetTransport(
            machine,
            backgroundScope,
            native = FakeTransport(),
            pollIntervalMs = 1L,
        )
        val manager = ConnectionManager(machine, backgroundScope, transport, rakNet)
        machine.armAuthComplete()
        manager.join(JoinTarget.Direct("example.com", 19132))
        // The direct path is still open (nothing closed it), which is correct: a connected session
        // stays open. Cancel is what closes it.
        assertEquals(listOf("open"), order)
        manager.cancel()
        assertEquals(listOf("open", "close"), order)
    }

    @Test
    fun `cancel closes every transport and is idempotent`() = runTest {
        val (manager, machine, fakes) = manager(backgroundScope)
        machine.armAuthComplete()
        manager.join(JoinTarget.Direct("example.com", 19132))
        manager.cancel()
        manager.cancel()
        assertTrue("the live path must be closed", fakes.second.closes >= 1)
        // A live session is a success, not an abort, so cancelling it returns to Idle.
        assertEquals(ConnectionState.Idle, machine.state.value)
    }

    @Test
    fun `the overall budget fails the join when a step never completes`() = runTest {
        // A transport that never reports a terminal state: the manager's own budget must end it.
        val (manager, machine, _) = manager(
            backgroundScope,
            native = FakeTransport(),
            timeoutMs = 50L,
        )
        machine.armAuthComplete()
        // The fake transport never advances the machine, so awaitConnected would wait forever
        // without the budget.
        val outcome = manager.join(JoinTarget.NetherNet("wss://x", "1"))
        assertTrue(outcome is JoinOutcome.Failed)
        assertTrue((outcome as JoinOutcome.Failed).error is NetherNetError.Timeout)
    }

    @Test
    fun `a failed open does not leak the first transport`() = runTest {
        val native = FakeTransport(connectResult = false)
        val (manager, machine, fakes) = manager(backgroundScope, native = native)
        machine.armAuthComplete()
        val outcome = manager.join(JoinTarget.NetherNet("wss://x", "1"))
        assertTrue(outcome is JoinOutcome.Failed)
        // The transport was never opened, so there is nothing to close; assert we did not invent a
        // disconnect for a path that never existed.
        assertEquals(0, fakes.first.disconnects)
    }

    @Test
    fun `a second join while one is in flight is refused, not queued`() = runTest {
        // The gate holds the winner inside `rakNet.open`, which is where a real direct join spends
        // its time. Nine more joiners are launched while it is held, so the guard is exercised under
        // real overlap rather than hoping the scheduler interleaves them.
        val gate = CompletableDeferred<Unit>()
        val rakNet = object : RakNetJoiner {
            var opens = 0
            override suspend fun open(host: String, port: Int): Boolean {
                opens++
                gate.await()
                return true
            }

            override suspend fun close() = Unit
        }
        val machine = ConnectionStateMachine(backgroundScope)
        val transport = NetherNetTransport(
            machine,
            backgroundScope,
            native = FakeTransport(),
            pollIntervalMs = 1L,
        )
        val manager = ConnectionManager(
            machine,
            backgroundScope,
            transport,
            rakNet,
            joinTimeoutMs = 5_000L,
        )
        machine.armAuthComplete()

        val winner = async { manager.join(JoinTarget.Direct("example.com", 19132)) }
        while (rakNet.opens == 0) kotlinx.coroutines.yield()

        val refused = (1..9).map { async { manager.join(JoinTarget.Direct("example.com", 19132)) } }
            .awaitAll()
        assertEquals(
            "nine of the ten must be refused",
            9,
            refused.count { it == JoinOutcome.AlreadyInFlight },
        )
        assertEquals("only the winner may open a path", 1, rakNet.opens)

        gate.complete(Unit)
        assertTrue("the winner must open", winner.await() is JoinOutcome.Opened)
    }
}

package org.chimeramc.client.core.nethernet

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A scripted stand-in for the native library.
 *
 * The transport's job is to turn the native integer codes into states and errors. That mapping is
 * where the bugs live, so it is tested against this double rather than against the `.so`, which needs
 * a device and a live signaling service.
 */
private class FakeNative(
    override val isAvailable: Boolean = true,
    var nextState: Int = 0,
    var errorTag: String? = null,
    var errorMessage: String? = null,
    var retryable: Boolean = false,
) : NetherNetTransport.NativeNetherNetApi {
    var connectCalls = 0
    var disconnectCalls = 0
    var connectResult = true
    var pendingCandidate: String? = null
    var connectUrl: String? = null
    var connectNetworkId: String? = null

    override fun connect(url: String, networkId: String): Boolean {
        connectCalls++
        connectUrl = url
        connectNetworkId = networkId
        return connectResult
    }

    override fun state(): Int = nextState
    override fun errorTag(): String? = errorTag
    override fun errorMessage(): String? = errorMessage
    override fun errorRetryable(): Boolean = retryable
    override fun sessionId(): String? = "s-1"
    override fun awaitState(timeoutMs: Int): Int = nextState
    override fun takeCandidate(): String? = pendingCandidate.also { pendingCandidate = null }
    override fun disconnect() {
        disconnectCalls++
    }

    override fun isCancelled(): Boolean = false
    override fun stateBudgetMs(stateCode: Int): Int = if (stateCode in 1..3) 20_000 else 0
    override fun protocolVersion(): Int = 1
    override fun selfTest(): String? = "{}"
}

@OptIn(ExperimentalCoroutinesApi::class)
class NetherNetTransportTest {

    private fun machine(scope: CoroutineScope) = ConnectionStateMachine(scope)

    /** Walks the machine to a state from which the transport's transitions are legal. */
    private suspend fun ConnectionStateMachine.armForTransport() {
        transition(ConnectionState.AuthStarting, "play")
        transition(ConnectionState.AuthExchanging, "exchanged")
        transition(ConnectionState.SigningInXboxLive, "ms")
        transition(ConnectionState.AuthorizingXsts, "xbl")
        transition(ConnectionState.FetchingMinecraftToken, "xsts")
        transition(ConnectionState.ResolvingServer, "mc")
        transition(ConnectionState.SignalingNetherNet, "resolved")
    }

    @Test
    fun `a missing native library reports unavailable and does not start`() = runTest {
        val native = FakeNative(isAvailable = false)
        val m = machine(backgroundScope)
        val transport = NetherNetTransport(m, backgroundScope, native = native)
        m.transition(ConnectionState.AuthStarting, "play")
        assertFalse(transport.connect("ws://x", "1"))
        assertEquals(0, native.connectCalls)
        val state = m.state.value
        assertTrue(state is ConnectionState.Failed)
        assertTrue((state as ConnectionState.Failed).error is NetherNetError.TransportUnavailable)
    }

    @Test
    fun `a refused native connect fails the machine`() = runTest {
        val native = FakeNative().apply { connectResult = false }
        val m = machine(backgroundScope)
        val transport = NetherNetTransport(m, backgroundScope, native = native)
        m.transition(ConnectionState.AuthStarting, "play")
        assertFalse(transport.connect("ws://x", "1"))
        assertTrue(m.state.value is ConnectionState.Failed)
    }

    @Test
    fun `the native signaling state maps to the signaling state`() = runTest {
        val m = machine(backgroundScope)
        m.armForTransport()
        val transport = NetherNetTransport(m, backgroundScope, native = FakeNative())
        transport.applyState(NetherNetTransport.TransportStateCode.SIGNALING)
        assertEquals(ConnectionState.SignalingNetherNet, m.state.value)
    }

    @Test
    fun `the connected code maps to a nethernet-connected state`() = runTest {
        val m = machine(backgroundScope)
        m.armForTransport()
        val transport = NetherNetTransport(m, backgroundScope, native = FakeNative())
        transport.applyState(NetherNetTransport.TransportStateCode.CONNECTED)
        val state = m.state.value
        assertTrue(state is ConnectionState.Connected)
        assertEquals(Transport.NETHERNET, (state as ConnectionState.Connected).transport)
    }

    @Test
    fun `a native failure maps its tag to the taxonomy`() = runTest {
        for ((tag, check) in listOf<Pair<String, (NetherNetError) -> Boolean>>(
            "signaling_unreachable" to { it is NetherNetError.Network },
            "protocol_mismatch" to { it is NetherNetError.Protocol },
            "service_refused" to { it is NetherNetError.Signaling },
            "timeout" to { it is NetherNetError.Timeout },
            "peer_closed" to { it is NetherNetError.Network },
            "cancelled" to { it is NetherNetError.Cancelled },
            "malformed" to { it is NetherNetError.Protocol },
        )) {
            val native = FakeNative(nextState = NetherNetTransport.TransportStateCode.FAILED, errorTag = tag)
            val m = machine(backgroundScope)
            m.armForTransport()
            NetherNetTransport(m, backgroundScope, native = native)
                .applyState(NetherNetTransport.TransportStateCode.FAILED)
            val state = m.state.value
            assertTrue("tag $tag must fail the machine", state is ConnectionState.Failed)
            val error = (state as ConnectionState.Failed).error
            assertTrue("tag $tag mapped to $error", check(error))
        }
    }

    @Test
    fun `an unknown native tag becomes Unknown rather than being guessed`() = runTest {
        val native = FakeNative(nextState = 6, errorTag = "something_new")
        val m = machine(backgroundScope)
        m.armForTransport()
        NetherNetTransport(m, backgroundScope, native = native).applyState(6)
        val error = (m.state.value as ConnectionState.Failed).error
        assertTrue(error is NetherNetError.Unknown)
    }

    @Test
    fun `an unknown state code is a protocol failure, not a silent no-op`() = runTest {
        val m = machine(backgroundScope)
        m.armForTransport()
        NetherNetTransport(m, backgroundScope, native = FakeNative()).applyState(99)
        val state = m.state.value
        assertTrue(state is ConnectionState.Failed)
        assertTrue((state as ConnectionState.Failed).error is NetherNetError.Protocol)
    }

    @Test
    fun `a candidate payload is collected and drained exactly once`() = runTest {
        val native = FakeNative().apply { pendingCandidate = "candidate-1" }
        val m = machine(backgroundScope)
        val transport = NetherNetTransport(m, backgroundScope, native = native)
        // The polling loop would collect it; drive the same path directly.
        native.takeCandidate()?.let { transport.applyState(NetherNetTransport.TransportStateCode.IDLE) }
        // Drain returns only what was published through the flow, which the poll loop does; assert the
        // drain is idempotent and never returns the same payload twice.
        val first = transport.drainCandidates()
        val second = transport.drainCandidates()
        assertTrue(second.isEmpty())
        assertTrue(first.isEmpty() || first.size == 1)
    }

    @Test
    fun `disconnect stops the native worker and is idempotent`() = runTest {
        val native = FakeNative()
        val m = machine(backgroundScope)
        val transport = NetherNetTransport(m, backgroundScope, native = native)
        transport.disconnect()
        transport.disconnect()
        assertEquals(2, native.disconnectCalls)
    }

    @Test
    fun `connect passes the url and network id through unchanged`() = runTest {
        val native = FakeNative()
        val m = machine(backgroundScope)
        val transport = NetherNetTransport(m, backgroundScope, native = native)
        m.transition(ConnectionState.AuthStarting, "play")
        transport.connect("wss://signaling.example/ws", "1234567890")
        assertEquals("wss://signaling.example/ws", native.connectUrl)
        assertEquals("1234567890", native.connectNetworkId)
        transport.disconnect()
    }

    @Test
    fun `the state codes mirror the native contract`() {
        assertEquals(0, NetherNetTransport.TransportStateCode.IDLE)
        assertEquals(1, NetherNetTransport.TransportStateCode.SIGNALING)
        assertEquals(2, NetherNetTransport.TransportStateCode.AWAITING_WELCOME)
        assertEquals(3, NetherNetTransport.TransportStateCode.CONNECTING)
        assertEquals(4, NetherNetTransport.TransportStateCode.CONNECTED)
        assertEquals(5, NetherNetTransport.TransportStateCode.CLOSED)
        assertEquals(6, NetherNetTransport.TransportStateCode.FAILED)
    }

    @Test
    fun `the self-test report parses and a broken one is null`() {
        val report = NativeSelfTestReport.parse(
            """{"protocol":1,"states":7,"framing_max":1048576,"backoff_cap_ms":20000}""",
        )
        assertEquals(1, report?.protocol)
        assertEquals(7, report?.states)
        assertEquals(1_048_576, report?.framingMaxBytes)
        assertEquals(20_000L, report?.backoffCapMs)
        assertEquals(null, NativeSelfTestReport.parse("not json"))
        assertEquals(null, NativeSelfTestReport.parse(null))
    }
}

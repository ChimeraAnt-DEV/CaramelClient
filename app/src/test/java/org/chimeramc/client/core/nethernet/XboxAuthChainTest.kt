package org.chimeramc.client.core.nethernet

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The Xbox Live → XSTS → Minecraft chain against a real local HTTP server.
 *
 * Each fixture is the shape the real service returns, including the `DisplayClaims.xui` block that
 * carries the `uhs`/`xid` the next step needs. The XErr branches get their own tests because they are
 * the only failures in the chain with actionable copy.
 */
class XboxAuthChainTest {

    private lateinit var server: MockWebServer
    private lateinit var endpoints: XboxEndpoints
    private val clock = 1_000_000L

    private val chain get() = XboxAuthChain(
        transport = OkHttpTransport(),
        endpoints = endpoints,
        nowMs = { clock },
    )

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val base = server.url("/").toString().trimEnd('/')
        endpoints = XboxEndpoints(
            userAuth = "$base/user",
            xstsAuth = "$base/xsts",
            mcLogin = "$base/mc",
            mcEntitlements = "$base/entitlements",
            mcProfile = "$base/profile",
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun enqueueXbl(token: String = "xbl-token", uhs: String = "uhs-1") = server.enqueue(
        MockResponse(code = 200, body = """
            {"Token":"$token","DisplayClaims":{"xui":[{"uhs":"$uhs"}]}}
        """.trimIndent()),
    )

    private fun enqueueXsts(
        token: String = "xsts-token",
        uhs: String = "uhs-1",
        xid: String = "2535400000000000",
    ) = server.enqueue(
        MockResponse(code = 200, body = """
            {"Token":"$token","DisplayClaims":{"xui":[{"uhs":"$uhs","xid":"$xid"}]}}
        """.trimIndent()),
    )

    private fun enqueueMc(token: String = "mc-token", expiresIn: Long = 86_400L) = server.enqueue(
        MockResponse(code = 200, body = """{"access_token":"$token","expires_in":$expiresIn}"""),
    )

    private fun enqueueEntitlements(count: Int = 1) {
        val items = (1..count).joinToString(",") { """{"name":"item$it"}""" }
        server.enqueue(MockResponse(code = 200, body = """{"items":[$items]}"""))
    }

    private fun enqueueProfile(id: String = "2535400000000000", name: String = "Player") =
        server.enqueue(MockResponse(code = 200, body = """{"id":"$id","name":"$name"}"""))

    @Test
    fun `the full chain yields a minecraft session`() = runTest {
        enqueueXbl()
        enqueueXsts()
        enqueueMc()
        enqueueEntitlements()
        enqueueProfile()

        val steps = mutableListOf<String>()
        val xbl = chain.authenticateUser("ms-access") { steps.add(it) }
        val xsts = chain.authorizeXsts(xbl.token, xbl.uhs) { steps.add(it) }
        val mc = chain.loginWithXbox(xsts.uhs, xsts.token) { steps.add(it) }

        assertEquals("xbl-token", xbl.token)
        assertEquals("uhs-1", xbl.uhs)
        assertEquals("xsts-token", xsts.token)
        assertEquals("2535400000000000", xsts.xuid)
        assertEquals("mc-token", mc.accessToken)
        assertEquals("Player", mc.gamertag)
        assertEquals(86_400_000L, mc.expiresAtMs - clock)
        assertEquals(
            listOf(
                XboxAuthChain.Steps.XBL,
                XboxAuthChain.Steps.XSTS,
                XboxAuthChain.Steps.MC,
            ),
            steps,
        )
    }

    @Test
    fun `the minecraft login sends the xbl3 bearer header`() = runTest {
        enqueueMc()
        enqueueEntitlements()
        enqueueProfile()
        chain.loginWithXbox("uhs-9", "xsts-token")
        val request = server.takeRequest()
        assertEquals("XBL3.0 x=uhs-9;xsts-token", request.headers["Authorization"])
    }

    @Test
    fun `a missing token in the xbl response is a protocol failure`() = runTest {
        server.enqueue(MockResponse(code = 200, body = """{"Token":"","DisplayClaims":{}}"""))
        val e = runCatching { chain.authenticateUser("ms") }.exceptionOrNull()
        assertTrue(e is XboxAuthChain.ChainException)
        assertTrue((e as XboxAuthChain.ChainException).error is NetherNetError.Protocol)
    }

    @Test
    fun `a 401 with a refresh gets exactly one retry`() = runTest {
        server.enqueue(MockResponse(code = 401, body = "{}"))
        enqueueXbl(token = "second")
        var refreshes = 0
        val session = chain.authenticateUser("stale-ms", refreshMsToken = {
            refreshes++
            "fresh-ms"
        })
        assertEquals("second", session.token)
        assertEquals(1, refreshes)
        assertEquals(2, server.requestCount)
        // The retry must carry the refreshed ticket, not the stale one.
        server.takeRequest()
        val retry = server.takeRequest()
        assertTrue(retry.body?.utf8().orEmpty().contains("fresh-ms"))
    }

    @Test
    fun `a 401 that survives the refresh is final, not a loop`() = runTest {
        server.enqueue(MockResponse(code = 401, body = "{}"))
        server.enqueue(MockResponse(code = 401, body = "{}"))
        var refreshes = 0
        val e = runCatching {
            chain.authenticateUser("stale-ms", refreshMsToken = {
                refreshes++
                "fresh-ms"
            })
        }.exceptionOrNull()
        assertTrue(e is XboxAuthChain.ChainException)
        assertTrue((e as XboxAuthChain.ChainException).error is NetherNetError.Auth)
        assertEquals("exactly one refresh, never a retry storm", 1, refreshes)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `a 429 is a rate-limit network failure`() = runTest {
        server.enqueue(MockResponse(code = 429, body = "{}"))
        val e = runCatching { chain.authenticateUser("ms") }.exceptionOrNull()
        assertTrue((e as XboxAuthChain.ChainException).error is NetherNetError.Network)
    }

    @Test
    fun `malformed json is a protocol failure`() = runTest {
        server.enqueue(MockResponse(code = 200, body = "not json"))
        val e = runCatching { chain.authenticateUser("ms") }.exceptionOrNull()
        assertTrue((e as XboxAuthChain.ChainException).error is NetherNetError.Protocol)
    }

    @Test
    fun `xerr no xbox profile routes to its own copy`() = runTest {
        server.enqueue(MockResponse(code = 401, body = """{"XErr":2148916233}"""))
        val e = runCatching { chain.authorizeXsts("xbl", "uhs") }.exceptionOrNull()
        val error = (e as XboxAuthChain.ChainException).error
        assertTrue(error is NetherNetError.Auth)
        assertTrue(error.reason.contains("no Xbox profile"))
    }

    @Test
    fun `xerr region blocked routes to its own copy`() = runTest {
        server.enqueue(MockResponse(code = 401, body = """{"XErr":2148916235}"""))
        val e = runCatching { chain.authorizeXsts("xbl", "uhs") }.exceptionOrNull()
        val error = (e as XboxAuthChain.ChainException).error
        assertTrue(error.reason.contains("region"))
    }

    @Test
    fun `xerr child account routes to its own copy`() = runTest {
        server.enqueue(MockResponse(code = 401, body = """{"XErr":2148916238}"""))
        val e = runCatching { chain.authorizeXsts("xbl", "uhs") }.exceptionOrNull()
        val error = (e as XboxAuthChain.ChainException).error
        assertTrue(error.reason.contains("child account"))
        assertTrue(error.reason.contains("family"))
    }

    @Test
    fun `an unknown xerr falls back to the generic auth copy`() = runTest {
        server.enqueue(MockResponse(code = 401, body = """{"XErr":123456789}"""))
        val e = runCatching { chain.authorizeXsts("xbl", "uhs") }.exceptionOrNull()
        val error = (e as XboxAuthChain.ChainException).error
        assertTrue(error is NetherNetError.Auth)
        assertTrue(!error.reason.contains("123456789"))
    }

    @Test
    fun `an account with no entitlement is refused with ownership copy`() = runTest {
        enqueueMc()
        server.enqueue(MockResponse(code = 200, body = """{"items":[]}"""))
        val e = runCatching { chain.loginWithXbox("uhs", "xsts") }.exceptionOrNull()
        val error = (e as XboxAuthChain.ChainException).error
        assertTrue(error.reason.contains("does not own Minecraft"))
    }

    @Test
    fun `an account with no profile is refused with profile copy`() = runTest {
        enqueueMc()
        enqueueEntitlements()
        server.enqueue(MockResponse(code = 404, body = "{}"))
        val e = runCatching { chain.loginWithXbox("uhs", "xsts") }.exceptionOrNull()
        val error = (e as XboxAuthChain.ChainException).error
        assertTrue(error is NetherNetError.Auth)
        assertTrue(error.reason.contains("Minecraft profile"))
    }

    @Test
    fun `the user-facing copy never leaks a raw code or a stack word`() = runTest {
        server.enqueue(MockResponse(code = 500, body = "boom"))
        val e = runCatching { chain.authenticateUser("ms") }.exceptionOrNull()
        val reason = (e as XboxAuthChain.ChainException).error.reason
        assertTrue(!reason.contains("500"))
        assertTrue(!reason.contains("null"))
        assertTrue(!reason.contains("Exception"))
    }
}

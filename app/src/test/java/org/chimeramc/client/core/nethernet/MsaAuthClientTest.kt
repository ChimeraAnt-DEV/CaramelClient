package org.chimeramc.client.core.nethernet

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The device-code flow against a real local HTTP server.
 *
 * MockWebServer rather than a mocked [HttpTransport]: the flow's whole risk is in how it reacts to
 * the server's actual responses and timing (pending, slow_down, expiry), so the test drives the real
 * OkHttp path and scripts the server's answers.
 */
class MsaAuthClientTest {

    private lateinit var server: MockWebServer
    private lateinit var endpoints: MsaEndpoints
    private var clock = 0L

    private val client get() = MsaAuthClient(
        transport = OkHttpTransport(),
        endpoints = endpoints,
        clientId = "test-client",
        nowMs = { clock },
    )

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val base = server.url("/").toString().trimEnd('/')
        endpoints = MsaEndpoints(
            deviceCode = "$base/devicecode",
            token = "$base/token",
            authorize = "$base/authorize",
        )
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun grant(interval: Long = 0L) = DeviceCodeGrant(
        deviceCode = "dc",
        userCode = "ABCD",
        verificationUri = "https://microsoft.com/link",
        expiresInSeconds = 900L,
        intervalSeconds = interval,
    )

    @Test
    fun `a device code is parsed with the fields the UI shows`() = runTest {
        server.enqueue(MockResponse(code = 200, body = """
            {"device_code":"dc","user_code":"WXYZ-1234",
             "verification_uri":"https://microsoft.com/link","expires_in":900,"interval":5}
        """.trimIndent()))
        val g = client.requestDeviceCode()
        assertEquals("dc", g.deviceCode)
        assertEquals("WXYZ-1234", g.userCode)
        assertEquals("https://microsoft.com/link", g.verificationUri)
        assertEquals(5L, g.intervalSeconds)
    }

    @Test
    fun `an incomplete device code response is a protocol failure`() = runTest {
        server.enqueue(MockResponse(code = 200, body = """{"device_code":"dc"}"""))
        val e = runCatching { client.requestDeviceCode() }.exceptionOrNull()
        assertTrue(e is MsaAuthClient.MsaException)
        assertTrue((e as MsaAuthClient.MsaException).error is NetherNetError.Protocol)
    }

    @Test
    fun `pending then success yields tokens with a refresh token`() = runTest {
        server.enqueue(MockResponse(code = 400, body = """{"error":"authorization_pending"}"""))
        server.enqueue(MockResponse(code = 200, body = """
            {"access_token":"at","refresh_token":"rt","expires_in":3600,"scope":"XboxLive.signin"}
        """.trimIndent()))
        val tokens = client.pollForTokens(grant(), deadlineMs = 1_000_000L) { false }
        assertEquals("at", tokens.accessToken)
        assertEquals("rt", tokens.refreshToken)
        assertEquals(3_600_000L, tokens.expiresAtMs - clock)
    }

    @Test
    fun `a token response without a refresh token is refused`() = runTest {
        server.enqueue(MockResponse(code = 200, body = """{"access_token":"at","expires_in":3600}"""))
        val e = runCatching {
            client.pollForTokens(grant(), deadlineMs = 1_000_000L) { false }
        }.exceptionOrNull() as MsaAuthClient.MsaException
        val error = e.error
        assertTrue("a missing offline_access must be its own failure", error is NetherNetError.Auth)
        assertTrue(error.reason.contains("stay signed in"))
    }

    @Test
    fun `slow_down widens the interval instead of failing`() = runTest {
        server.enqueue(MockResponse(code = 400, body = """{"error":"slow_down"}"""))
        server.enqueue(MockResponse(code = 200, body = """
            {"access_token":"at","refresh_token":"rt","expires_in":3600}
        """.trimIndent()))
        val tokens = client.pollForTokens(grant(interval = 1L), deadlineMs = 1_000_000L) { false }
        assertEquals("at", tokens.accessToken)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `an expired device code is reported as such`() = runTest {
        server.enqueue(MockResponse(code = 400, body = """{"error":"expired_token"}"""))
        val e = runCatching {
            client.pollForTokens(grant(), deadlineMs = 1_000_000L) { false }
        }.exceptionOrNull() as MsaAuthClient.MsaException
        assertTrue((e.error as NetherNetError.Auth).reason.contains("expired"))
    }

    @Test
    fun `access denied is reported without blame`() = runTest {
        server.enqueue(MockResponse(code = 400, body = """{"error":"access_denied"}"""))
        val e = runCatching {
            client.pollForTokens(grant(), deadlineMs = 1_000_000L) { false }
        }.exceptionOrNull() as MsaAuthClient.MsaException
        val reason = e.error.reason
        assertTrue(reason.contains("denied"))
        assertTrue(!reason.contains("null") && !reason.contains("exception"))
    }

    @Test
    fun `the poll loop exits at the deadline even when the server never answers`() = runTest {
        // The server always says "keep waiting"; the deadline is what must end the loop.
        repeat(200) {
            server.enqueue(MockResponse(code = 400, body = """{"error":"authorization_pending"}"""))
        }
        clock = 0L
        val e = runCatching {
            // Deadline is in the past relative to the fake clock after the first iteration.
            client.pollForTokens(grant(interval = 0L), deadlineMs = 0L) { false }
        }.exceptionOrNull() as MsaAuthClient.MsaException
        assertTrue("the loop must stop at the deadline", e.error is NetherNetError.Timeout)
        assertEquals("no request should be sent once the deadline has passed", 0, server.requestCount)
    }

    @Test
    fun `cancel aborts the poll loop immediately`() = runTest {
        // Cancelled from the start: the loop must check before it ever touches the network, so the
        // request count stays zero and no socket is opened.
        val e = runCatching {
            client.pollForTokens(grant(interval = 0L), deadlineMs = 1_000_000L) { true }
        }.exceptionOrNull()
        assertTrue(e is MsaAuthClient.MsaException)
        assertTrue((e as MsaAuthClient.MsaException).error is NetherNetError.Cancelled)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `cancel mid-poll stops after the in-flight response`() = runTest {
        // One pending response, then cancel: the loop must not send a second request.
        server.enqueue(MockResponse(code = 400, body = """{"error":"authorization_pending"}"""))
        var checks = 0
        val e = runCatching {
            client.pollForTokens(grant(interval = 1L), deadlineMs = 1_000_000L) {
                checks++
                checks > 1
            }
        }.exceptionOrNull()
        assertTrue(e is MsaAuthClient.MsaException)
        assertTrue((e as MsaAuthClient.MsaException).error is NetherNetError.Cancelled)
        assertEquals("only the first request may be sent", 1, server.requestCount)
    }

    @Test
    fun `a 401 on the token endpoint is an auth failure`() = runTest {
        server.enqueue(MockResponse(code = 401, body = """{"error":"invalid_client"}"""))
        val e = runCatching {
            client.pollForTokens(grant(), deadlineMs = 1_000_000L) { false }
        }.exceptionOrNull() as MsaAuthClient.MsaException
        assertTrue(e.error is NetherNetError.Auth)
    }

    @Test
    fun `invalid_grant on refresh requires signing in again`() = runTest {
        server.enqueue(MockResponse(code = 400, body = """{"error":"invalid_grant"}"""))
        val e = runCatching { client.refresh("old-refresh") }.exceptionOrNull()
        assertTrue(e is MsaAuthClient.MsaException)
        val error = (e as MsaAuthClient.MsaException).error
        assertTrue(error is NetherNetError.Auth)
        assertTrue(error.reason.contains("expired"))
    }

    @Test
    fun `the authorize url carries pkce and the redirect`() = runTest {
        val url = client.authorizeUrl(
            redirectUri = "msal://callback",
            state = "st",
            codeChallenge = "ch",
        )
        assertTrue(url.contains("code_challenge=ch"))
        assertTrue(url.contains("code_challenge_method=S256"))
        assertTrue(url.contains("state=st"))
        assertTrue(url.contains("redirect_uri=msal%3A%2F%2Fcallback"))
        assertTrue(url.contains("response_type=code"))
    }

    @Test
    fun `the real transport applies the per-call timeout`() = runTest {
        // A response delayed past the call timeout must surface as a Network failure, not a hang.
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("Content-Type", "application/json")
                .body("""{"device_code":"dc","user_code":"u","verification_uri":"v"}""")
                .bodyDelay(2, TimeUnit.SECONDS)
                .build(),
        )
        val e = runCatching { client.requestDeviceCode(timeoutMs = 300L) }.exceptionOrNull()
        assertTrue("a stalled server must fail as a network error", e is MsaAuthClient.MsaException)
        assertTrue((e as MsaAuthClient.MsaException).error is NetherNetError.Network)
    }
}

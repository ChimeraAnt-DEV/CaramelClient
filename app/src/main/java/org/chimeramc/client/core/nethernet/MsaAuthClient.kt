package org.chimeramc.client.core.nethernet

import java.io.IOException
import kotlinx.coroutines.delay
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONObject

/** A device-code grant: what to show the user, and what to poll with. */
data class DeviceCodeGrant(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    /** Seconds until the device code itself expires. */
    val expiresInSeconds: Long,
    /** The server-mandated minimum poll interval, in seconds. */
    val intervalSeconds: Long,
)

/** A successful token exchange. */
data class MsaTokens(
    val accessToken: String,
    val refreshToken: String,
    /** Monotonic milliseconds at which [accessToken] expires, computed from `expires_in`. */
    val expiresAtMs: Long,
    val scope: String,
)

/** The endpoints the MSA flow talks to. Overridable so tests point at a MockWebServer. */
data class MsaEndpoints(
    val deviceCode: String = "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode",
    val token: String = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token",
    val authorize: String = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize",
)

/**
 * Microsoft account sign-in, over the device-code flow.
 *
 * The device-code flow is the one that fits a launcher: the user types a short code into any browser
 * on any device, and the app polls. The polling is the part that has to be bounded — the spec allows
 * the server to answer `authorization_pending` indefinitely — so [pollForTokens] takes an explicit
 * deadline and a cancellation check, and it honours `slow_down` by widening its own interval.
 *
 * The class holds no state between calls. The state machine owns where the flow is; this owns the
 * protocol. Every wait is cancellable through the `isCancelled` predicate, which the caller wires to
 * the Cancel button, so a user can always back out of a wait that is otherwise minutes long.
 */
class MsaAuthClient(
    private val transport: HttpTransport,
    private val endpoints: MsaEndpoints = MsaEndpoints(),
    private val clientId: String,
    private val scope: String = "XboxLive.signin offline_access",
    private val nowMs: () -> Long = { System.nanoTime() / 1_000_000L },
) {

    /** A step failed in a way the UI can explain. */
    class MsaException(val error: NetherNetError) : Exception(error.reason)

    /** Requests a device code. The caller shows [DeviceCodeGrant.userCode] and the verification URL. */
    suspend fun requestDeviceCode(timeoutMs: Long = 20_000L): DeviceCodeGrant {
        val result = post(
            endpoints.deviceCode,
            mapOf("client_id" to clientId, "scope" to scope),
            timeoutMs,
            "requesting a sign-in code",
        )
        if (!result.isSuccess) {
            throw MsaException(classify(result, "requesting a sign-in code"))
        }
        val json = parse(result.body, "requesting a sign-in code")
        val grant = DeviceCodeGrant(
            deviceCode = json.optString("device_code"),
            userCode = json.optString("user_code"),
            verificationUri = json.optString("verification_uri")
                .ifEmpty { json.optString("verification_url") },
            expiresInSeconds = json.optLong("expires_in", 900L),
            intervalSeconds = json.optLong("interval", 5L),
        )
        if (grant.deviceCode.isEmpty() || grant.userCode.isEmpty() || grant.verificationUri.isEmpty()) {
            throw MsaException(NetherNetError.Protocol("requesting a sign-in code", "incomplete device code"))
        }
        return grant
    }

    /**
     * Polls the token endpoint until the user approves, the code expires, or [deadlineMs] passes.
     *
     * @param deadlineMs an absolute monotonic deadline. The loop never outlives it, so even a server
     *   that answers `authorization_pending` forever cannot hold the UI.
     * @param isCancelled checked before every wait and after every response, so Cancel is immediate.
     */
    suspend fun pollForTokens(
        grant: DeviceCodeGrant,
        deadlineMs: Long,
        isCancelled: () -> Boolean,
    ): MsaTokens {
        var intervalMs = (grant.intervalSeconds.coerceAtLeast(1L)) * 1_000L
        while (true) {
            if (isCancelled()) throw MsaException(NetherNetError.Cancelled("waiting for you to finish signing in"))
            if (nowMs() >= deadlineMs) {
                throw MsaException(NetherNetError.Timeout("waiting for you to finish signing in"))
            }
            val remaining = (deadlineMs - nowMs()).coerceAtLeast(1_000L)
            val result = post(
                endpoints.token,
                mapOf(
                    "client_id" to clientId,
                    "grant_type" to "urn:ietf:params:oauth:grant-type:device_code",
                    "device_code" to grant.deviceCode,
                ),
                timeoutMs = remaining,
                step = "finishing sign-in",
            )
            val json = parse(result.body, "finishing sign-in")
            if (result.isSuccess) return tokensFrom(json)

            when (json.optString("error")) {
                "authorization_pending" -> Unit
                "slow_down" -> intervalMs += 5_000L
                "expired_token" -> throw MsaException(
                    NetherNetError.Auth(
                        "signing in",
                        "The sign-in code expired before it was approved.",
                        "Request a new code and try again.",
                    ),
                )
                "access_denied" -> throw MsaException(
                    NetherNetError.Auth(
                        "signing in",
                        "The sign-in request was denied.",
                        "Approve the request in the browser to continue.",
                    ),
                )
                else -> throw MsaException(classify(result, "finishing sign-in"))
            }
            // Wait the interval, but never past the deadline, and wake at once on cancel.
            val waitMs = minOf(intervalMs, (deadlineMs - nowMs()).coerceAtLeast(0L))
            if (waitMs <= 0L) {
                throw MsaException(NetherNetError.Timeout("waiting for you to finish signing in"))
            }
            delay(waitMs)
        }
    }

    /** Exchanges a refresh token for a new access token. Used by the proactive/reactive refresh path. */
    suspend fun refresh(refreshToken: String, timeoutMs: Long = 15_000L): MsaTokens {
        val result = post(
            endpoints.token,
            mapOf(
                "client_id" to clientId,
                "grant_type" to "refresh_token",
                "refresh_token" to refreshToken,
                "scope" to scope,
            ),
            timeoutMs,
            "refreshing your sign-in",
        )
        val json = parse(result.body, "refreshing your sign-in")
        if (result.isSuccess) return tokensFrom(json)
        if (json.optString("error") == "invalid_grant") {
            throw MsaException(NetherNetError.reauthRequired())
        }
        throw MsaException(classify(result, "refreshing your sign-in"))
    }

    /** The authorize URL for the Custom-Tabs flow, with PKCE-ready state. */
    fun authorizeUrl(redirectUri: String, state: String, codeChallenge: String): String {
        val url = endpoints.authorize.toHttpUrl().newBuilder()
            .addQueryParameter("client_id", clientId)
            .addQueryParameter("response_type", "code")
            .addQueryParameter("redirect_uri", redirectUri)
            .addQueryParameter("response_mode", "query")
            .addQueryParameter("scope", scope)
            .addQueryParameter("state", state)
            .addQueryParameter("code_challenge", codeChallenge)
            .addQueryParameter("code_challenge_method", "S256")
            .build()
        return url.toString()
    }

    /** Exchanges an authorization code (Custom-Tabs flow) for tokens. */
    suspend fun exchangeCode(
        code: String,
        redirectUri: String,
        codeVerifier: String,
        timeoutMs: Long = 20_000L,
    ): MsaTokens {
        val result = post(
            endpoints.token,
            mapOf(
                "client_id" to clientId,
                "grant_type" to "authorization_code",
                "code" to code,
                "redirect_uri" to redirectUri,
                "code_verifier" to codeVerifier,
                "scope" to scope,
            ),
            timeoutMs,
            "finishing sign-in",
        )
        val json = parse(result.body, "finishing sign-in")
        if (!result.isSuccess) throw MsaException(classify(result, "finishing sign-in"))
        return tokensFrom(json)
    }

    private fun tokensFrom(json: JSONObject): MsaTokens {
        val access = json.optString("access_token")
        val refresh = json.optString("refresh_token")
        if (access.isEmpty()) {
            throw MsaException(NetherNetError.Protocol("signing in", "no access token returned"))
        }
        // Without offline_access there is no refresh token, and the session would silently die when
        // the access token expires. Refusing here keeps that failure visible instead of deferred.
        if (refresh.isEmpty()) throw MsaException(NetherNetError.authScopeMissing())
        val expiresIn = json.optLong("expires_in", 3600L)
        return MsaTokens(
            accessToken = access,
            refreshToken = refresh,
            expiresAtMs = nowMs() + expiresIn * 1_000L,
            scope = json.optString("scope"),
        )
    }

    private suspend fun post(
        url: String,
        form: Map<String, String>,
        timeoutMs: Long,
        step: String,
        headers: Map<String, String> = emptyMap(),
    ): HttpResult {
        return try {
            transport.postForm(url, form, timeoutMs, headers)
        } catch (e: IOException) {
            throw MsaException(NetherNetError.Network(step, e::class.java.simpleName))
        }
    }

    private fun parse(body: String, step: String): JSONObject {
        return try {
            JSONObject(body)
        } catch (e: Exception) {
            throw MsaException(NetherNetError.Protocol(step, "unreadable response"))
        }
    }

    private fun classify(result: HttpResult, step: String): NetherNetError = when (result.status) {
        400, 401, 403 -> NetherNetError.Auth(
            step,
            "Microsoft refused the sign-in request.",
            "Check that the account can sign in, then try again.",
        )
        429 -> NetherNetError.Network(step, "rate limited")
        in 500..599 -> NetherNetError.Network(step, "server error")
        else -> NetherNetError.Unknown(step, "http ${result.status}")
    }
}

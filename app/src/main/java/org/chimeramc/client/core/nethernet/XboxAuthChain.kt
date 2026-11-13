package org.chimeramc.client.core.nethernet

import java.io.IOException
import org.json.JSONArray
import org.json.JSONObject

/** What Xbox Live user authentication returns. */
data class XblSession(val token: String, val uhs: String)

/** What XSTS returns, plus the Xbox user id it belongs to. */
data class XstsSession(val token: String, val uhs: String, val xuid: String)

/** What Minecraft services returns: a bearer token, and the profile it belongs to. */
data class MinecraftSession(
    val accessToken: String,
    val expiresAtMs: Long,
    val xuid: String,
    val gamertag: String,
)

/** The auth endpoints, overridable so tests point at a MockWebServer. */
data class XboxEndpoints(
    val userAuth: String = "https://user.auth.xboxlive.com/user/authenticate",
    val xstsAuth: String = "https://xsts.auth.xboxlive.com/xsts/authorize",
    val mcLogin: String = "https://multiplayer.minecraft.net/authentication",
    val mcEntitlements: String = "https://api.minecraftservices.com/entitlements/mcstore",
    val mcProfile: String = "https://api.minecraftservices.com/minecraft/profile",
)

/**
 * The Xbox Live → XSTS → Minecraft chain, in the order the launcher has to walk it.
 *
 * The three steps live in one class because they share exactly one policy: a 401 gets one token
 * refresh and one retry, everything else is final. Splitting them would mean three copies of that
 * policy drifting apart. Each step reports through [onStep] so the caller's state machine can show
 * which one is in flight, which is the whole point of having the state machine at all.
 *
 * The XErr codes XSTS returns are the only place an Xbox sign-in fails with something the user can
 * act on, so they are mapped to distinct copy in [NetherNetError.fromXErr] rather than collapsed
 * into "sign-in failed".
 */
class XboxAuthChain(
    private val transport: HttpTransport,
    private val endpoints: XboxEndpoints = XboxEndpoints(),
    private val nowMs: () -> Long = { System.nanoTime() / 1_000_000L },
    private val stepTimeoutMs: Long = 15_000L,
) {

    /** The step names, matching [ConnectionState]'s names so [onStep] can drive the state machine. */
    object Steps {
        const val XBL = "SigningInXboxLive"
        const val XSTS = "AuthorizingXsts"
        const val MC = "FetchingMinecraftToken"
    }

    /** Thrown when a step fails; [error] is the user-facing taxonomy. */
    class ChainException(val error: NetherNetError) : Exception(error.reason)

    /**
     * Authenticates against Xbox Live.
     *
     * @param msAccessToken the MSA access token, used as the RPS ticket.
     * @param onStep invoked with a [Steps] constant as each stage begins.
     */
    suspend fun authenticateUser(
        msAccessToken: String,
        refreshMsToken: (suspend () -> String)? = null,
        onStep: suspend (String) -> Unit = {},
    ): XblSession {
        onStep(Steps.XBL)
        val json = postXblUserAuth(msAccessToken, refreshMsToken)
        val token = json.optString("Token")
        val uhs = json.optJSONObject("DisplayClaims")
            ?.optJSONArray("xui")
            ?.optJSONObject(0)
            ?.optString("uhs")
            .orEmpty()
        if (token.isEmpty() || uhs.isEmpty()) {
            throw ChainException(NetherNetError.Protocol("signing in to Xbox Live", "no token or uhs"))
        }
        return XblSession(token, uhs)
    }

    /**
     * Posts the RPS ticket, retrying once with a refreshed MSA token if Xbox Live answers 401.
     *
     * One retry, not a loop: a 401 that survives a refresh is a real sign-in failure, and retrying it
     * is how a client turns an expired session into a rate-limit ban.
     */
    private suspend fun postXblUserAuth(
        msAccessToken: String,
        refreshMsToken: (suspend () -> String)?,
    ): JSONObject {
        var ticket = msAccessToken
        var attempts = 0
        while (true) {
            val (body, status) = call(
                url = endpoints.userAuth,
                body = JSONObject().apply {
                    put("Properties", JSONObject().apply {
                        put("AuthMethod", "RPS")
                        put("SiteName", "user.auth.xboxlive.com")
                        put("RpsTicket", "d=$ticket")
                    })
                    put("RelyingParty", "http://auth.xboxlive.com")
                    put("TokenType", "JWT")
                },
                step = "signing in to Xbox Live",
            )
            if (body != null) return body
            if (status == 401 && refreshMsToken != null && attempts < 1) {
                attempts++
                ticket = refreshMsToken.invoke()
                continue
            }
            throw ChainException(classify(status!!, "signing in to Xbox Live"))
        }
    }

    /** Authorizes against XSTS. Maps the XErr codes to their own copy. */
    suspend fun authorizeXsts(
        xblToken: String,
        uhs: String,
        onStep: suspend (String) -> Unit = {},
    ): XstsSession {
        onStep(Steps.XSTS)
        val json = callJson(
            url = endpoints.xstsAuth,
            body = JSONObject().apply {
                put("Properties", JSONObject().apply {
                    put("SandboxId", "RETAIL")
                    put("UserTokens", JSONArray().put(xblToken))
                })
                put("RelyingParty", "http://xboxlive.com")
                put("TokenType", "JWT")
            },
            step = "authorizing with Xbox Live",
            mapXErr = true,
        )
        val token = json.optString("Token")
        val claims = json.optJSONObject("DisplayClaims")?.optJSONArray("xui")?.optJSONObject(0)
        val claimUhs = claims?.optString("uhs").orEmpty().ifEmpty { uhs }
        val xuid = claims?.optString("xid").orEmpty()
        if (token.isEmpty()) {
            throw ChainException(NetherNetError.Protocol("authorizing with Xbox Live", "no token"))
        }
        return XstsSession(token, claimUhs, xuid)
    }

    /** Logs in to Minecraft services and reads the profile. Refuses an account with no entitlement. */
    suspend fun loginWithXbox(
        uhs: String,
        xstsToken: String,
        onStep: suspend (String) -> Unit = {},
    ): MinecraftSession {
        onStep(Steps.MC)
        val login = callJson(
            url = endpoints.mcLogin,
            body = JSONObject().apply {
                put("identityPublicKey", MC_PUBLIC_KEY)
            },
            step = "checking your Minecraft account",
            headers = mapOf("Authorization" to "XBL3.0 x=$uhs;$xstsToken"),
        )
        val mcToken = login.optString("access_token").ifEmpty { login.optString("accessToken") }
        val expiresIn = login.optLong("expires_in", 86_400L)
        if (mcToken.isEmpty()) {
            throw ChainException(
                NetherNetError.Auth(
                    "checking your Minecraft account",
                    "Microsoft would not issue a Minecraft session for this account.",
                    "Try signing in again.",
                ),
            )
        }

        // Ownership, then profile: an account that owns nothing has no profile to read, and saying
        // "no ownership" is more useful than a bare 404.
        val entitlements = get(
            endpoints.mcEntitlements,
            mcToken,
            "checking your Minecraft account",
        )
        val owned = entitlements.optJSONArray("items")?.length() ?: 0
        if (entitlements.has("items") && owned == 0) {
            throw ChainException(NetherNetError.noMinecraftOwnership())
        }

        val profile = get(
            endpoints.mcProfile,
            mcToken,
            "checking your Minecraft account",
            notFound = NetherNetError.noProfile(),
        )
        if (!profile.has("id") || profile.optString("id").isEmpty()) {
            throw ChainException(NetherNetError.noProfile())
        }
        return MinecraftSession(
            accessToken = mcToken,
            expiresAtMs = nowMs() + expiresIn * 1_000L,
            xuid = profile.optString("id"),
            gamertag = profile.optString("name"),
        )
    }

    private suspend fun get(
        url: String,
        token: String,
        step: String,
        notFound: NetherNetError? = null,
    ): JSONObject {
        val result = try {
            transport.get(url, stepTimeoutMs, mapOf("Authorization" to "Bearer $token"))
        } catch (e: IOException) {
            throw ChainException(NetherNetError.Network(step, e::class.java.simpleName))
        }
        if (result.status == 404 && notFound != null) throw ChainException(notFound)
        if (!result.isSuccess) throw ChainException(classify(result.status, step))
        return parse(result.body.ifEmpty { "{}" }, step)
    }

    /** One call: `(parsed body, null)` on success, `(null, status)` on a non-2xx response. */
    private suspend fun call(
        url: String,
        body: JSONObject,
        step: String,
        mapXErr: Boolean = false,
        headers: Map<String, String> = emptyMap(),
        method: Method = Method.JSON,
    ): Pair<JSONObject?, Int?> {
        val result = try {
            when (method) {
                Method.JSON -> transport.postJson(url, body.toString(), stepTimeoutMs, headers)
                Method.FORM -> {
                    val form = body.keys().asSequence().associateWith { body.get(it).toString() }
                    transport.postForm(url, form, stepTimeoutMs, headers)
                }
            }
        } catch (e: IOException) {
            throw ChainException(NetherNetError.Network(step, e::class.java.simpleName))
        }
        if (result.isSuccess) return parse(result.body, step) to null
        val json = runCatching { JSONObject(result.body) }.getOrNull()
        if (mapXErr) {
            val xerr = json?.optLong("XErr", 0L) ?: 0L
            if (xerr != 0L) NetherNetError.fromXErr(xerr)?.let { throw ChainException(it) }
        }
        return null to result.status
    }

    /** The common case: a call that must succeed. */
    private suspend fun callJson(
        url: String,
        body: JSONObject,
        step: String,
        mapXErr: Boolean = false,
        headers: Map<String, String> = emptyMap(),
        method: Method = Method.JSON,
    ): JSONObject {
        val (json, status) = call(url, body, step, mapXErr, headers, method)
        return json ?: throw ChainException(classify(status!!, step))
    }

    private fun parse(body: String, step: String): JSONObject = try {
        JSONObject(body)
    } catch (e: Exception) {
        throw ChainException(NetherNetError.Protocol(step, "unreadable response"))
    }

    private fun classify(status: Int, step: String): NetherNetError = when (status) {
        401, 403 -> NetherNetError.Auth(
            step,
            "Microsoft rejected the sign-in.",
            "Sign in again to continue.",
        )
        429 -> NetherNetError.Network(step, "rate limited")
        in 500..599 -> NetherNetError.Network(step, "server error")
        else -> NetherNetError.Unknown(step, "http $status")
    }

    private enum class Method { JSON, FORM }

    private companion object {
        /** The public key Minecraft services expects; not a secret, and identical for every client. */
        const val MC_PUBLIC_KEY =
            "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAECRXueJeTDqNRRgJi/vlRufByu/2G0i2Ebt6YMar5QX/R0DIIyrJ" +
                "L4SSTC1ldAkclTTQqL7CpG4kgtu1YvAPRRfnhKcBlqwqGgPKZXfu7HEtwLTWi1v/yFRB6EmZU7n89"
    }
}

package org.chimeramc.client.core.nethernet

import org.json.JSONObject

/**
 * The token chain for one signed-in account.
 *
 * Every field is a secret except the two identifiers, and even those are personal data, so this
 * object is never logged, never serialised outside [TokenStore], and never put in a diagnostics
 * buffer. [toString] is overridden to redact, because the one realistic way these leak is an
 * accidental string interpolation into a log line or an exception message.
 */
data class TokenSnapshot(
    val msAccess: String = "",
    val msRefresh: String = "",
    /** Monotonic milliseconds; not wall-clock, so a clock change cannot extend a token's life. */
    val msExpiresAtMs: Long = 0L,
    val xblToken: String = "",
    val xstsToken: String = "",
    val mcToken: String = "",
    val mcExpiresAtMs: Long = 0L,
    val xuid: String = "",
    val gamertag: String = "",
) {

    val hasMsToken: Boolean get() = msAccess.isNotEmpty() && msRefresh.isNotEmpty()

    /** Redacted. Safe to interpolate anywhere. */
    override fun toString(): String =
        "TokenSnapshot(ms=***, refresh=***, xbl=***, xsts=***, mc=***, xuid=***, gamertag=***)"

    fun toJson(): String = JSONObject().apply {
        put("ms_access", msAccess)
        put("ms_refresh", msRefresh)
        put("ms_expires_at", msExpiresAtMs)
        put("xbl_token", xblToken)
        put("xsts_token", xstsToken)
        put("mc_token", mcToken)
        put("mc_expires_at", mcExpiresAtMs)
        put("xuid", xuid)
        put("gamertag", gamertag)
    }.toString()

    companion object {
        fun fromJson(json: String): TokenSnapshot {
            val o = JSONObject(json)
            return TokenSnapshot(
                msAccess = o.optString("ms_access"),
                msRefresh = o.optString("ms_refresh"),
                msExpiresAtMs = o.optLong("ms_expires_at"),
                xblToken = o.optString("xbl_token"),
                xstsToken = o.optString("xsts_token"),
                mcToken = o.optString("mc_token"),
                mcExpiresAtMs = o.optLong("mc_expires_at"),
                xuid = o.optString("xuid"),
                gamertag = o.optString("gamertag"),
            )
        }
    }
}

/** Seals and opens the token blob. The production implementation is backed by Android Keystore. */
interface SecretBox {
    fun seal(plaintext: ByteArray): ByteArray
    fun open(ciphertext: ByteArray): ByteArray
}

/** Where the sealed blob lives. Production uses a private file; tests use memory. */
interface TokenStorage {
    fun read(): ByteArray?
    fun write(blob: ByteArray)
    fun clear()
}

/** The store could not open or seal the blob. */
class TokenStoreException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * The encrypted token store.
 *
 * The tokens are sealed with [SecretBox] and the sealed blob is the only thing [TokenStorage] ever
 * sees, so no plaintext token reaches disk, SharedPreferences, logcat or a crash report. The key
 * material lives in [SecretBox]'s implementation (Android Keystore in production), never here.
 *
 * The expiry rule is the one piece of policy worth pinning: a token is treated as expired five
 * minutes early, so a token that is valid when read is still valid by the time the request it
 * authorises reaches the server.
 */
class TokenStore(
    private val secretBox: SecretBox,
    private val storage: TokenStorage,
    private val nowMs: () -> Long = { System.nanoTime() / 1_000_000L },
) {

    /** The freshness margin. A token inside this window is refreshed proactively. */
    val refreshMarginMs: Long = 5 * 60 * 1_000L

    /** Reads the stored snapshot, or an empty one when nothing is stored or it cannot be opened. */
    fun load(): TokenSnapshot {
        val blob = storage.read() ?: return TokenSnapshot()
        return try {
            TokenSnapshot.fromJson(String(secretBox.open(blob), Charsets.UTF_8))
        } catch (e: Exception) {
            // A blob that will not open means the key was rotated or the data is corrupt. Treat it
            // as "no session" rather than throwing into the UI: the user signs in again, which is
            // the only recovery either way.
            TokenSnapshot()
        }
    }

    /** Seals and persists [snapshot]. Throws [TokenStoreException] when the seal fails. */
    fun save(snapshot: TokenSnapshot) {
        val plain = snapshot.toJson().toByteArray(Charsets.UTF_8)
        try {
            storage.write(secretBox.seal(plain))
        } finally {
            plain.fill(0)
        }
    }

    /**
     * Removes the stored tokens.
     *
     * The sealed blob is overwritten with zeros before deletion where the storage allows it, so a
     * deleted row is not merely unlinked.
     */
    fun clearAll() {
        val existing = storage.read()
        if (existing != null) {
            existing.fill(0)
            runCatching { storage.write(existing) }
        }
        storage.clear()
    }

    /** True when the stored MSA access token is still valid outside the refresh margin. */
    fun hasFreshMsToken(): Boolean {
        val s = load()
        return s.hasMsToken && s.msExpiresAtMs - refreshMarginMs > nowMs()
    }

    /** True when the stored Minecraft token is still valid outside the refresh margin. */
    fun hasFreshMcToken(): Boolean {
        val s = load()
        return s.mcToken.isNotEmpty() && s.mcExpiresAtMs - refreshMarginMs > nowMs()
    }
}

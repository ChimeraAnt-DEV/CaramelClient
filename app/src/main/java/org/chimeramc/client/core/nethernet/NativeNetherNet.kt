package org.chimeramc.client.core.nethernet

import org.json.JSONObject

/**
 * The JNI surface of `libsweetloader_nethernet.so`.
 *
 * The native side is the only place the WebSocket, the framing and the signaling protocol live; this
 * object is a thin, total translation of its C ABI into Kotlin types. Every function is declared here
 * so `JniBindingParityTest` can check the exports against these declarations, and so a native build
 * that drops a symbol fails a test rather than a join.
 *
 * A missing library is a normal condition, not an error: an install without the arm64 native libs
 * (a build without the Rust toolchain, or a non-arm64 device) reports [isAvailable] false and the
 * launcher falls back to its RakNet path. Loading is attempted once.
 */
object NativeNetherNet {

    /** True when the library loaded and its exports are callable. */
    val isAvailable: Boolean by lazy {
        try {
            System.loadLibrary("sweetloader_nethernet")
            true
        } catch (e: UnsatisfiedLinkError) {
            false
        }
    }

    /**
     * Starts a connection. Returns false when the library is unavailable or the arguments are
     * rejected; a connection failure arrives through [nativeState].
     */
    external fun nativeConnect(url: String, networkId: String): Boolean

    /** The current state, as a [TransportState.code]. */
    external fun nativeState(): Int

    /** The last error's tag, or null. */
    external fun nativeErrorTag(): String?

    /** The last error's user-facing message, or null. */
    external fun nativeErrorMessage(): String?

    /** True when the last error is retryable. */
    external fun nativeErrorRetryable(): Boolean

    /** The session id the service assigned, or null. */
    external fun nativeSessionId(): String?

    /** Waits up to `timeoutMs` for a state change and returns the state code. */
    external fun nativeAwaitState(timeoutMs: Int): Int

    /** Takes the most recent relayed candidate payload, or null. */
    external fun nativeTakeCandidate(): String?

    /** Asks the transport to stop and waits for its worker. Idempotent. */
    external fun nativeDisconnect()

    /** True when a shutdown has been requested. */
    external fun nativeIsCancelled(): Boolean

    /** The budget for a state code, in milliseconds; 0 when it has none. */
    external fun nativeStateBudgetMs(stateCode: Int): Int

    /** The signaling protocol version this client speaks. */
    external fun nativeProtocolVersion(): Int

    /** A JSON self-test report, for the diagnostics screen. */
    external fun nativeSelfTest(): String?

    /** Logs a message into the native logcat tag. */
    external fun nativeLog(message: String)
}

/**
 * The parsed form of [NativeNetherNet.nativeSelfTest].
 *
 * A data class rather than raw JSON so the diagnostics screen cannot accidentally print a raw string
 * into the UI, and so a test can assert the fields.
 */
data class NativeSelfTestReport(
    val protocol: Int,
    val states: Int,
    val framingMaxBytes: Int,
    val backoffCapMs: Long,
) {
    companion object {
        /** Parses the report, or null when it is missing or unreadable. */
        fun parse(json: String?): NativeSelfTestReport? {
            if (json.isNullOrEmpty()) return null
            return try {
                val o = JSONObject(json)
                NativeSelfTestReport(
                    protocol = o.optInt("protocol"),
                    states = o.optInt("states"),
                    framingMaxBytes = o.optInt("framing_max"),
                    backoffCapMs = o.optLong("backoff_cap_ms"),
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

package org.chimeramc.client.core.nethernet

/**
 * The one error type the connection path reports.
 *
 * Every failure — a timeout, an HTTP status, a native signaling code, a protocol violation — is
 * mapped to exactly one variant here before it reaches the UI. The UI therefore never sees a raw
 * HTTP code, a native error number, or a stack trace: it sees a variant whose [reason] is written in
 * plain language and whose [retryable] flag decides whether a Retry button is offered.
 *
 * [step] is the connection stage the failure happened in, so the copy can say "while signing in to
 * Xbox Live" rather than "something went wrong".
 */
sealed class NetherNetError {

    /** The stage name as the diagnostics buffer and the copy both use. */
    abstract val step: String

    /** Plain-language description. Must never contain a raw code, "null", or "exception". */
    abstract val reason: String

    /** Whether pressing Retry could plausibly succeed. */
    open val retryable: Boolean = true

    /** A step exceeded its configured timeout. */
    data class Timeout(override val step: String) : NetherNetError() {
        override val reason: String = "Timed out while $step."
    }

    /** A transport-level network failure (no route, TLS failure, reset). */
    data class Network(override val step: String, val kind: String) : NetherNetError() {
        override val reason: String = "Could not reach the network while $step."
    }

    /** An authentication refusal, already phrased for the user. */
    data class Auth(override val step: String, val userMessage: String, val action: String) :
        NetherNetError() {
        override val reason: String = "$userMessage $action"
        override val retryable: Boolean = false
    }

    /** The signaling server returned a typed rejection. [code] is debug-only, never shown. */
    data class Signaling(override val step: String, val code: String) : NetherNetError() {
        override val reason: String = "The server refused the connection while $step."
    }

    /** ICE negotiation failed; the peers could not find a path. */
    data class Ice(override val step: String, val detail: String) : NetherNetError() {
        override val reason: String = "Could not establish a direct connection while $step."
    }

    /** A frame or signaling message violated the protocol. */
    data class Protocol(override val step: String, val detail: String) : NetherNetError() {
        override val reason: String = "The connection became unusable while $step."
    }

    /** The user cancelled. Not an error to apologise for, so the UI stays quiet. */
    data class Cancelled(override val step: String) : NetherNetError() {
        override val reason: String = "Cancelled."
        override val retryable: Boolean = false
    }

    /** A failure with no better classification. [cause] is for the debug log only. */
    data class Unknown(override val step: String, val cause: String) : NetherNetError() {
        override val reason: String = "Something went wrong while $step."
    }

    /** The XErr codes XSTS returns, each with its own actionable copy. */
    enum class XErr(val code: Long) {
        /** The account has no Xbox profile yet. */
        NO_XBOX_PROFILE(2148916233L),

        /** Xbox Live is not available in the account's region. */
        REGION_BLOCKED(2148916235L),

        /** A child account that is not in a Microsoft family. */
        CHILD_ACCOUNT(2148916238L),
    }

    companion object {
        /**
         * Maps an XSTS `XErr` value to its copy.
         *
         * Returns null for an unrecognised code, so the caller falls back to the generic auth copy
         * rather than inventing a reason for a code we do not understand.
         */
        fun fromXErr(code: Long): Auth? = when (code) {
            XErr.NO_XBOX_PROFILE.code -> Auth(
                step = "signing in to Xbox Live",
                userMessage = "This Microsoft account has no Xbox profile yet.",
                action = "Create one at xbox.com, then try again.",
            )
            XErr.REGION_BLOCKED.code -> Auth(
                step = "signing in to Xbox Live",
                userMessage = "Xbox Live is not available in this account's region.",
                action = "Xbox Live is required to sign in.",
            )
            XErr.CHILD_ACCOUNT.code -> Auth(
                step = "signing in to Xbox Live",
                userMessage = "This is a child account that is not in a Microsoft family.",
                action = "Ask a family organiser to add this account, then try again.",
            )
            else -> null
        }

        /** The copy for a missing Minecraft entitlement. */
        fun noMinecraftOwnership() = Auth(
            step = "checking Minecraft",
            userMessage = "This account does not own Minecraft.",
            action = "Sign in with an account that owns Minecraft: Bedrock Edition.",
        )

        /** The copy for an account that has not created a Minecraft profile. */
        fun noProfile() = Auth(
            step = "checking Minecraft",
            userMessage = "This account has not set up a Minecraft profile yet.",
            action = "Open Minecraft once with this account, then try again.",
        )

        /** The copy for a token that can no longer be refreshed (password change, MFA change). */
        fun reauthRequired() = Auth(
            step = "refreshing your sign-in",
            userMessage = "Your sign-in has expired and needs to be renewed.",
            action = "Sign in again to continue.",
        )

        /** The copy for a scope missing `offline_access`. */
        fun authScopeMissing() = Auth(
            step = "signing in",
            userMessage = "The sign-in did not grant permission to stay signed in.",
            action = "Try signing in again.",
        )

        /** The copy for a friends join that cannot use NetherNet. */
        fun netherNetUnavailable() = Unknown(
            step = "connecting to the session",
            cause = "NetherNet signaling is unavailable and this target requires it",
        ).let {
            Auth(
                step = it.step,
                userMessage = "This session can only be joined through NetherNet.",
                action = "Check your connection and try again.",
            )
        }
    }
}

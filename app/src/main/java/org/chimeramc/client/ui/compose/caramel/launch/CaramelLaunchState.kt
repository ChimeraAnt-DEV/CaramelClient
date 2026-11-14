package org.chimeramc.client.ui.compose.caramel.launch

import android.graphics.Bitmap

/**
 * The Launch screen's content, as plain data.
 *
 * <p>Same reason the hero has its own state object: the Compose screen must be a pure function of a
 * small immutable value so the presentation rules can be tested on the JVM without inflating a
 * layout or starting a version manager. The activity owns the state and pushes a new instance when
 * something changes.
 *
 * <p>Only fields the Caramel launch screen actually draws live here. Anything the screen does not
 * read stays in the activity, where it was already tracked.
 */
class CaramelLaunchState @JvmOverloads constructor(
    /** The name shown on the hero, or empty when no instance is selected. */
    val instanceName: String = "",
    /** The line under LAUNCH GAME: the build it will start. */
    val buildLine: String = "",
    /** The selected version id, shown on the version chip. */
    val versionId: String = "",
    /** Every installed version, newest first, for the picker. */
    val versions: List<Version> = emptyList(),
    /** Whether the launch control accepts a tap. */
    val launchEnabled: Boolean = false,
    /** A launch is in flight: the face shows progress rather than an idle action. */
    val launching: Boolean = false,
    /** The account name, or empty when signed out. */
    val accountName: String = "",
    /** The account avatar, or null when signed out or no avatar is set. */
    val avatar: Bitmap? = null,
    /** How many game instances are currently running (the title bar's count). */
    val runningInstances: Int = 0,
    /** The paint-splat drawable behind the character preview; 0 draws none. */
    val backgroundRes: Int = 0,
) {
    /** One row of the version picker. */
    class Version(val id: String, val beta: Boolean)

    /** Whether the account row should read as signed in. */
    val signedIn: Boolean get() = accountName.isNotEmpty()

    companion object {
        /** The empty state, before a version manager exists. */
        @JvmStatic
        fun empty(fallbackName: String): CaramelLaunchState =
            CaramelLaunchState(instanceName = fallbackName)
    }
}

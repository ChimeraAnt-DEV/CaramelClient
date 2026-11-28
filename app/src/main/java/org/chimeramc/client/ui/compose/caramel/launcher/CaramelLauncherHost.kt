package org.chimeramc.client.ui.compose.caramel.launcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import org.chimeramc.client.ui.compose.caramel.CaramelCurseForgeItem
import org.chimeramc.client.ui.compose.caramel.CaramelFileEntry
import org.chimeramc.client.ui.compose.caramel.CaramelLauncherState
import org.chimeramc.client.ui.compose.caramel.CaramelModInfo
import org.chimeramc.client.ui.compose.caramel.CaramelScreenshotInfo
import org.chimeramc.client.ui.compose.caramel.CaramelTheme
import org.chimeramc.client.ui.compose.caramel.CaramelCosmeticInfo
import org.chimeramc.client.ui.compose.caramel.CaramelServerInfo
import org.chimeramc.client.launcher.core.splash.SplashShow
import org.chimeramc.client.ui.compose.caramel.CaramelSettingsProvider

/**
 * The Java-facing entry point for the Caramel launcher UI.
 *
 * <p>A `@Composable` function cannot be called from Java: the compiler threads a `Composer`
 * parameter and a content lambda whose type Java cannot name. This host is the seam — the activity
 * builds the state and hands over plain callbacks, and everything Compose-shaped stays on the
 * Kotlin side. It mirrors the pack's `Main.kt` entry, minus the desktop `Window`.
 */
object CaramelLauncherHost {

    /** The whole launcher state, as a Kotlin [MutableState] so a set recomposes. */
    @JvmStatic
    fun state(): MutableState<CaramelLauncherState> = mutableStateOf(CaramelLauncherState())

    /** Everything the launcher can ask the activity to do. */
    interface Actions {
        fun launch()
        fun selectVersion(id: String)
        /** Opens the package picker and imports the chosen .apk/.xapk as a new instance. */
        fun importInstance()
        fun toggleMod(mod: CaramelModInfo, on: Boolean)
        fun equipCosmetic(cosmetic: CaramelCosmeticInfo)
        fun toggleFavourite(cosmetic: CaramelCosmeticInfo)
        fun joinServer(server: CaramelServerInfo)
        fun addServer()
        fun submitAddServer()
        fun resetAddServer()
        fun deleteServer(server: CaramelServerInfo)
        fun toggleServerFavourite(server: CaramelServerInfo)
        fun openModsFolder()
        fun openModsFolderEntry(entry: CaramelFileEntry)
        fun openModsFolderUp()
        fun openSettings()
        fun openAccounts()
        fun openScreenshots()
        fun openScreenshotsFolder()
        fun openScreenshot(screenshot: CaramelScreenshotInfo)
        fun deleteScreenshot(screenshot: CaramelScreenshotInfo)
        fun startSignIn()
        fun openDeviceCodePage()
        fun copyDeviceCode()
        fun signOut()
        fun searchFriend(query: String)
        fun sendFriendRequest()
        fun resetFriendSearch()
        /** Re-reads the signed-in account's Xbox Live friends, so a failed first read can be retried. */
        fun refreshFriends()
        fun renameInstance(name: String)
        fun repairInstance()
        fun deleteInstance()
        fun openInstanceFolder()
        /** Writes one performance feature's value into the selected instance's options file. */
        fun setPerfFeature(featureId: String, value: String)
        /** Resets every performance feature to its catalogue default. */
        fun resetPerfFeatures()
        /** Toggles the instance's MB shader-loader compatibility fixer. */
        fun setInstanceShaderCompat(enabled: Boolean)
        /** Toggles whether this instance keeps its own game data. */
        fun setInstanceIsolation(enabled: Boolean)
        /** Toggles portrait launch for this instance. */
        fun setInstanceLaunchVertical(enabled: Boolean)
        /** Toggles the in-game resource-pack changer for this instance. */
        fun setInstancePackChanger(enabled: Boolean)
        /**
         * Browses [classId] (0 for every section) with [query], publishing the results into the
         * state. An empty query is the tab's default listing, not an error.
         */
        fun searchCurseForge(query: String, classId: Int)
        /** Downloads [item]'s newest file and imports it into the selected instance. */
        fun downloadCurseForge(item: CaramelCurseForgeItem)
        fun quit()
    }

    /**
     * Mounts the launcher into [composeView], wrapped in the Caramel theme.
     *
     * <p>The skin preview slot defaults to the real software-rendered character (see
     * [CaramelSkinPreview]); a Kotlin caller may override [skinViewerProvider] to substitute its own.
     * A `@Composable` lambda cannot be passed from Java, so the activity opts in from Kotlin code
     * (or not at all, in which case the Cosmetics screen still shows the real render).
     */
    @JvmStatic
    fun setContent(
        composeView: ComposeView,
        state: MutableState<CaramelLauncherState>,
        actions: Actions,
        settings: CaramelSettingsProvider? = null,
    ) {
        composeView.setContent {
            CaramelTheme {
                CaramelLauncherScreen(
                    state = state.value,
                    actions = actions,
                    skinViewer = skinViewerProvider,
                    settings = settings,
                )
            }
        }
    }

    /** An override for the Cosmetics skin-preview slot; null means the real render is used. */
    @JvmStatic
    var skinViewerProvider: (@Composable (androidx.compose.ui.Modifier) -> Unit)? = null
}

/**
 * The Java-facing entry point for the cold-start splash.
 *
 * <p>Same seam as [CaramelLauncherHost]: a `@Composable` cannot be called from Java, so the activity
 * passes the plain `preparing` string and this mounts the screen.
 */
object CaramelSplashHost {
    /**
     * Mounts the cold-start show.
     *
     * <p>[art] is the ordered list of drawables this launch picked, [show] its timeline, and
     * [onFinished] is called once the show has run its length — the activity plays the hand-over pop
     * and opens the launcher there, so the screen is on the clock rather than the warm-up.
     * [onSkip] is the same hand-over, taken immediately: the show is twenty seconds of art and a
     * returning user should not have to sit through it.
     */
    @JvmStatic
    fun setContent(
        composeView: ComposeView,
        art: IntArray,
        show: SplashShow,
        preparing: String,
        onFinished: Runnable,
        onSkip: Runnable,
    ) {
        composeView.setContent {
            CaramelTheme {
                CaramelSplashScreen(
                    art = art.asList(),
                    show = show,
                    preparing = preparing,
                    onFinished = { onFinished.run() },
                    onSkip = { onSkip.run() },
                )
            }
        }
    }
}

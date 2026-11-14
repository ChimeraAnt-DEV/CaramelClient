package org.chimeramc.client.ui.compose.caramel.launch

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import org.chimeramc.client.ui.compose.caramel.CaramelTheme

/**
 * The Java-facing entry point for the Caramel launch screen.
 *
 * <p>A `@Composable` function cannot be called from Java: the compiler threads a `Composer`
 * parameter and a content lambda whose type Java cannot name. This host is the seam — the activity
 * builds the state and hands over plain callbacks, and everything Compose-shaped stays on the
 * Kotlin side.
 */
object CaramelLaunchHost {

    /** The mutable state the activity drives; a Kotlin [MutableState] so a set recomposes. */
    @JvmStatic
    fun state(initial: CaramelLaunchState): MutableState<CaramelLaunchState> = mutableStateOf(initial)

    /** The quick-toggle list's state. */
    @JvmStatic
    fun toggleState(initial: List<CaramelQuickToggle>): MutableState<List<CaramelQuickToggle>> =
        mutableStateOf(initial)

    /** A content-count state (worlds, packs, behaviors). */
    @JvmStatic
    fun countState(initial: Int): MutableState<Int> = mutableStateOf(initial)

    /** Everything the launch screen can ask the activity to do. */
    interface Actions {
        fun launch()
        fun selectVersion(id: String)
        fun openAccount()
        fun openMods()
        fun openCosmetics()
        fun openSettings()
        fun openContent()
        fun openAccounts()
        fun openCurseForge()
        fun openModHub()
        fun openQuickLaunch()
        fun toggle(id: String, on: Boolean)
        fun cycleBackground()
    }

    /** Mounts the launch screen into [composeView], wrapped in the Caramel theme. */
    @JvmStatic
    fun setContent(
        composeView: ComposeView,
        state: MutableState<CaramelLaunchState>,
        quickToggles: MutableState<List<CaramelQuickToggle>>,
        worlds: MutableState<Int>,
        packs: MutableState<Int>,
        behaviors: MutableState<Int>,
        onPreviewReady: java.util.function.Consumer<org.chimeramc.client.core.mods.inbuilt.overlay.CapePreviewView>,
        actions: Actions,
    ) {
        composeView.setContent {
            CaramelTheme {
                CaramelLaunchScreen(
                    state = state.value,
                    onLaunch = { actions.launch() },
                    onSelectVersion = { actions.selectVersion(it) },
                    onAccountClick = { actions.openAccount() },
                    onOpenMods = { actions.openMods() },
                    onOpenCosmetics = { actions.openCosmetics() },
                    onOpenSettings = { actions.openSettings() },
                    onOpenContent = { actions.openContent() },
                    onOpenAccounts = { actions.openAccounts() },
                    onOpenCurseForge = { actions.openCurseForge() },
                    onOpenModHub = { actions.openModHub() },
                    onOpenQuickLaunch = { actions.openQuickLaunch() },
                    contentWorlds = worlds.value,
                    contentPacks = packs.value,
                    contentBehaviors = behaviors.value,
                    quickToggles = quickToggles.value,
                    onToggle = { id, on -> actions.toggle(id, on) },
                    onPreviewReady = { onPreviewReady.accept(it) },
                    onCycleBackground = { actions.cycleBackground() },
                )
            }
        }
    }
}

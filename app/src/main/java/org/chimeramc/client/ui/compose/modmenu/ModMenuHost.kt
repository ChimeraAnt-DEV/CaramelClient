package org.chimeramc.client.ui.compose.modmenu

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import org.chimeramc.client.ui.compose.theme.GlowTheme

/**
 * The Java-facing entry point for the Compose Mod Menu.
 *
 * <p>The overlay (Java) owns the mod list and the persistence; it builds a [ModMenuState] and hosts
 * it here. Everything Compose-shaped stays in Kotlin, and every interaction comes back as a
 * callback, so the Java side keeps its existing `InbuiltModManager` wiring rather than being
 * rewritten to talk to Compose directly.
 */
object ModMenuHost {

    @JvmStatic
    fun state(initial: ModMenuState): MutableState<ModMenuState> = mutableStateOf(initial)

    @JvmStatic
    fun setContent(
        composeView: ComposeView,
        state: MutableState<ModMenuState>,
        onDestination: java.util.function.Consumer<ModMenuDestination>,
        onSearch: java.util.function.Consumer<String>,
        onToggleModule: ModuleToggleListener,
        onToggleFavorite: java.util.function.Consumer<String>,
        onToggleFavoritesOnly: Runnable,
        onToggleSetting: SettingToggleListener,
        onClose: Runnable,
    ) {
        composeView.setContent {
            GlowTheme {
                ModMenu(
                    state = state.value,
                    onDestination = { onDestination.accept(it) },
                    onSearch = { onSearch.accept(it) },
                    onToggleModule = { key, enabled -> onToggleModule.onToggle(key, enabled) },
                    onToggleFavorite = { onToggleFavorite.accept(it) },
                    onToggleFavoritesOnly = { onToggleFavoritesOnly.run() },
                    onToggleSetting = { key, enabled -> onToggleSetting.onToggle(key, enabled) },
                    onClose = { onClose.run() },
                )
            }
        }
    }

    /** A module toggle, as a Java functional interface so the overlay can pass a lambda. */
    fun interface ModuleToggleListener {
        fun onToggle(stableKey: String, enabled: Boolean)
    }

    /** A settings toggle, as a Java functional interface. */
    fun interface SettingToggleListener {
        fun onToggle(key: String, enabled: Boolean)
    }
}

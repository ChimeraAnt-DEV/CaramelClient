package org.chimeramc.client.ui.compose.launch

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import org.chimeramc.client.ui.compose.theme.GlowTheme

/**
 * The Java-facing entry point for the Compose quick-toggle row.
 *
 * <p>Same reason as [LaunchHeroHost]: a {@code @Composable} cannot be called from Java, so the
 * activity hands over the state and one callback and everything Compose-shaped stays in Kotlin.
 */
object LaunchTogglesHost {

    @JvmStatic
    fun state(initial: List<LaunchToggleState.Toggle>): MutableState<List<LaunchToggleState.Toggle>> =
        mutableStateOf(initial)

    @JvmStatic
    fun setContent(
        composeView: ComposeView,
        state: MutableState<List<LaunchToggleState.Toggle>>,
        onToggle: java.util.function.Consumer<LaunchToggleState.Kind>,
    ) {
        composeView.setContent {
            GlowTheme {
                LaunchToggles(
                    toggles = state.value,
                    onToggle = { onToggle.accept(it) },
                )
            }
        }
    }
}

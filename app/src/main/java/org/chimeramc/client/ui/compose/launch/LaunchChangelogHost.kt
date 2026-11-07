package org.chimeramc.client.ui.compose.launch

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import org.chimeramc.client.ui.compose.theme.GlowTheme

/**
 * The Java-facing entry point for the Compose changelog card and its popup.
 *
 * <p>Same pattern as [LaunchHeroHost]: a @Composable cannot be called from Java, so the activity
 * hands over two state values (the release notes and whether the popup is open) and everything
 * Compose-shaped stays in Kotlin.
 *
 * <p>The popup is a Compose Dialog, so it draws in its own window and overlays the whole tab even
 * though it is hosted from the card's ComposeView in the scroll column.
 */
object LaunchChangelogHost {

    @JvmStatic
    fun state(initial: LaunchChangelogState): MutableState<LaunchChangelogState> =
        mutableStateOf(initial)

    @JvmStatic
    fun openState(initial: Boolean): MutableState<Boolean> = mutableStateOf(initial)

    @JvmStatic
    fun setContent(
        composeView: ComposeView,
        state: MutableState<LaunchChangelogState>,
        open: MutableState<Boolean>,
    ) {
        composeView.setContent {
            GlowTheme {
                LaunchChangelogCard(
                    state = state.value,
                    onOpen = { open.value = true },
                )
                if (open.value) {
                    LaunchChangelogPopup(
                        state = state.value,
                        onDismiss = { open.value = false },
                    )
                }
            }
        }
    }
}

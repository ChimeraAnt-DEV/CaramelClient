package org.chimeramc.client.ui.compose.launch

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import org.chimeramc.client.core.mods.inbuilt.overlay.CapePreviewView
import org.chimeramc.client.ui.compose.theme.GlowTheme

/**
 * The Java-facing entry point for the Compose launch hero.
 *
 * <p>A {@code @Composable} function cannot be called from Java — the compiler threads a {@code
 * Composer} parameter and a content lambda whose type Java cannot name. This host is the seam: the
 * activity builds the state and hands over three plain callbacks, and everything Compose-shaped stays
 * on the Kotlin side.
 */
object LaunchHeroHost {

    /** The mutable state the activity drives; a Kotlin [MutableState] so a set recomposes. */
    @JvmStatic
    fun state(initial: LaunchHeroState): MutableState<LaunchHeroState> = mutableStateOf(initial)

    /** Mounts the hero into [composeView], wrapping it in the Caramel theme. */
    @JvmStatic
    fun setContent(
        composeView: ComposeView,
        state: MutableState<LaunchHeroState>,
        onPreviewClick: Runnable,
        onHeroClick: Runnable,
        onPreviewReady: java.util.function.Consumer<CapePreviewView>,
    ) {
        composeView.setContent {
            GlowTheme {
                LaunchHero(
                    state = state.value,
                    onPreviewClick = { onPreviewClick.run() },
                    onHeroClick = { onHeroClick.run() },
                    onPreviewReady = { onPreviewReady.accept(it) },
                )
            }
        }
    }
}

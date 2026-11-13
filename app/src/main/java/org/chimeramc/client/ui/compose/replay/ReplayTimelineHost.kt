package org.chimeramc.client.ui.compose.replay

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import org.chimeramc.client.core.replay.ReplayTimelineData
import org.chimeramc.client.ui.compose.theme.GlowTheme

/**
 * The Java-facing entry point for the Compose cinematic timeline strip.
 *
 * <p>Same reason as the launch hosts: a {@code @Composable} cannot be called from Java, so the
 * cinematic dialog hands over an immutable [ReplayTimelineData] snapshot and one callback and
 * everything Compose-shaped stays on the Kotlin side. The dialog rebuilds the snapshot whenever it
 * adds a keyframe, so the strip redraws.
 */
object ReplayTimelineHost {

    /** The state the dialog drives: the timeline snapshot to draw. */
    @JvmStatic
    fun state(initial: ReplayTimelineData): MutableState<ReplayTimelineData> =
        mutableStateOf(initial)

    /** Mounts the strip into [composeView], wrapping it in the Caramel theme. */
    @JvmStatic
    fun setContent(
        composeView: ComposeView,
        state: MutableState<ReplayTimelineData>,
        onScrub: java.util.function.LongConsumer,
    ) {
        composeView.setContent {
            GlowTheme {
                ReplayTimelineStrip(
                    data = state.value,
                    onScrub = { ms -> onScrub.accept(ms) },
                )
            }
        }
    }
}


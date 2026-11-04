package org.chimeramc.client.ui.compose.launch

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import org.chimeramc.client.core.mods.inbuilt.overlay.CapePreviewView
import org.chimeramc.client.ui.compose.theme.GlowTheme

/**
 * The Java-facing entry point for the Compose preview stage and its background picker.
 *
 * <p>A {@code @Composable} cannot be called from Java, so the activity hands over the state and the
 * callbacks and everything Compose-shaped stays in Kotlin.
 */
object LaunchPreviewHost {

    /** The stage's state: which background is behind the character. */
    @JvmStatic
    fun state(backgroundId: String): MutableState<String> = mutableStateOf(backgroundId)

    /** Mounts the preview stage. */
    @JvmStatic
    fun setStageContent(
        composeView: ComposeView,
        state: MutableState<String>,
        previewDescription: String,
        onLongPress: Runnable,
        onPreviewReady: java.util.function.Consumer<CapePreviewView>,
    ) {
        composeView.setContent {
            GlowTheme {
                LaunchPreviewStage(
                    backgroundRes = PreviewBackground.resolve(state.value).drawableRes,
                    previewDescription = previewDescription,
                    onLongPress = { onLongPress.run() },
                    onPreviewReady = { onPreviewReady.accept(it) },
                )
            }
        }
    }

    /** Mounts the background picker, which reports the chosen id. */
    @JvmStatic
    fun setPickerContent(
        composeView: ComposeView,
        state: MutableState<String>,
        onSelected: java.util.function.Consumer<String>,
    ) {
        composeView.setContent {
            GlowTheme {
                LaunchBackgroundPicker(
                    selectedId = state.value,
                    onSelect = { id ->
                        state.value = id
                        onSelected.accept(id)
                    },
                )
            }
        }
    }
}

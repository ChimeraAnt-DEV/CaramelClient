package org.chimeramc.client.ui.compose.launch

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.chimeramc.client.ui.compose.theme.GlassSurface
import org.chimeramc.client.ui.compose.theme.GlowDimens
import org.chimeramc.client.ui.compose.theme.glowColors

/**
 * The launch tab's preview stage: the character, centred, on the chosen paint-splat background.
 *
 * <p>The background is a small image shown behind the character at a size that keeps its own
 * aspect, so the splat reads as paint rather than a stretched rectangle. A long-press opens the
 * picker (the caller shows it); a plain tap is left to the character preview's own handler.
 *
 * <p>The character itself is the real [org.chimeramc.client.core.mods.inbuilt.overlay.CapePreviewView]
 * hosted in an [AndroidView], so the equipped cape, hat and pet render exactly as they do in game.
 * The Compose layer only places it, sizes it and releases it when the composition leaves.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LaunchPreviewStage(
    backgroundRes: Int,
    previewDescription: String,
    onLongPress: () -> Unit,
    onPreviewReady: (org.chimeramc.client.core.mods.inbuilt.overlay.CapePreviewView) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongPress),
        contentAlignment = Alignment.Center,
    ) {
        // The splat, kept at its own aspect so it is not stretched into a rectangle.
        Image(
            painter = painterResource(backgroundRes),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .aspectRatio(228f / 128f),
        )
        // The character, standing on the splat.
        AndroidView(
            modifier = Modifier
                .width(210.dp)
                .height(260.dp),
            factory = { ctx ->
                org.chimeramc.client.core.mods.inbuilt.overlay.CapePreviewView(ctx).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    setAlwaysAnimate(true)
                    contentDescription = previewDescription
                    onPreviewReady(this)
                }
            },
            onRelease = { view ->
                try {
                    view.stopPreview()
                    view.release()
                } catch (t: Throwable) {
                    org.chimeramc.client.util.AppLog.warnOnce(
                        "CaramelClient", "LaunchPreviewStage:release",
                        "Ignored a failure releasing the character preview", t,
                    )
                }
            },
        )
    }
}

/**
 * The background picker: one tile per background, each showing the image itself, so the choice is
 * made by sight rather than by name.
 */
@Composable
fun LaunchBackgroundPicker(
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = glowColors
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(GlowDimens.CARD_RADIUS.dp),
    ) {
        Column(modifier = Modifier.padding(GlowDimens.CARD_PADDING.dp)) {
            Text(
                text = "Background",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = GlowDimens.SPACE_3.dp),
                horizontalArrangement = Arrangement.spacedBy(GlowDimens.SPACE_3.dp),
            ) {
                for (entry in PreviewBackground.all()) {
                    val selected = entry.id == selectedId
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(228f / 128f)
                            .clip(RoundedCornerShape(GlowDimens.RADIUS_MEDIUM.dp))
                            .combinedClickable(
                                onClick = { onSelect(entry.id) },
                                onLongClick = {},
                            ),
                    ) {
                        Image(
                            painter = painterResource(entry.drawableRes),
                            contentDescription = entry.label,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (selected) {
                            // A selected tile gets a solid ring in the accent, so the current
                            // choice is unmistakable at a glance.
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .border(
                                        width = 2.dp,
                                        color = colors.snowBlue,
                                        shape = RoundedCornerShape(GlowDimens.RADIUS_MEDIUM.dp),
                                    ),
                            )
                        }
                    }
                }
            }
        }
    }
}

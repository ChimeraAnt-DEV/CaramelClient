package org.chimeramc.client.ui.compose.launch

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.chimeramc.client.R
import org.chimeramc.client.ui.compose.theme.GlassSurface
import org.chimeramc.client.ui.compose.theme.GlowDimens
import org.chimeramc.client.ui.compose.theme.glowColors
import org.chimeramc.client.ui.compose.theme.glowMotion

/**
 * The Launch tab's changelog card: the release the player is on, with the count of what changed.
 *
 * <p>It floats when idle so the eye finds it without a banner; the motion is a slow vertical bob
 * rather than a pulse, and it stops under reduced motion. Tapping it opens the full popup.
 *
 * <p>Hidden entirely when there is nothing to show, so a build with no release notes does not open a
 * card that leads to an empty screen.
 */
@Composable
fun LaunchChangelogCard(
    state: LaunchChangelogState,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isEmpty) return
    val colors = glowColors
    val motion = glowMotion
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = tween(if (motion.reduced) 0 else motion.fastMs),
        label = "changelogCardPress",
    )
    // The idle float: a slow, small rise and fall. Reduced motion holds it still rather than
    // animating faster, so the card is never the reason a screen cannot be read.
    val float = if (motion.reduced) {
        0f
    } else {
        val transition = rememberInfiniteTransition(label = "changelogFloat")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2600, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "changelogFloatValue",
        )
        // -3dp .. +3dp, so it drifts rather than bounces.
        (value - 0.5f) * 6f
    }

    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .scale(pressScale)
            .padding(top = float.dp)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onOpen,
            ),
        shape = RoundedCornerShape(GlowDimens.CARD_RADIUS.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GlowDimens.CARD_PADDING.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GlowDimens.SPACE_3.dp),
        ) {
            Badge(icon = Icons.Filled.NewReleases, tint = colors.snowBlue)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.changelog_card_title, state.version),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(GlowDimens.SPACE_HAIRLINE.dp))
                Text(
                    text = state.headline.ifEmpty { stringResource(R.string.changelog_card_subtitle) },
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(GlowDimens.SPACE_2.dp))
                CountRow(state = state)
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = stringResource(R.string.changelog_card_action),
                tint = colors.textSecondary,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** The three counts, so the card says how big the release is without opening it. */
@Composable
private fun CountRow(state: LaunchChangelogState) {
    val colors = glowColors
    Row(horizontalArrangement = Arrangement.spacedBy(GlowDimens.SPACE_2.dp)) {
        CountPill(stringResource(R.string.changelog_popup_added), state.added.size, colors.success)
        CountPill(stringResource(R.string.changelog_popup_changed), state.changed.size, colors.snowBlue)
        CountPill(stringResource(R.string.changelog_popup_fixed), state.fixed.size, colors.warning)
    }
}

@Composable
private fun CountPill(label: String, count: Int, tint: Color) {
    val colors = glowColors
    Surface(
        shape = RoundedCornerShape(GlowDimens.RADIUS_TAG.dp),
        color = tint.copy(alpha = 0.16f),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = GlowDimens.SPACE_2.dp,
                vertical = GlowDimens.SPACE_HAIRLINE.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GlowDimens.SPACE_1.dp),
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = tint,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                maxLines = 1,
            )
        }
    }
}

/** A round icon plate, so a leading glyph reads as a badge rather than a stray icon. */
@Composable
private fun Badge(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(GlowDimens.RADIUS_MEDIUM.dp))
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

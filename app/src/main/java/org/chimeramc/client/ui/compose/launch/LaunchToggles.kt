package org.chimeramc.client.ui.compose.launch

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import org.chimeramc.client.R
import org.chimeramc.client.ui.compose.theme.GlowDimens
import org.chimeramc.client.ui.compose.theme.glowColors

/**
 * The Launch tab's quick-toggle row: AntEgg, Voice and Replays, as navy cards with a filled icon, a
 * label and a live status line.
 *
 * <p>The cards and their statuses come from [LaunchToggleState], so the row is a pure function of the
 * data and guest mode is a data rule rather than a visibility hack. Each card is one tap target with
 * a press-scaled surface, matching the rest of the tab.
 */
@Composable
fun LaunchToggles(
    toggles: List<LaunchToggleState.Toggle>,
    onToggle: (LaunchToggleState.Kind) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GlowDimens.SPACE_3.dp),
    ) {
        for (toggle in toggles) {
            ToggleCard(
                toggle = toggle,
                onClick = { onToggle(toggle.kind) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ToggleCard(
    toggle: LaunchToggleState.Toggle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = glowColors
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(GlowDimens.CARD_RADIUS.dp),
        color = colors.surface,
        border = BorderStroke(GlowDimens.CARD_BORDER_WIDTH.dp, colors.stroke),
    ) {
        Column(modifier = Modifier.padding(GlowDimens.CARD_PADDING.dp)) {
            Icon(
                imageVector = iconFor(toggle.kind),
                contentDescription = null,
                tint = tintFor(toggle.kind),
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.height(GlowDimens.SPACE_2.dp))
            Text(
                text = toggle.label,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(GlowDimens.SPACE_HAIRLINE.dp))
            Text(
                text = toggle.status,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** A filled icon per card: a plugin for packs, a mic for voice, a camera for replays. */
private fun iconFor(kind: LaunchToggleState.Kind): ImageVector = when (kind) {
    LaunchToggleState.Kind.ANTEGG -> Icons.Filled.Extension
    LaunchToggleState.Kind.VOICE -> Icons.Filled.Mic
    LaunchToggleState.Kind.REPLAY -> Icons.Filled.Videocam
}

/** A per-card accent from the palette, so the row is colourful but still on the navy theme. */
@Composable
private fun tintFor(kind: LaunchToggleState.Kind): Color {
    val colors = glowColors
    return when (kind) {
        LaunchToggleState.Kind.ANTEGG -> colors.snowBlueDeep
        LaunchToggleState.Kind.VOICE -> colors.snowBlue
        LaunchToggleState.Kind.REPLAY -> colors.success
    }
}

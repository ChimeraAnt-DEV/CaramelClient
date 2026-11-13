package org.chimeramc.client.ui.compose.caramel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A small tinted pill: a fill at 16% of the tone, a border at 50%, and the label in the tone.
 *
 * The tint does the talking, which is why the caller passes a colour rather than a severity enum:
 * the same pill is used for a version channel, a profile mode and a version range, and each has its
 * own tone that does not map onto a shared severity scale.
 */
@Composable
fun CaramelStatusLabel(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .height(20.dp)
            .clip(shape)
            .background(color.copy(0.16f))
            .border(1.dp, color.copy(0.5f), shape)
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(11.dp), color)
        Text(text, style = CaramelType.Chip.copy(color = color))
    }
}

/** A selectable filter pill: a blue gradient when selected, a quiet fill when not. */
@Composable
fun CaramelFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .height(26.dp)
            .clip(shape)
            .background(
                if (selected) {
                    Brush.verticalGradient(listOf(CaramelColors.BlueLight, CaramelColors.BlueDark))
                } else {
                    SolidColor(Color.White.copy(0.07f))
                },
            )
            .border(1.dp, if (selected) Color.Transparent else Color.White.copy(0.16f), shape)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = 13.dp),
        Alignment.Center,
    ) {
        Text(
            text,
            style = CaramelType.Chip.copy(
                fontSize = 11.5.sp,
                color = if (selected) Color.White else CaramelColors.Text2,
            ),
        )
    }
}

/**
 * A segmented control.
 *
 * Each segment is a fixed width, so the control does not resize as the selection moves and the
 * highlight cannot appear to jump. A caller with more than a few segments wants a scrollable row of
 * chips instead.
 */
@Composable
fun CaramelSegmentedTabs(
    items: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    segmentWidth: Int = 96,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .height(36.dp)
            .clip(shape)
            .background(Color.White.copy(0.06f))
            .border(1.dp, Color.White.copy(0.14f), shape)
            .padding(3.dp),
    ) {
        items.forEachIndexed { index, label ->
            Box(
                Modifier
                    .width(segmentWidth.dp)
                    .fillMaxHeight()
                    .clip(shape)
                    .background(
                        if (index == selected) {
                            Brush.verticalGradient(
                                listOf(CaramelColors.BlueLight, CaramelColors.BlueDark),
                            )
                        } else {
                            SolidColor(Color.Transparent)
                        },
                    )
                    .clickable(remember { MutableInteractionSource() }, null) { onSelect(index) },
                Alignment.Center,
            ) {
                Text(
                    label.uppercase(),
                    style = CaramelType.Chip.copy(
                        letterSpacing = 1.sp,
                        color = if (index == selected) Color.White else CaramelColors.Text2,
                    ),
                )
            }
        }
    }
}

/**
 * A placeholder avatar ring.
 *
 * A slot, not a picture: the Microsoft profile image is supplied by the caller, so this draws the
 * ring and the fill and leaves the middle empty. A green ring means the account is signed in.
 */
@Composable
fun CaramelAvatar(size: Int = 36, online: Boolean = false) {
    Box(
        Modifier
            .size(size.dp)
            .background(Color.White.copy(0.09f), CircleShape)
            .border(
                if (online) 2.dp else 1.dp,
                if (online) CaramelColors.Green else Color.White.copy(0.3f),
                CircleShape,
            ),
    )
}

/**
 * A server icon slot.
 *
 * The inner square is where the server's own icon goes; until the caller supplies one this is a
 * tinted rounded square with a solid core, which reads as an icon without pretending to be a
 * particular server.
 */
@Composable
fun CaramelServerIcon(color: Color, size: Int = 26) {
    val shape = RoundedCornerShape((size * 0.3f).dp)
    Box(
        Modifier
            .size(size.dp)
            .clip(shape)
            .background(color.copy(0.22f))
            .border(1.dp, color.copy(0.6f), shape),
        Alignment.Center,
    ) {
        Box(Modifier.size((size * 0.4f).dp).background(color, RoundedCornerShape(2.dp)))
    }
}

/** The supported-version range of a server, e.g. `1.21.132 - 26.62`. */
@Composable
fun CaramelVersionRangeLabel(min: String, max: String, modifier: Modifier = Modifier) =
    CaramelStatusLabel("$min - $max", Color(0xFF78A0FF), modifier)

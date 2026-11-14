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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
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

/**
 * The Caramel label and chip set, ported from the pack's `components/Labels.kt`.
 *
 * <p>Labels carry status (Release/Beta, Locked/Unlocked, Equipped, a server's version range). The
 * pack's rule is one tinted pill per status with a matching hairline, so a status reads the same
 * wherever it appears. Keeping the rule in one function is what stops a "Beta" pill drifting to a
 * different orange in one screen.
 */
@Composable
fun StatusLabel(
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
        Text(text, style = T.Chip.copy(color = color))
    }
}

/** A "Release" (green) or "Beta" (orange) pill. */
@Composable
fun ChannelLabel(label: String, color: Color, modifier: Modifier = Modifier) = StatusLabel(label, color, modifier)

/** An "Unlocked" (green, open padlock) or "Locked" (red, closed padlock) pill. */
@Composable
fun ProfileLabel(locked: Boolean, modifier: Modifier = Modifier) = StatusLabel(
    if (locked) "Locked" else "Unlocked",
    if (locked) C.Red else C.Green,
    modifier,
    if (locked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
)

/** A selectable filter chip; the selected one is a blue gradient. */
@Composable
fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .height(26.dp)
            .clip(shape)
            .background(
                if (selected) {
                    Brush.verticalGradient(listOf(C.BlueLight, C.BlueDark))
                } else {
                    SolidColor(Color.White.copy(0.07f))
                },
            )
            .border(1.dp, if (selected) Color.Transparent else Color.White.copy(0.16f), shape)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = 13.dp),
        Alignment.Center,
    ) {
        Text(text, style = T.Chip.copy(fontSize = 11.5.sp_, color = if (selected) Color.White else C.Text2))
    }
}

/** The pack's `SegmentedTabs`: a pill row with a sliding blue selection. */
@Composable
fun SegmentedTabs(
    items: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
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
        items.forEachIndexed { i, t ->
            Box(
                Modifier
                    .width(96.dp)
                    .fillMaxHeight()
                    .clip(shape)
                    .background(
                        if (i == selected) {
                            Brush.verticalGradient(listOf(C.BlueLight, C.BlueDark))
                        } else {
                            SolidColor(Color.Transparent)
                        },
                    )
                    .clickable(remember { MutableInteractionSource() }, null) { onSelect(i) },
                Alignment.Center,
            ) {
                Text(
                    t.uppercase(),
                    style = T.Chip.copy(
                        letterSpacing = androidx.compose.ui.unit.TextUnit(1f, androidx.compose.ui.unit.TextUnitType.Sp),
                        color = if (i == selected) Color.White else C.Text2,
                    ),
                )
            }
        }
    }
}

/** The account avatar slot. The profile picture is drawn by the caller over this. */
@Composable
fun CaramelAvatar(size: Int = 36, online: Boolean = false, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size.dp)
            .background(Color.White.copy(0.09f), CircleShape)
            .border(if (online) 2.dp else 1.dp, if (online) C.Green else Color.White.copy(0.3f), CircleShape),
    )
}

/** The server-icon slot used by Quick play and the Servers list. */
@Composable
fun ServerIcon(color: Color, size: Int = 26, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape((size * 0.3f).dp)
    Box(
        modifier
            .size(size.dp)
            .clip(shape)
            .background(color.copy(0.22f))
            .border(1.dp, color.copy(0.6f), shape),
        Alignment.Center,
    ) {
        Box(Modifier.size((size * 0.4f).dp).background(color, RoundedCornerShape(2.dp)))
    }
}

/** A "1.21.132 – 26.62" supported-version label for a server. */
@Composable
fun VersionRangeLabel(min: String, max: String, modifier: Modifier = Modifier) =
    StatusLabel("$min – $max", Color(0xFF78A0FF), modifier)

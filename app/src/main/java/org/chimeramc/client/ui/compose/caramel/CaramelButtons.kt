package org.chimeramc.client.ui.compose.caramel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** The four button fills. A tone carries its own label colour so a caller cannot pick a mismatched pair. */
enum class CaramelTone(val top: Color, val bottom: Color, val on: Color) {
    Green(CaramelColors.GreenLight, CaramelColors.GreenDark, CaramelColors.OnGreen),
    Caramel(Color(0xFFFFBE5A), CaramelColors.CaramelDark, Color.White),
    Blue(CaramelColors.BlueLight, CaramelColors.BlueDark, Color.White),
    Red(Color(0xFFFF6C7E), Color(0xFFD6344E), Color.White),
}

/**
 * The filled action button: a vertical gradient, a top sheen, a lit border, and a drop shadow in the
 * tone's own colour so the button appears to glow.
 *
 * The pack drives the hover lift from `collectIsHoveredAsState`. A phone has no hover, so the lift is
 * driven by the press instead: same shadow and sheen ramp, triggered by the gesture the platform
 * actually has.
 */
@Composable
fun CaramelPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: CaramelTone = CaramelTone.Green,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Int = 44,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val shape = RoundedCornerShape((height / 2.6).dp)
    val lit = pressed && enabled
    Box(
        modifier
            .height(height.dp)
            .shadow(if (lit) 16.dp else 10.dp, shape, ambientColor = tone.bottom, spotColor = tone.bottom)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(tone.top, tone.bottom)))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(if (lit) 0.28f else 0.20f),
                        Color.Transparent,
                        Color.Transparent,
                    ),
                ),
            )
            .border(1.dp, Color.White.copy(0.28f), shape)
            .clickable(source, null, enabled = enabled, onClick = onClick)
            .then(if (enabled) Modifier else Modifier.alpha(0.45f)),
        Alignment.Center,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (icon != null) Icon(icon, null, Modifier.size(18.dp), tone.on)
            Text(text, style = CaramelType.Button.copy(color = tone.on))
        }
    }
}

/**
 * The quiet button: a translucent fill and a border, with no shadow.
 *
 * Used for everything that is not the one primary action on a screen, which is what keeps the
 * hierarchy readable when a row has several controls.
 */
@Composable
fun CaramelGlassButton(
    text: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    danger: Boolean = false,
    active: Boolean = false,
    enabled: Boolean = true,
    height: Int = 40,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val tint = when {
        danger -> CaramelColors.Red
        active -> CaramelColors.Blue
        else -> Color.White
    }
    val shape = RoundedCornerShape(12.dp)
    val foreground = if (danger) CaramelColors.Red else CaramelColors.Text
    val fillAlpha = when {
        danger || active -> 0.16f
        pressed -> 0.14f
        else -> 0.08f
    }
    val borderAlpha = when {
        danger || active -> 0.45f
        pressed -> 0.28f
        else -> 0.17f
    }
    Box(
        modifier
            .height(height.dp)
            .then(if (text == null) Modifier.width(height.dp) else Modifier)
            .clip(shape)
            .background(tint.copy(fillAlpha))
            .border(1.dp, tint.copy(borderAlpha), shape)
            .clickable(source, null, enabled = enabled, onClick = onClick)
            .then(if (enabled) Modifier else Modifier.alpha(0.45f)),
        Alignment.Center,
    ) {
        Row(
            Modifier.padding(horizontal = if (text == null) 0.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (icon != null) Icon(icon, null, Modifier.size(17.dp), foreground)
            if (text != null) Text(text, style = CaramelType.Label.copy(color = foreground))
        }
    }
}

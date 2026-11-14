package org.chimeramc.client.ui.compose.caramel

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The Caramel control set, ported from the pack's `components/Buttons.kt` and `Controls.kt`.
 *
 * <p>The pack styles its buttons on hover, which a touch screen never does. `collectIsHoveredAsState`
 * is replaced by `collectIsPressedAsState`, so the same brightening happens under a finger instead
 * of a cursor; everything else (the tone ramps, the shadow, the top-edge highlight) is the pack's.
 */

/** The pack's `Tone` ramps: a vertical gradient plus the colour text sits on. */
enum class Tone(val top: Color, val bottom: Color, val on: Color) {
    Green(C.GreenLight, C.GreenDark, C.OnGreen),
    Caramel(Color(0xFFFFBE5A), C.CaramelDark, Color.White),
    Blue(C.BlueLight, C.BlueDark, Color.White),
    Red(Color(0xFFFF6C7E), Color(0xFFD6344E), Color.White),
}

/** The pack's `PrimaryButton`: a filled, gradient, glowing action. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: Tone = Tone.Green,
    icon: ImageVector? = null,
    height: Int = 44,
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val shape = RoundedCornerShape((height / 2.6).dp)
    Box(
        modifier
            .height(height.dp)
            .shadow(if (pressed) 16.dp else 10.dp, shape, ambientColor = tone.bottom, spotColor = tone.bottom)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(tone.top, tone.bottom)))
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(if (pressed) 0.28f else 0.20f), Color.Transparent, Color.Transparent),
                ),
            )
            .border(1.dp, Color.White.copy(0.28f), shape)
            .clickable(src, null, onClick = onClick),
        Alignment.Center,
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (icon != null) Icon(icon, null, Modifier.size(18.dp), tone.on)
            Text(text, style = T.Button.copy(color = tone.on))
        }
    }
}

/** The pack's `GlassButton`: a translucent secondary action. `text == null` makes it square. */
@Composable
fun GlassButton(
    text: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    danger: Boolean = false,
    active: Boolean = false,
    height: Int = 40,
) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val tint = when {
        danger -> C.Red
        active -> C.Blue
        else -> Color.White
    }
    val shape = RoundedCornerShape(CaramelDimens.RadiusButton)
    val fg = if (danger) C.Red else C.Text
    Box(
        modifier
            .height(height.dp)
            .then(if (text == null) Modifier.width(height.dp) else Modifier)
            .clip(shape)
            .background(tint.copy(if (danger || active) 0.16f else if (pressed) 0.14f else 0.08f))
            .border(1.dp, tint.copy(if (danger || active) 0.45f else if (pressed) 0.28f else 0.17f), shape)
            .clickable(src, null, onClick = onClick),
        Alignment.Center,
    ) {
        Row(
            Modifier.padding(horizontal = if (text == null) 0.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (icon != null) Icon(icon, null, Modifier.size(17.dp), fg)
            if (text != null) Text(text, style = T.Label.copy(color = fg))
        }
    }
}

/** The pack's `CaramelToggle`: a gradient track with a sliding thumb. */
@Composable
fun CaramelToggle(checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val x by animateDpAsState(if (checked) 21.dp else 3.dp, spring(stiffness = 600f))
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .size(40.dp, 22.dp)
            .clip(shape)
            .background(
                if (checked) {
                    Brush.horizontalGradient(listOf(C.GreenLight, C.GreenDark))
                } else {
                    SolidColor(Color.White.copy(0.10f))
                },
            )
            .border(1.dp, if (checked) Color.Transparent else Color.White.copy(0.24f), shape)
            .clickable(remember { MutableInteractionSource() }, null) { onChange(!checked) },
    ) {
        Box(
            Modifier
                .offset(x, 3.dp)
                .size(16.dp)
                .shadow(3.dp, CircleShape)
                .background(if (checked) Color.White else Color(0xFFBECAEB), CircleShape),
        )
    }
}

/** The pack's `CaramelCheckbox`. */
@Composable
fun CaramelCheckbox(checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(5.dp)
    Box(
        modifier
            .size(18.dp)
            .clip(shape)
            .background(
                if (checked) {
                    Brush.verticalGradient(listOf(C.BlueLight, C.BlueDark))
                } else {
                    SolidColor(Color.White.copy(0.06f))
                },
            )
            .border(1.dp, if (checked) Color.Transparent else Color.White.copy(0.28f), shape)
            .clickable(remember { MutableInteractionSource() }, null) { onChange(!checked) },
        Alignment.Center,
    ) {
        if (checked) Icon(Icons.Rounded.Check, null, Modifier.size(13.dp), Color.White)
    }
}

/** The pack's `CaramelSlider`: a gradient track and a white knob with an accent core. */
@Composable
fun CaramelSlider(
    value: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = C.Blue,
    accentEnd: Color = Color(0xFF4ABEFF),
) {
    var w by remember { mutableStateOf(1) }
    Canvas(
        modifier
            .fillMaxWidth()
            .height(22.dp)
            .onSizeChanged { w = it.width.coerceAtLeast(1) }
            .pointerInput(Unit) {
                detectTapGestures { onChange((it.x / w).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectDragGestures { c, _ -> onChange((c.position.x / w).coerceIn(0f, 1f)) }
            },
    ) {
        val cy = size.height / 2
        val th = 6.dp.toPx()
        val kx = size.width * value
        drawRoundRect(Color.White.copy(0.12f), Offset(0f, cy - th / 2), Size(size.width, th), CornerRadius(th))
        drawRoundRect(Brush.horizontalGradient(listOf(accent, accentEnd)), Offset(0f, cy - th / 2), Size(kx, th), CornerRadius(th))
        drawCircle(Color.Black.copy(0.35f), 9.dp.toPx(), Offset(kx, cy + 1.5.dp.toPx()))
        drawCircle(Color.White, 8.dp.toPx(), Offset(kx, cy))
        drawCircle(accent, 3.dp.toPx(), Offset(kx, cy))
    }
}

/** The pack's `CaramelTextField`, with a search leading icon for [SearchField]. */
@Composable
fun CaramelTextField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leading: ImageVector? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val border by animateColorAsState(if (focused) C.Blue.copy(0.8f) else Color.White.copy(0.16f))
    val shape = RoundedCornerShape(CaramelDimens.RadiusControl)
    BasicTextField(
        value,
        onChange,
        modifier
            .height(38.dp)
            .clip(shape)
            .background(Color(0x6E040A1A))
            .border(1.dp, border, shape)
            .onFocusChangedCompat { focused = it },
        singleLine = true,
        textStyle = T.Label.copy(fontWeight = FontWeight.Normal),
        cursorBrush = SolidColor(C.Blue),
        decorationBox = { inner ->
            Row(
                Modifier
                    .padding(horizontal = 14.dp)
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (leading != null) Icon(leading, null, Modifier.size(16.dp), C.Text3)
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, style = T.Label.copy(color = C.Text3, fontWeight = FontWeight.Normal))
                    inner()
                }
            }
        },
    )
}

/** A search box, the pack's `SearchField`. */
@Composable
fun SearchField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) = CaramelTextField(value, onChange, modifier, placeholder, Icons.Rounded.Search)

/** A title, an optional description and a trailing control, from the pack's `SettingRow`. */
@Composable
fun SettingRow(
    title: String,
    desc: String? = null,
    modifier: Modifier = Modifier,
    control: @Composable () -> Unit,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = T.Label)
            if (desc != null) Text(desc, style = T.Body)
        }
        control()
    }
}

/** A labelled slider whose value is shown in the accent colour, from the pack's `SliderRow`. */
@Composable
fun SliderRow(
    title: String,
    valueText: String,
    value: Float,
    onChange: (Float) -> Unit,
    accent: Color = C.Blue,
    accentEnd: Color = Color(0xFF4ABEFF),
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text(title, style = T.Label, modifier = Modifier.weight(1f))
            Text(valueText, style = T.Label.copy(color = accent))
        }
        CaramelSlider(value, onChange, accent = accent, accentEnd = accentEnd)
    }
}

/** Local sp helpers, so call sites can write `12.sp_` next to the pack's own literals. */
val Int.sp_: androidx.compose.ui.unit.TextUnit
    get() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)

val Double.sp_: androidx.compose.ui.unit.TextUnit
    get() = androidx.compose.ui.unit.TextUnit(this.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp)

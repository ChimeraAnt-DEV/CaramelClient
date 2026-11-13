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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
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

/** A switch: a gradient track when on, a hairline outline when off, and a spring-driven knob. */
@Composable
fun CaramelToggle(
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val knobOffset by animateDpAsState(if (checked) 21.dp else 3.dp, spring(stiffness = 600f))
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .size(40.dp, 22.dp)
            .clip(shape)
            .background(
                if (checked) {
                    Brush.horizontalGradient(listOf(CaramelColors.GreenLight, CaramelColors.GreenDark))
                } else {
                    SolidColor(Color.White.copy(0.10f))
                },
            )
            .border(1.dp, if (checked) Color.Transparent else Color.White.copy(0.24f), shape)
            .clickable(remember { MutableInteractionSource() }, null, enabled = enabled) { onChange(!checked) },
    ) {
        Box(
            Modifier
                .offset(knobOffset, 3.dp)
                .size(16.dp)
                .shadow(3.dp, CircleShape)
                .background(if (checked) Color.White else Color(0xFFBECAEB), CircleShape),
        )
    }
}

/** A checkbox: a blue gradient fill and a tick when checked. */
@Composable
fun CaramelCheckbox(checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(5.dp)
    Box(
        modifier
            .size(18.dp)
            .clip(shape)
            .background(
                if (checked) {
                    Brush.verticalGradient(listOf(CaramelColors.BlueLight, CaramelColors.BlueDark))
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

/** A radio: an outlined circle with an accent core when selected. */
@Composable
fun CaramelRadio(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(Color.White.copy(0.06f))
            .border(
                1.4.dp,
                if (selected) CaramelColors.Blue else Color.White.copy(0.28f),
                CircleShape,
            )
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
        Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(8.dp).background(CaramelColors.Blue, CircleShape))
    }
}

/**
 * A slider with a gradient track and a knob carrying an accent core.
 *
 * Drawn on a [Canvas] rather than built from Material's slider because the track is a two-colour
 * gradient and the knob is three concentric circles; reproducing that through the Material API would
 * mean overriding every slot. The two gesture detectors are separate so a tap jumps the knob and a
 * drag tracks it — one detector cannot do both without lagging a jump.
 */
@Composable
fun CaramelSlider(
    value: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = CaramelColors.Blue,
    accentEnd: Color = Color(0xFF4ABEFF),
) {
    var trackWidth by remember { mutableIntStateOf(1) }
    Canvas(
        modifier
            .fillMaxWidth()
            .height(22.dp)
            .onSizeChanged { trackWidth = it.width.coerceAtLeast(1) }
            .pointerInput(Unit) {
                detectTapGestures { onChange((it.x / trackWidth).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    onChange((change.position.x / trackWidth).coerceIn(0f, 1f))
                }
            },
    ) {
        val centreY = size.height / 2
        val thickness = 6.dp.toPx()
        val knobX = size.width * value.coerceIn(0f, 1f)
        drawRoundRect(
            Color.White.copy(0.12f),
            Offset(0f, centreY - thickness / 2),
            Size(size.width, thickness),
            CornerRadius(thickness),
        )
        drawRoundRect(
            Brush.horizontalGradient(listOf(accent, accentEnd)),
            Offset(0f, centreY - thickness / 2),
            Size(knobX, thickness),
            CornerRadius(thickness),
        )
        drawCircle(Color.Black.copy(0.35f), 9.dp.toPx(), Offset(knobX, centreY + 1.5.dp.toPx()))
        drawCircle(Color.White, 8.dp.toPx(), Offset(knobX, centreY))
        drawCircle(accent, 3.dp.toPx(), Offset(knobX, centreY))
    }
}

/** A single-line field: a dark translucent fill and a border that lights up on focus. */
@Composable
fun CaramelTextField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leading: ImageVector? = null,
) {
    var focused by remember { mutableStateOf(false) }
    val border by animateColorAsState(
        if (focused) CaramelColors.Blue.copy(0.8f) else Color.White.copy(0.16f),
    )
    val shape = RoundedCornerShape(11.dp)
    BasicTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier
            .height(38.dp)
            .clip(shape)
            .background(Color(0x6E040A1A))
            .border(1.dp, border, shape)
            .onFocusChangedCompat { focused = it },
        singleLine = true,
        textStyle = CaramelType.Label.copy(fontWeight = FontWeight.Normal),
        cursorBrush = SolidColor(CaramelColors.Blue),
        decorationBox = { inner ->
            Row(
                Modifier
                    .padding(horizontal = 14.dp)
                    .fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (leading != null) Icon(leading, null, Modifier.size(16.dp), CaramelColors.Text3)
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            style = CaramelType.Label.copy(
                                color = CaramelColors.Text3,
                                fontWeight = FontWeight.Normal,
                            ),
                        )
                    }
                    inner()
                }
            }
        },
    )
}

/** The search variant of [CaramelTextField]. */
@Composable
fun CaramelSearchField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) = CaramelTextField(value, onChange, modifier, placeholder, Icons.Rounded.Search)

/** A settings row: the label and its description on the left, the control on the right. */
@Composable
fun CaramelSettingRow(
    title: String,
    modifier: Modifier = Modifier,
    desc: String? = null,
    control: @Composable () -> Unit,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = CaramelType.Label)
            if (desc != null) Text(desc, style = CaramelType.Body)
        }
        control()
    }
}

/** A labelled slider with its current value on the right of the label row. */
@Composable
fun CaramelSliderRow(
    title: String,
    valueText: String,
    value: Float,
    onChange: (Float) -> Unit,
    accent: Color = CaramelColors.Blue,
    accentEnd: Color = Color(0xFF4ABEFF),
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text(title, style = CaramelType.Label, modifier = Modifier.weight(1f))
            Text(valueText, style = CaramelType.Label.copy(color = accent))
        }
        CaramelSlider(value, onChange, accent = accent, accentEnd = accentEnd)
    }
}

/** Adapts [androidx.compose.ui.focus.onFocusChanged] to a `(focused: Boolean) -> Unit` callback. */
fun Modifier.onFocusChangedCompat(block: (Boolean) -> Unit): Modifier =
    onFocusChanged { block(it.isFocused) }

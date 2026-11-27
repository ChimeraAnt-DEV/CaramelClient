package org.chimeramc.client.ui.compose.caramel

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.chimeramc.client.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * The three loader glyphs, ported from the pack's `components/LoaderIcons.kt`: the Bedrock block,
 * the Rust cog and the Lightning bolt. Each is drawn rather than shipped as an asset so it scales
 * and recolours without a density bucket.
 */
@Composable
fun LoaderGlyph(loader: CaramelLoader, modifier: Modifier = Modifier.size(28.dp)) {
    when (loader) {
        CaramelLoader.Bedrock -> Canvas(modifier) {
            val n = 12
            val px = size.width / n
            val rnd = Random(7)
            for (i in 0 until n) {
                for (j in 0 until n) {
                    val v = if (rnd.nextFloat() > 0.15f) listOf(52, 64, 78, 92, 108, 124).random(rnd) else listOf(28, 34).random(rnd)
                    drawRect(Color(v, v, v + 6), Offset(i * px, j * px), Size(px + 0.5f, px + 0.5f))
                }
            }
            drawRoundRect(Color(0xFF14141A), Offset.Zero, size, CornerRadius(2.dp.toPx()), Stroke(1.5.dp.toPx()))
        }
        CaramelLoader.Rust -> Box(modifier, Alignment.Center) {
            Canvas(Modifier.size(28.dp)) {
                val c = Offset(size.width / 2, size.height / 2)
                val r = size.width * 0.36f
                val rust = Color(0xFFDEA584)
                for (i in 0 until 12) {
                    val a = i * PI / 6
                    drawCircle(
                        rust,
                        size.width * 0.085f,
                        Offset(c.x + (r + size.width * 0.1f) * cos(a).toFloat(), c.y + (r + size.width * 0.1f) * sin(a).toFloat()),
                    )
                }
                drawCircle(rust, r + size.width * 0.04f, c)
                drawCircle(Color(0xFF221612), r * 0.72f, c)
            }
            Text("R", style = T.Title.copy(color = Color(0xFFDEA584), fontSize = 12.sp_))
        }
        CaramelLoader.Lightning -> Canvas(modifier) {
            val w = size.width
            val h = size.height
            val p = Path().apply {
                moveTo(0.56f * w, 0.06f * h)
                lineTo(0.19f * w, 0.56f * h)
                lineTo(0.46f * w, 0.56f * h)
                lineTo(0.40f * w, 0.94f * h)
                lineTo(0.81f * w, 0.40f * h)
                lineTo(0.54f * w, 0.40f * h)
                close()
            }
            drawPath(p, C.Yellow)
        }
        CaramelLoader.Road -> Image(
            painterResource(R.drawable.roadloader_icon),
            contentDescription = null,
            modifier = modifier,
        )
    }
}

/**
 * A selectable loader tile: the glyph in a rounded square, with a green tick when selected.
 *
 * <p>An unavailable loader (one that has not been built yet) is drawn greyed and inert, and says so
 * on a long press, so the choice is visible without pretending it can start a game. The shipped
 * SweetLoader is the only selectable one, which is the truth about this build.
 *
 * <p>The long-press explanation is raised through [onUnavailable], which the host routes to the
 * launcher's own toast, rather than a raw system `Toast`: the square system bubble ignores the
 * launcher's visual language and its reduce-motion setting, and the picker is drawn inside the
 * launcher's Compose tree where the notice is already mounted.
 */
@Composable
fun LoaderTile(
    loader: CaramelLoader,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onUnavailable: () -> Unit = {},
    onInDevelopment: () -> Unit = onUnavailable,
) {
    val shape = RoundedCornerShape(CaramelDimens.RadiusControl)
    val source = remember { MutableInteractionSource() }
    // A loader still in development answers a press: it shakes, its timer turns red and the host
    // raises the notice that says why it cannot be chosen. The other unavailable loaders stay inert
    // and explain themselves on a long press.
    val interactive = loader.available || loader.inDevelopment
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var warned by remember { mutableStateOf(false) }
    val shakeT = shake.value
    // A decaying oscillation reads as a struck object rather than a linear wobble: the angle and
    // the offset both fade out over the same 620ms, so the tile settles instead of snapping back.
    val decay = (1f - shakeT).coerceIn(0f, 1f)
    val angle = (sin(shakeT * 2.0 * PI * 4.5) * 9.0 * decay).toFloat()
    val nudge = (sin(shakeT * 2.0 * PI * 3.5) * 7.0 * decay).toFloat()
    Box(
        modifier
            .size(44.dp)
            .graphicsLayer {
                rotationZ = angle
                translationX = nudge * density
            }
            .background(C.sheen(if (selected) 0.12f else 0.07f), shape)
            .border(
                if (selected) 1.6.dp else 1.dp,
                if (selected) C.Green else C.sheen(0.14f),
                shape,
            )
            .clickable(
                source,
                null,
                enabled = interactive,
                onClick = {
                    if (loader.inDevelopment) {
                        warned = true
                        onInDevelopment()
                        scope.launch {
                            shake.snapTo(0f)
                            shake.animateTo(1f, tween(620, easing = LinearEasing))
                            shake.snapTo(0f)
                        }
                    } else {
                        onClick()
                    }
                },
            )
            .pointerInput(loader) {
                detectTapGestures(onLongPress = {
                    if (!loader.available && !loader.inDevelopment) onUnavailable()
                })
            },
        Alignment.Center,
    ) {
        Box(Modifier.alpha(if (loader.available) 1f else 0.35f)) {
            LoaderGlyph(loader, Modifier.size(28.dp))
        }
        if (loader.inDevelopment) {
            // The timer sits in front of the glyph: it is the "not yet" mark, grey until the user
            // presses the tile and red afterwards, so the refusal has a visible reason.
            Box(
                Modifier.size(22.dp).background(C.Abyss.copy(0.72f), CircleShape),
                Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Timer,
                    contentDescription = stringResource(R.string.caramel_loader_in_development),
                    modifier = Modifier.size(15.dp),
                    tint = if (warned) C.Red else C.Text3,
                )
            }
        }
        if (selected) {
            // The tick sits on the glyph's centre, not the tile's top-right corner: it marks the
            // chosen loader, and a corner badge read as a notification dot floating off the icon.
            // It is drawn large enough to read at a glance (it covers most of the 44dp tile) with a
            // ring in the tile colour so it still separates from the glyph behind it.
            Box(
                Modifier
                    .size(30.dp)
                    .background(C.Green, CircleShape)
                    .border(2.5.dp, C.Navy, CircleShape),
                Alignment.Center,
            ) { Icon(Icons.Rounded.Check, null, Modifier.size(20.dp), C.OnGreen) }
        }
    }
}

/**
 * The loader choice: one tile per preloader, plus an honest note when some are not built.
 *
 * <p>Both the launch menu and the instance settings draw the same picker, so the note that explains
 * the greyed tiles cannot be shown in one place and forgotten in the other. The picker declares its
 * own width rather than sizing to its content: the note is a long sentence, and an unconstrained
 * `Text` in a `Row` measures at the full available width, which starved the sibling label and made
 * "Loaders" wrap one letter per line.
 */
@Composable
fun CaramelLoaderPicker(
    selected: CaramelLoader,
    onSelect: (CaramelLoader) -> Unit,
    modifier: Modifier = Modifier,
    onUnavailable: (() -> Unit)? = null,
    onInDevelopment: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val unavailableText = stringResource(R.string.caramel_loader_unavailable)
    // The host supplies the launcher's own notice; the bare default keeps the picker usable from a
    // preview or a test, where there is no launcher tree to route a notice into. It still goes
    // through the notice bus, so a mounted launcher draws its own toast even if the host forgot to
    // pass one, and only a truly detached picker falls back to the system bubble.
    val report: () -> Unit = onUnavailable ?: {
        CaramelNoticeBus.post(context, unavailableText, false)
    }
    Column(modifier.width(IntrinsicSize.Min), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val reportDevelopment: () -> Unit = onInDevelopment ?: report
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CaramelLoader.entries.forEach {
                LoaderTile(
                    it,
                    selected == it,
                    { onSelect(it) },
                    onUnavailable = report,
                    onInDevelopment = reportDevelopment,
                )
            }
        }
        if (CaramelLoader.entries.any { !it.available }) {
            Text(stringResource(R.string.caramel_loaders_note), style = T.Caption)
        }
    }
}

package org.chimeramc.client.ui.compose.caramel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Frosted navy glass: the surface every Caramel panel is cut from.
 *
 * Ported from the UI pack's `GlassPanel` with two Android-specific changes:
 *
 * * The backdrop blur samples the screen behind the panel rather than a desktop wallpaper, so
 *   [CaramelBackdrop] draws the colour pools the glass refracts. Without something behind it, a
 *   frosted panel over a flat colour is just a slightly lighter rectangle.
 * * [popup] blurs the window layer instead of the wallpaper layer, for a modal that sits over the
 *   whole screen. Both sources are the same [HazeState] on a phone screen; the split is kept because
 *   it is what the pack's call sites pass and it documents the intent.
 */
@Composable
fun CaramelGlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    popup: Boolean = false,
    blur: Dp = 22.dp,
    elevation: Dp = 24.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val haze: HazeState = if (popup) LocalWindowHaze.current else LocalHaze.current
    val style = remember(blur, popup) {
        HazeStyle(
            backgroundColor = CaramelColors.Navy,
            tints = listOf(HazeTint(Color(0xFF9DB4FF).copy(alpha = if (popup) 0.10f else 0.06f))),
            blurRadius = blur,
            noiseFactor = 0.035f,
        )
    }
    Box(
        modifier
            .shadow(elevation, shape, ambientColor = Color.Black, spotColor = Color(0xFF00010F))
            .clip(shape)
            .hazeEffect(haze, style)
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(0.075f), Color.White.copy(0.015f)),
                ),
            )
            .border(
                1.dp,
                Brush.linearGradient(listOf(Color.White.copy(0.30f), Color.White.copy(0.05f))),
                shape,
            ),
        content = content,
    )
}

/**
 * The deep navy backdrop the glass refracts.
 *
 * Four soft colour pools over a vertical gradient. Ported verbatim from the pack: the pool
 * positions and alphas are the reason the glass reads as glass, so they are not a detail to
 * re-derive.
 */
@Composable
fun CaramelBackdrop(modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF070D22), Color(0xFF050918)))),
    ) {
        fun pool(x: Float, y: Float, r: Float, color: Color) {
            val centre = Offset(size.width * x, size.height * y)
            val radius = size.width * r
            drawCircle(
                Brush.radialGradient(listOf(color, Color.Transparent), centre, radius),
                radius = radius,
                center = centre,
            )
        }
        pool(0.18f, 0.20f, 0.42f, Color(0xFF3660FF).copy(0.55f))
        pool(0.82f, 0.10f, 0.32f, Color(0xFF28AAFF).copy(0.32f))
        pool(0.62f, 0.92f, 0.40f, Color(0xFF563CFF).copy(0.38f))
        pool(0.06f, 0.95f, 0.26f, Color(0xFFF5A742).copy(0.20f))
    }
}

/**
 * A dimmed scrim over a centred glass modal, with a scale-and-fade entrance.
 *
 * The scrim swallows taps so a modal cannot be dismissed by tapping the content behind it — the
 * pack's `clickable {}` no-op, which is a deliberate sink rather than a mistake.
 */
@Composable
fun CaramelModal(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xAA030714))
                .clickable(remember { MutableInteractionSource() }, null, onClick = onDismiss),
            Alignment.Center,
        ) {
            AnimatedVisibility(
                visible,
                enter = scaleIn(initialScale = 0.96f) + fadeIn(),
                exit = scaleOut(targetScale = 0.97f) + fadeOut(),
            ) {
                CaramelGlassPanel(
                    modifier.clickable(remember { MutableInteractionSource() }, null) {},
                    RoundedCornerShape(26.dp),
                    popup = true,
                    elevation = 40.dp,
                    content = content,
                )
            }
        }
    }
}

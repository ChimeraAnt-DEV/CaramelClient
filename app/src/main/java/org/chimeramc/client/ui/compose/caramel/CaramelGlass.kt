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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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

/**
 * The Caramel glass surface, ported from the pack's `components/Glass.kt`.
 *
 * <p>The pack frosts its glass with Haze, a Compose Multiplatform backdrop-blur library that samples
 * the window behind the card. Android's Compose has no equivalent (a `RenderEffect` can blur a
 * view's own content, not what is behind it), so this uses the recipe the pack itself falls back to
 * for ImGui: layered translucent fills that read as glass without a blur pass.
 *
 * <p>Two layers, and the light is what sells it: a cool blue-tinted fill at low alpha, then a
 * vertical sheen that is brightest at the top and falls to nothing, so the card catches light from
 * above. The hairline is a diagonal gradient rather than a flat colour, bright at the top-left and
 * dim at the bottom-right, which is the cheap trick that stops a rounded rectangle reading as a
 * flat outline.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(CaramelDimens.RadiusCard),
    popup: Boolean = false,
    elevation: Dp = 24.dp,
    tint: Color = Color(0xFF9DB4FF).copy(alpha = if (popup) 0.10f else 0.06f),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier
            .shadow(elevation, shape, ambientColor = Color.Black, spotColor = Color(0xFF00010F))
            .clip(shape)
            .background(C.Navy)
            .background(tint)
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(0.075f), Color.White.copy(0.015f)),
                ),
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(Color.White.copy(0.30f), Color.White.copy(0.05f)),
                ),
                shape,
            ),
        content = content,
    )
}

/**
 * The navy wallpaper with soft colour pools, ported from the pack's `Backdrop`.
 *
 * <p>The pools exist so the glass has something to sit on: a flat fill under translucent cards
 * leaves them looking like slightly lighter rectangles. Four radial gradients, drawn once on a
 * Canvas, give the glass a light source to refract.
 */
@Composable
fun CaramelBackdrop(modifier: Modifier = Modifier) {
    Canvas(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF070D22), Color(0xFF050918)))),
    ) {
        fun pool(x: Float, y: Float, r: Float, c: Color) = drawCircle(
            Brush.radialGradient(
                listOf(c, Color.Transparent),
                Offset(size.width * x, size.height * y),
                size.width * r,
            ),
            radius = size.width * r,
            center = Offset(size.width * x, size.height * y),
        )
        pool(0.18f, 0.20f, 0.42f, Color(0xFF3660FF).copy(0.55f))
        pool(0.82f, 0.10f, 0.32f, Color(0xFF28AAFF).copy(0.32f))
        pool(0.62f, 0.92f, 0.40f, Color(0xFF563CFF).copy(0.38f))
        pool(0.06f, 0.95f, 0.26f, Color(0xFFF5A742).copy(0.20f))
    }
}

/** Dimmed scrim plus a centred glass modal with a scale/fade entrance, from the pack's `Modal`. */
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
                GlassPanel(
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

/** The pack's `Pill`: a rounded chip used by the title bar and the account row. */
@Composable
fun CaramelPill(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .height(26.dp)
            .clip(shape)
            .background(Color.White.copy(0.07f))
            .border(1.dp, Color.White.copy(0.16f), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        content = content,
    )
}

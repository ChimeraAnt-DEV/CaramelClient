package org.chimeramc.client.ui.compose.caramel

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
    shape: Shape = RoundedCornerShape(caramelPrefs.cardRadiusDp.dp),
    popup: Boolean = false,
    elevation: Dp = 24.dp,
    tint: Color = Color(0xFF9DB4FF).copy(alpha = if (popup) 0.10f else 0.06f),
    content: @Composable BoxScope.() -> Unit,
) {
    // "Glass blur strength" scales the frost and the sheen together: Android has no backdrop blur,
    // so the only honest knob is how much light the layered fills hold. A low strength reads as a
    // near-flat card, a high one as heavier glass; neither claims a blur the platform cannot do.
    val glass = caramelPrefs.glassStrength
    // "UI Transparency" is the base fill's opacity. It stops at 0.30 rather than 0, because a
    // fully transparent panel puts its label straight on the backdrop and the text stops being
    // readable; the last third of the slider is where the surface is already as thin as it can go.
    val fill = 0.30f + 0.70f * caramelPrefs.uiTransparency.coerceIn(0f, 1f)
    Box(
        modifier
            .shadow(elevation, shape, ambientColor = Color.Black, spotColor = Color(0xFF00010F))
            .clip(shape)
            .background(C.Navy.copy(alpha = fill))
            .background(tint.copy(alpha = tint.alpha * glass * fill))
            .background(
                Brush.verticalGradient(
                    listOf(C.sheen(0.075f * glass * fill), C.sheen(0.015f * glass * fill)),
                ),
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(C.sheen(0.30f), C.sheen(0.05f)),
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
    val light = C.light
    Canvas(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    if (light) listOf(Color(0xFFF8FAFE), Color(0xFFE9EEF9))
                    else listOf(Color(0xFF070D22), Color(0xFF050918)),
                ),
            ),
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
        val strength = if (light) 0.30f else 1f
        pool(0.18f, 0.20f, 0.42f, Color(0xFF3660FF).copy(0.55f * strength))
        pool(0.82f, 0.10f, 0.32f, Color(0xFF28AAFF).copy(0.32f * strength))
        pool(0.62f, 0.92f, 0.40f, Color(0xFF563CFF).copy(0.38f * strength))
        pool(0.06f, 0.95f, 0.26f, Color(0xFFF5A742).copy(0.20f * strength))
    }
}

/**
 * The duration a decorative transition should take.
 *
 * <p>Honours two settings, in order: "Reduce motion" collapses every transition to a single frame
 * so a panel or toast appears rather than animates (the switch used to be stored and never read, so
 * it had no effect in the Compose launcher), and "Animation Speed" then scales whatever remains.
 */
@Composable
fun caramelMotionMs(baseMs: Int): Int {
    if (caramelPrefs.motionScale <= 0f) return 1
    val speed = caramelPrefs.animationSpeed.coerceIn(0.5f, 3f)
    return (baseMs / speed).toInt().coerceAtLeast(1)
}

/**
 * Draws the user's chosen launcher background, when there is one.
 *
 * <p>Android Compose has no backdrop blur, so "blur" is honest here too: the blur slider scales a
 * translucent navy scrim over the image (a soft veil) rather than a real blur pass, and brightness
 * is a colour filter. The image itself is scaled to fill, cropped to the centre. When no image is
 * stored this draws nothing and the navy backdrop shows through.
 */
@Composable
fun CaramelBackgroundImage(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val pm = remember(context) { org.chimeramc.client.util.PersonalizationManager(context) }
    // Re-read on every store change: the panel can pick, clear or restyle the image while this
    // composition is alive, and without the key it would keep drawing the first image it decoded.
    val generation = CaramelPersonalizationSignal.generation
    val bitmap = remember(context, generation) {
        try {
            if (pm.hasBackgroundImage()) pm.loadBackgroundBitmap() else null
        } catch (t: Throwable) {
            null
        }
    }
    if (bitmap == null) return
    val brightness = pm.getBackgroundImageBrightness() / 100f
    val veil = (pm.getBackgroundImageBlur() / 25f) * 0.5f
    // Dimming is a scrim drawn over the image, not a `ColorFilter.tint`: a tint with alpha 0 is a
    // SrcIn blend whose result is transparent, so the default brightness of 100% erased the image
    // entirely — which is why picking a background looked like nothing happened.
    val scrim = (1f - brightness).coerceIn(0f, 0.9f)
    val scrimColor = if (C.light) Color(0xFFF2F5FB) else Color(0xFF050918)
    Box(modifier.fillMaxSize()) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (scrim > 0f) {
            Box(Modifier.fillMaxSize().background(scrimColor.copy(alpha = scrim)))
        }
        if (veil > 0f) {
            Box(Modifier.fillMaxSize().background(scrimColor.copy(alpha = veil)))
        }
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
    // A modal closes from its own X, from the system back gesture, and from nothing else. The scrim
    // used to be the dismiss target, so a stray tap anywhere on the screen — the commonest thing to
    // do while reading a dialog — threw it away. The scrim still swallows the tap so it cannot
    // reach the screen behind, it just no longer closes the dialog.
    BackHandler(enabled = visible) { onDismiss() }
    val fade = tween<Float>(durationMillis = caramelMotionMs(160))
    val scale = tween<Float>(durationMillis = caramelMotionMs(160))
    AnimatedVisibility(visible, enter = fadeIn(fade), exit = fadeOut(fade)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xAA030714))
                .clickable(remember { MutableInteractionSource() }, null) {},
            Alignment.Center,
        ) {
            AnimatedVisibility(
                visible,
                enter = scaleIn(scale, initialScale = 0.96f) + fadeIn(fade),
                exit = scaleOut(scale, targetScale = 0.97f) + fadeOut(fade),
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
            .background(C.sheen(0.07f))
            .border(1.dp, C.sheen(0.16f), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        content = content,
    )
}

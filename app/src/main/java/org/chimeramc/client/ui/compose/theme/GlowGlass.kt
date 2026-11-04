package org.chimeramc.client.ui.compose.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The shared glass surface for the Compose screens: the Compose twin of
 * `res/drawable/glass_card.xml`.
 *
 * <p>A single solid fill with a hairline reads as a grey rectangle, which is what makes a UI look
 * unfinished. Glass reads as glass because of its light, not its colour: a rim that is brighter at
 * the top than the bottom, so the card catches light from above, and a vertical sheen that falls off
 * to transparent downward. [glowGlass] draws the fill and the sheen; [GlassSurface] adds the rim and
 * the standard corner radius, so every Compose card shares one material. Both are pure gradient
 * layers, so they cost nothing per frame and work on every supported version, exactly like the View
 * drawable.
 */
fun Modifier.glowGlass(shape: RoundedCornerShape, base: Color): Modifier = this
    .clip(shape)
    // The base fill first, then the sheen over it, so the sheen lightens the top without replacing
    // the surface colour. The sheen has fully fallen off by 60% of the height.
    .background(base)
    .background(
        Brush.verticalGradient(
            0f to Color(GlowPalette.GLASS_SHEEN),
            0.6f to Color(GlowPalette.GLASS_SHEEN_END),
        ),
    )

/**
 * A card in the shared glass material: the base fill under the sheen, a rim that is brighter at the
 * top, and the standard corner radius.
 *
 * <p>A drop-in for a `Surface` that drew a flat fill and a hairline. The rim's top edge is the
 * brighter colour, so the card reads as catching light from above.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(GlowDimens.CARD_RADIUS.dp),
    color: Color = glowColors.surface,
    border: BorderStroke? = BorderStroke(
        GlowDimens.CARD_BORDER_WIDTH.dp,
        Color(GlowPalette.GLASS_RIM_TOP),
    ),
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        // Transparent so the inner Box owns the fill: it draws the base colour and the sheen as one
        // clipped layer, rather than the fill sitting over an already-filled surface.
        color = Color.Transparent,
        border = border,
    ) {
        Box(modifier = Modifier.glowGlass(shape, color)) {
            content()
        }
    }
}

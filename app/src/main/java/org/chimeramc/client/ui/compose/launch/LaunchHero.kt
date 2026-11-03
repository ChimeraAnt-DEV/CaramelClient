package org.chimeramc.client.ui.compose.launch

import android.view.ViewGroup
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.chimeramc.client.R
import org.chimeramc.client.ui.compose.theme.GlowDimens
import org.chimeramc.client.ui.compose.theme.glowColors
import org.chimeramc.client.ui.compose.theme.glowMotion

/**
 * The Launch-tab hero: the selected instance's identity and stats on the left, the live 3D character
 * preview on the right.
 *
 * <p>The preview used to sit on the left. Moving it to the right is deliberate: the eye lands on the
 * instance name and the PLAY control first, and the character reads as the "what you are about to
 * play" flourish beside them rather than as the thing that pushes the identity off the screen.
 *
 * <p>The preview is the real [CapePreviewView] hosted in an [AndroidView], so the equipped cape, hat
 * and pet render exactly as they do in game; the Compose layer never reimplements the renderer. The
 * view is started and stopped with the composition, and released when it leaves, so it cannot animate
 * off-screen or leak its decoded atlases.
 */
@Composable
fun LaunchHero(
    state: LaunchHeroState,
    onPreviewClick: () -> Unit,
    onHeroClick: () -> Unit,
    onPreviewReady: (org.chimeramc.client.core.mods.inbuilt.overlay.CapePreviewView) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = glowColors
    val motion = glowMotion
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // A gentle press scale, matching the View screens' press feedback.
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = tween(if (motion.reduced) 0 else motion.fastMs),
        label = "heroPress",
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onHeroClick,
            ),
        shape = RoundedCornerShape(GlowDimens.CARD_RADIUS.dp),
        color = colors.surface,
        border = BorderStroke(GlowDimens.CARD_BORDER_WIDTH.dp, colors.stroke),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GlowDimens.CARD_PADDING.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left: identity and stats.
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = state.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(GlowDimens.SPACE_3.dp))
                    PillBadge(text = state.badge, fill = colors.snowBlue, onFill = colors.textOnSnow)
                }
                Spacer(Modifier.height(GlowDimens.SPACE_3.dp))
                Text(
                    text = state.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (state.version.isNotEmpty() || state.abi != LaunchHeroState.AbiKind.NONE) {
                    Spacer(Modifier.height(GlowDimens.SPACE_1.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.version.isNotEmpty()) {
                            Text(
                                text = state.version,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
                            )
                        }
                        if (state.abi != LaunchHeroState.AbiKind.NONE) {
                            Spacer(Modifier.width(GlowDimens.SPACE_2.dp))
                            val is32 = state.abi == LaunchHeroState.AbiKind.BIT_32
                            OutlinedChip(
                                text = stringResource(
                                    if (is32) R.string.abi_32_bit else R.string.abi_64_bit
                                ),
                                contentColor = if (is32) colors.warning else colors.snowBlue,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(GlowDimens.SPACE_4.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    HeroStat(
                        modifier = Modifier.weight(1f),
                        value = state.instancesCount.toString(),
                        label = stringResource(R.string.stat_instances),
                    )
                    StatDivider()
                    HeroStat(
                        modifier = Modifier.weight(1f),
                        value = state.modsCount.toString(),
                        label = stringResource(R.string.stat_mods),
                    )
                    StatDivider()
                    HeroStat(
                        modifier = Modifier.weight(1f),
                        value = state.playtime,
                        label = stringResource(R.string.stat_playtime),
                    )
                }
            }

            Spacer(Modifier.width(GlowDimens.SPACE_4.dp))

            // Right: the live 3D preview.
            CharacterPreview(
                contentDescription = state.previewDescription,
                onClick = onPreviewClick,
                onReady = onPreviewReady,
                modifier = Modifier
                    .width(160.dp)
                    .aspectRatio(0.82f),
            )
        }
    }
}

/**
 * The live character preview, hosted from the real [CapePreviewView].
 *
 * <p>The view is started on attach and released on dispose, so its animation frame callback and its
 * decoded atlases never outlive the screen.
 */
@Composable
private fun CharacterPreview(
    contentDescription: String,
    onClick: () -> Unit,
    onReady: (org.chimeramc.client.core.mods.inbuilt.overlay.CapePreviewView) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .semantics { this.contentDescription = contentDescription }
            .clickable(onClick = onClick),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                org.chimeramc.client.core.mods.inbuilt.overlay.CapePreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    setAlwaysAnimate(true)
                    // The host binds the equipped cape/hat/pet and starts the preview, because it
                    // owns the CosmeticStore read; the composable only creates the view.
                    onReady(this)
                }
            },
            onRelease = { view ->
                try {
                    view.stopPreview()
                    view.release()
                } catch (t: Throwable) {
                    // Releasing a preview must never be the reason a screen fails to leave, but a
                    // failure here is still worth one line in the log.
                    org.chimeramc.client.util.AppLog.warnOnce(
                        "GlowberryClient", "LaunchHero:release",
                        "Ignored a failure releasing the character preview", t,
                    )
                }
            },
        )
    }
}

@Composable
private fun HeroStat(modifier: Modifier, value: String, label: String) {
    val colors = glowColors
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(GlowDimens.SPACE_DIVIDER.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = GlowDimens.SPACE_4.dp)
            .width(GlowDimens.SPACE_DIVIDER.dp)
            .height(32.dp)
            .background(glowColors.stroke),
    )
}

@Composable
private fun PillBadge(text: String, fill: Color, onFill: Color) {
    Surface(shape = RoundedCornerShape(GlowDimens.RADIUS_TAG.dp), color = fill) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = GlowDimens.SPACE_3.dp,
                vertical = GlowDimens.SPACE_1.dp,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = onFill,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun OutlinedChip(text: String, contentColor: Color) {
    Surface(
        shape = RoundedCornerShape(GlowDimens.RADIUS_BADGE.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, contentColor),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = GlowDimens.SPACE_2.dp,
                vertical = GlowDimens.SPACE_HAIRLINE.dp,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            fontWeight = FontWeight.Bold,
        )
    }
}

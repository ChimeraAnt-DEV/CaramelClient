package org.chimeramc.client.ui.compose.launch

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.chimeramc.client.R
import org.chimeramc.client.ui.compose.theme.GlassSurface
import org.chimeramc.client.ui.compose.theme.GlowDimens
import org.chimeramc.client.ui.compose.theme.glowColors
import org.chimeramc.client.ui.compose.theme.glowMotion

/**
 * The changelog popup: a stretched, medium-sized dialog that pops in and holds the whole release.
 *
 * <p>It is deliberately not full-screen. The card promises the full list of what was added, changed
 * and fixed, and a reader wants to see that next to the tab rather than in a new page, so the dialog
 * is a fixed width (capped for tablets) and a height that grows with the content up to a ceiling.
 *
 * <p>Two window controls, because a release note is a thing people leave open: the minus collapses it
 * to its header, and the X dismisses it. "View Wiki" opens a small notice, because the wiki does not
 * exist yet and a dead link is worse than saying so.
 */
@Composable
fun LaunchChangelogPopup(
    state: LaunchChangelogState,
    onDismiss: () -> Unit,
) {
    val motion = glowMotion
    // The pop-in: a spring from 0.86 to 1, plus a fade. Reduced motion lands it at once.
    var entered by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.86f,
        animationSpec = if (motion.reduced) tween(0) else spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "changelogPopupScale",
    )
    val fade by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(if (motion.reduced) 0 else motion.standardMs),
        label = "changelogPopupFade",
    )
    androidx.compose.runtime.LaunchedEffect(Unit) { entered = true }

    var collapsed by remember { mutableStateOf(false) }
    var wikiNotice by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(GlowDimens.SPACE_4.dp),
            contentAlignment = Alignment.Center,
        ) {
            GlassSurface(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .alpha(fade)
                    .scale(scale),
                shape = RoundedCornerShape(GlowDimens.CARD_RADIUS.dp),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Header(
                        state = state,
                        collapsed = collapsed,
                        onCollapse = { collapsed = !collapsed },
                        onClose = onDismiss,
                    )
                    if (!collapsed) {
                        Body(state = state, modifier = Modifier.heightIn(max = 460.dp))
                        Footer(onViewWiki = { wikiNotice = true })
                    }
                }
            }
        }
    }

    if (wikiNotice) {
        UpcomingSoonDialog(onDismiss = { wikiNotice = false })
    }
}

/** The header: the release name, the counts, the minimise and the close controls. */
@Composable
private fun Header(
    state: LaunchChangelogState,
    collapsed: Boolean,
    onCollapse: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = glowColors
    Column(modifier = Modifier.padding(GlowDimens.CARD_PADDING.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GlowDimens.SPACE_3.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.changelog_card_title, state.version),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (state.headline.isNotEmpty()) {
                    Spacer(Modifier.height(GlowDimens.SPACE_HAIRLINE.dp))
                    Text(
                        text = state.headline,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onCollapse) {
                Icon(
                    imageVector = if (collapsed) Icons.Filled.Add else Icons.Filled.Remove,
                    contentDescription = stringResource(R.string.changelog_popup_minimize),
                    tint = colors.textSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.changelog_popup_close),
                    tint = colors.textSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        if (state.summary.isNotEmpty()) {
            Spacer(Modifier.height(GlowDimens.SPACE_2.dp))
            Text(
                text = state.summary,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                maxLines = if (collapsed) 1 else 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** The scrollable body: the three buckets, then the catch-all. */
@Composable
private fun Body(state: LaunchChangelogState, modifier: Modifier = Modifier) {
    if (state.isEmpty) {
        EmptyBody()
        return
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = GlowDimens.CARD_PADDING.dp)
            .padding(bottom = GlowDimens.SPACE_4.dp),
    ) {
        Bucket(stringResource(R.string.changelog_popup_added), state.added, glowColors.success)
        Bucket(stringResource(R.string.changelog_popup_changed), state.changed, glowColors.snowBlue)
        Bucket(stringResource(R.string.changelog_popup_fixed), state.fixed, glowColors.warning)
        Bucket(stringResource(R.string.changelog_popup_more), state.other, glowColors.textSecondary)
    }
}

@Composable
private fun EmptyBody() {
    val colors = glowColors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(GlowDimens.CARD_PADDING.dp),
    ) {
        Text(
            text = stringResource(R.string.changelog_popup_empty),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
        )
    }
}

/** One bucket: a coloured heading and its notes. An empty bucket renders nothing. */
@Composable
private fun Bucket(title: String, entries: List<ChangelogEntry>, tint: Color) {
    if (entries.isEmpty()) return
    val colors = glowColors
    Spacer(Modifier.height(GlowDimens.SPACE_4.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GlowDimens.SPACE_2.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(GlowDimens.RADIUS_TAG.dp))
                .background(tint),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = entries.size.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textDisabled,
        )
    }
    Spacer(Modifier.height(GlowDimens.SPACE_2.dp))
    entries.forEach { entry -> Note(entry, tint) }
}

@Composable
private fun Note(entry: ChangelogEntry, tint: Color) {
    val colors = glowColors
    val interaction = remember { MutableInteractionSource() }
    val context = androidx.compose.ui.platform.LocalContext.current
    val clickable = if (entry.hasUrl) {
        Modifier.clickable(interactionSource = interaction, indication = null) {
            openLink(context, entry.url)
        }
    } else {
        Modifier
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickable)
            .padding(vertical = GlowDimens.SPACE_2.dp),
        horizontalArrangement = Arrangement.spacedBy(GlowDimens.SPACE_2.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(4.dp)
                .clip(RoundedCornerShape(GlowDimens.RADIUS_TAG.dp))
                .background(tint.copy(alpha = 0.7f)),
        )
        Text(
            text = entry.text,
            style = MaterialTheme.typography.labelSmall,
            color = if (entry.hasUrl) colors.snowBlue else colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
    }
}

/** The footer: the one action, "View Wiki". */
@Composable
private fun Footer(onViewWiki: () -> Unit) {
    val colors = glowColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(GlowDimens.CARD_PADDING.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(GlowDimens.RADIUS_LARGE.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onViewWiki),
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = GlowDimens.SPACE_4.dp,
                    vertical = GlowDimens.SPACE_3.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(GlowDimens.SPACE_2.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.MenuBook,
                    contentDescription = null,
                    tint = colors.textOnSnow,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.changelog_view_wiki),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textOnSnow,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/** The "the wiki is not live yet" notice, shown instead of a dead link. */
@Composable
private fun UpcomingSoonDialog(onDismiss: () -> Unit) {
    val colors = glowColors
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(GlowDimens.SPACE_4.dp),
            contentAlignment = Alignment.Center,
        ) {
            GlassSurface(
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(GlowDimens.CARD_RADIUS.dp),
            ) {
                Column(modifier = Modifier.padding(GlowDimens.CARD_PADDING.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.changelog_upcoming_soon),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.changelog_popup_close),
                                tint = colors.textSecondary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(GlowDimens.SPACE_2.dp))
                    Text(
                        text = stringResource(R.string.changelog_upcoming_soon_body),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
}

/**
 * Opens a note's link through the app's single URL opener.
 *
 * <p>Not a bare ACTION_VIEW: `UrlOpener` is the one audited place a link is opened, it enforces
 * https, and it carries the deliberate `UnsafeImplicitIntentLaunch` suppression. A second implicit
 * intent here failed the lint gate.
 */
private fun openLink(context: android.content.Context, url: String) {
    org.chimeramc.client.util.UrlOpener.open(context, url)
}

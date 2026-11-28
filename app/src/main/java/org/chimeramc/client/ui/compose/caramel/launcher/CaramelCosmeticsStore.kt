package org.chimeramc.client.ui.compose.caramel.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import org.chimeramc.client.R
import androidx.compose.ui.unit.dp
import org.chimeramc.client.ui.compose.caramel.C
import org.chimeramc.client.ui.compose.caramel.CaramelNotice
import org.chimeramc.client.ui.compose.caramel.Tone
import org.chimeramc.client.ui.compose.caramel.CaramelCosmeticInfo
import org.chimeramc.client.ui.compose.caramel.CaramelCosmeticKind
import org.chimeramc.client.ui.compose.caramel.CaramelLauncherState
import org.chimeramc.client.ui.compose.caramel.FilterChip
import org.chimeramc.client.ui.compose.caramel.GlassButton
import org.chimeramc.client.ui.compose.caramel.GlassPanel
import org.chimeramc.client.ui.compose.caramel.PrimaryButton
import org.chimeramc.client.ui.compose.caramel.StatusLabel
import org.chimeramc.client.ui.compose.caramel.T
import org.chimeramc.client.ui.compose.caramel.sp_

private val cosAccents = listOf(C.Blue, C.Caramel, C.Green, Color(0xFF966EFF))

/** The cosmetic kinds, in the order the filter chips show them (index 1..4). */
private val cosFilterKinds = listOf(
    CaramelCosmeticKind.Cape,
    CaramelCosmeticKind.Wings,
    CaramelCosmeticKind.Hat,
    CaramelCosmeticKind.Pet,
)

/**
 * The tiles a chip selects: 0 is "All", 1..4 are the kinds above, 5 is the starred set.
 *
 * <p>Pure and index-based so the filter the chips promise is the filter the grid applies; before
 * this the chip only moved its own highlight and the grid kept showing everything.
 */
internal fun filterCosmetics(all: List<CaramelCosmeticInfo>, chip: Int): List<CaramelCosmeticInfo> =
    when {
        chip == 5 -> all.filter { it.favourite }
        chip in 1..cosFilterKinds.size -> all.filter { it.kind == cosFilterKinds[chip - 1] }
        else -> all
    }

/**
 * The Cosmetics screen, ported from the pack's `CosmeticsScreen`.
 *
 * <p>The pack reserves a white rounded square for the 3D skin viewer; on Android the client already
 * has a software skin renderer, so the slot is filled by the host through [skinViewer]. When the
 * host passes nothing, the slot shows its empty label rather than a fake model.
 */
@Composable
fun CaramelCosmeticsScreen(
    state: CaramelLauncherState,
    onToggleFavourite: (CaramelCosmeticInfo) -> Unit,
    onEquip: (CaramelCosmeticInfo) -> Unit,
    onResetView: () -> Unit,
    skinViewer: @Composable (Modifier) -> Unit = { EmptySkinSlot(it) },
) {
    var chip by remember { mutableStateOf(0) }
    val signedIn = state.signedIn
    CaramelPage(stringResource(R.string.mod_menu_tab_cosmetics), stringResource(R.string.caramel_cosmetics_sub)) {
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            GlassPanel(Modifier.width(320.dp).fillMaxHeight(), RoundedCornerShape(24.dp)) {
                Column(
                    Modifier.fillMaxSize().padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SkinViewport(signedIn, skinViewer, Modifier.fillMaxWidth().weight(1f))
                    GlassButton(
                        stringResource(R.string.caramel_save_outfit), {}, Modifier.fillMaxWidth(),
                        icon = Icons.Rounded.Check, enabled = signedIn,
                    )
                    GlassButton(
                        stringResource(R.string.caramel_reset_view), onResetView, Modifier.fillMaxWidth(),
                        icon = Icons.Rounded.SwapHoriz, enabled = signedIn,
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(R.string.caramel_filter_all, R.string.caramel_filter_capes, R.string.caramel_filter_wings, R.string.caramel_filter_hats, R.string.caramel_filter_pets).forEachIndexed { i, t -> FilterChip(stringResource(t), chip == i, { chip = i }) }
                    FilterChip(stringResource(R.string.caramel_filter_favorites), chip == 5, { chip = 5 })
                }
                val shown = remember(state.cosmetics, chip) { filterCosmetics(state.cosmetics, chip) }
                if (!signedIn) {
                    // A signed-out user has no account skin to wear, so the wardrobe is not usable.
                    // The grid is dimmed rather than hidden, so the catalogue is still discoverable
                    // and the reason is stated on top of it.
                    Box(Modifier.fillMaxWidth().weight(1f)) {
                        CosmeticGrid(shown, onToggleFavourite, onEquip, Modifier.fillMaxSize().alpha(0.35f))
                        Box(
                            Modifier.fillMaxSize()
                                .background(Brush.verticalGradient(listOf(Color(0xCC060B1A), Color(0xE6060B1A)))),
                            Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.width(280.dp),
                            ) {
                                Icon(Icons.Rounded.Lock, null, Modifier.size(30.dp), C.Text3)
                                Text(
                                    stringResource(R.string.caramel_cosmetics_signed_out_title),
                                    style = T.Title, textAlign = TextAlign.Center,
                                )
                                Text(
                                    stringResource(R.string.caramel_cosmetics_signed_out_body),
                                    style = T.Body, textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                } else {
                    CosmeticGrid(shown, onToggleFavourite, onEquip, Modifier.fillMaxWidth().weight(1f))
                }
            }
        }
    }
}

/** The tile grid, or the matching empty state when the filter selects nothing. */
@Composable
private fun CosmeticGrid(
    shown: List<CaramelCosmeticInfo>,
    onFav: (CaramelCosmeticInfo) -> Unit,
    onEquip: (CaramelCosmeticInfo) -> Unit,
    modifier: Modifier,
) {
    if (shown.isEmpty()) {
        EmptyState(
            stringResource(R.string.caramel_no_favourite_cosmetics_title),
            stringResource(R.string.caramel_no_favourite_cosmetics_body),
            modifier,
        )
        return
    }
    LazyVerticalGrid(
        GridCells.Adaptive(190.dp),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(shown, key = { it.name }) { c -> CosmeticTile(c, { onFav(c) }, { onEquip(c) }) }
    }
}

/**
 * The 3D skin viewport: a recessed, dark panel that hosts the renderer when an account is signed in.
 *
 * <p>The slot used to be a hard `Color.White` square, which read as a blank white box behind the
 * character and, when the renderer had nothing to draw, as a plain white rectangle with no
 * explanation. It is now a navy viewport that matches the launcher's glass, and a signed-out user
 * sees the "not signed in" state instead of an empty box.
 */
@Composable
private fun SkinViewport(
    signedIn: Boolean,
    skinViewer: @Composable (Modifier) -> Unit,
    modifier: Modifier,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(C.Glass, C.DeepBlue)))
            .border(1.dp, C.sheen(0.10f), RoundedCornerShape(22.dp)),
        Alignment.Center,
    ) {
        if (signedIn) {
            skinViewer(Modifier.fillMaxSize().padding(8.dp))
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(20.dp),
            ) {
                Icon(Icons.Rounded.PersonOff, null, Modifier.size(40.dp), C.Text3)
                Text(
                    stringResource(R.string.not_signed_in),
                    style = T.Title.copy(color = C.Text2), textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.caramel_skin_signed_out_hint),
                    style = T.Body, textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
internal fun EmptySkinSlot(modifier: Modifier) {
    Box(modifier, Alignment.Center) {
        Text(
            stringResource(R.string.caramel_skin_preview),
            style = T.Title.copy(color = C.Text3, fontSize = 15.sp_, textAlign = TextAlign.Center),
            modifier = Modifier.width(190.dp),
        )
    }
}

@Composable
private fun CosmeticTile(c: CaramelCosmeticInfo, onFav: () -> Unit, onEquip: () -> Unit) {
    val accent = cosAccents[(c.name.hashCode() and 0x7fffffff) % 4]
    GlassPanel(Modifier.fillMaxWidth().height(188.dp), RoundedCornerShape(18.dp), elevation = 6.dp) {
        Column(Modifier.padding(8.dp)) {
            Box(
                Modifier.fillMaxWidth().height(112.dp)
                    .background(Brush.verticalGradient(listOf(accent.copy(0.32f), accent.copy(0.05f))), RoundedCornerShape(12.dp)),
                Alignment.Center,
            ) {
                CaramelCosmeticThumbnail(c, Modifier.fillMaxWidth().height(104.dp).padding(6.dp), 192)
                Box(
                    Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp).clip(CircleShape)
                        .background(Color(0x96081024))
                        .border(1.dp, if (c.favourite) C.Caramel else C.sheen(0.25f), CircleShape)
                        .clickable(remember { MutableInteractionSource() }, null, onClick = onFav),
                    Alignment.Center,
                ) {
                    Icon(
                        if (c.favourite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        null, Modifier.size(16.dp), if (c.favourite) C.Caramel else C.Text,
                    )
                }
            }
            Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(c.name, style = T.Label, maxLines = 1)
                if (c.equipped) {
                    StatusLabel(stringResource(R.string.caramel_equipped), C.Green)
                } else if (c.owned) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.caramel_owned), style = T.Body, modifier = Modifier.weight(1f))
                        GlassButton(stringResource(R.string.caramel_equip), onEquip, height = 28)
                    }
                } else {
                    StatusLabel(stringResource(R.string.caramel_locked), C.Caramel)
                }
            }
        }
    }
}

/** The News screen: a featured post over a side list. */
@Composable
fun CaramelNewsScreen(state: CaramelLauncherState) {
    CaramelPage(stringResource(R.string.caramel_news_title), stringResource(R.string.caramel_news_sub)) {
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            // The featured slot follows the card the user pressed, falling back to the newest
            // flagged post. Without this the screen always showed the first post and the other two
            // announcements could not be opened at all.
            val featured = state.news.firstOrNull { it.title == state.featuredNewsTitle }
                ?: state.news.firstOrNull { it.featured }
                ?: state.news.firstOrNull()
            if (featured == null) {
                EmptyState(stringResource(R.string.caramel_no_news_title), stringResource(R.string.caramel_no_news_body), Modifier.fillMaxSize())
            } else {
                GlassPanel(Modifier.weight(1.3f).fillMaxHeight(), RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(6.dp)) {
                        Box(
                            Modifier.fillMaxWidth().height(260.dp)
                                .background(Brush.verticalGradient(listOf(Color(0xFF5A2818), Color(0xFFF5963C))), RoundedCornerShape(18.dp)),
                        ) {
                            if (featured.imageRes != 0) {
                                androidx.compose.foundation.Image(
                                    androidx.compose.ui.res.painterResource(featured.imageRes),
                                    contentDescription = null,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)),
                                )
                            }
                        }
                        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatusLabel(stringResource(R.string.caramel_featured), C.Caramel)
                            Text(featured.title, style = T.Display)
                            Text(featured.summary, style = T.Body.copy(fontSize = 13.sp_))
                            Text(featured.meta, style = T.Label.copy(color = C.Caramel))
                            if (featured.actionLabel.isNotEmpty() && featured.actionUrl.isNotEmpty()) {
                                PrimaryButton(
                                    featured.actionLabel,
                                    {
                                        state.webTitle = featured.title
                                        state.webUrl = featured.actionUrl
                                    },
                                    tone = Tone.Blue,
                                    icon = androidx.compose.material.icons.Icons.Rounded.OpenInNew,
                                    height = 34,
                                )
                            }
                        }
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.news.filter { it.title != featured.title }.take(3).forEachIndexed { i, n ->
                        GlassPanel(
                            Modifier.fillMaxWidth().height(130.dp)
                                .clickable(remember { MutableInteractionSource() }, null) {
                                    state.featuredNewsTitle = n.title
                                },
                            RoundedCornerShape(20.dp),
                            elevation = 6.dp,
                        ) {
                            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Box(
                                    Modifier.size(150.dp, 106.dp)
                                        .background(Brush.verticalGradient(listOf(cosAccents[i % 4].copy(0.5f), cosAccents[i % 4].copy(0.1f))), RoundedCornerShape(12.dp)),
                                ) {
                                    if (n.imageRes != 0) {
                                        androidx.compose.foundation.Image(
                                            androidx.compose.ui.res.painterResource(n.imageRes),
                                            contentDescription = null,
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                        )
                                    }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(n.title, style = T.Subtitle.copy(fontSize = 15.sp_), maxLines = 2)
                                    Text(n.summary, style = T.Body, maxLines = 2)
                                    Text(n.meta, style = T.Caption)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** The Store screen: a Caramel+ banner over the bundle grid. */
@Composable
fun CaramelStoreScreen(state: CaramelLauncherState) {
    CaramelPage(stringResource(R.string.caramel_store_title), stringResource(R.string.caramel_store_sub)) {
        GlassPanel(Modifier.fillMaxWidth().height(160.dp), RoundedCornerShape(24.dp)) {
            Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(C.Caramel.copy(0.3f), C.Blue.copy(0.08f)))))
            Row(Modifier.fillMaxSize().padding(horizontal = 36.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.caramel_plus), style = T.Display.copy(fontSize = 28.sp_))
                    Text(
                        stringResource(R.string.caramel_plus_blurb),
                        style = T.Body.copy(fontSize = 13.5.sp_),
                    )
                }
                val noPayment = stringResource(R.string.caramel_plus_no_payment)
                PrimaryButton(
                    stringResource(R.string.caramel_get_plus),
                    { state.notice = CaramelNotice(noPayment, warning = true) },
                    Modifier.width(200.dp),
                    Tone.Caramel,
                    muted = true,
                )
            }
        }
        EmptyState(
            stringResource(R.string.caramel_store_soon_title),
            stringResource(R.string.caramel_store_soon_body),
            Modifier.fillMaxWidth().weight(1f),
        )
    }
}

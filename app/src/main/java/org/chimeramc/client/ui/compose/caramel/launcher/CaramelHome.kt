package org.chimeramc.client.ui.compose.caramel.launcher

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.chimeramc.client.R
import org.chimeramc.client.ui.compose.caramel.C
import org.chimeramc.client.ui.compose.caramel.CaramelFriendRelation
import org.chimeramc.client.ui.compose.caramel.CaramelFriendRow
import org.chimeramc.client.ui.compose.caramel.CaramelNotice
import org.chimeramc.client.ui.compose.caramel.CaramelLauncherState
import org.chimeramc.client.ui.compose.caramel.CaramelNewsInfo
import org.chimeramc.client.ui.compose.caramel.GlassButton
import org.chimeramc.client.ui.compose.caramel.GlassPanel
import org.chimeramc.client.ui.compose.caramel.PrimaryButton
import org.chimeramc.client.ui.compose.caramel.T
import org.chimeramc.client.ui.compose.caramel.Tone
import org.chimeramc.client.ui.compose.caramel.sp_

/**
 * The Home screen, ported from the pack's `launcher/Home.kt`.
 *
 * <p>The pack's hero, launch split button, latest-news row, friends card and Caramel+ promo are all
 * here. The two cards whose content is data (news, the version line) read [CaramelLauncherState]
 * rather than the pack's literals, so nothing drawn is a value the client did not measure.
 */
@Composable
fun CaramelHomeScreen(
    state: CaramelLauncherState,
    onLaunch: () -> Unit,
    onOpenScreenshots: () -> Unit,
) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Hero(state, onLaunch, onOpenScreenshots, Modifier.fillMaxWidth().height(320.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.caramel_latest_news), style = T.Title, modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.caramel_view_all_news),
                    style = T.Label.copy(color = C.Text2, fontSize = 11.5.sp_),
                    modifier = Modifier.clickable(remember { MutableInteractionSource() }, null) { state.newsAll = true },
                )
            }
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val cards = state.news.take(3)
                if (cards.isEmpty()) {
                    EmptyCard(stringResource(R.string.caramel_no_news_title), stringResource(R.string.caramel_no_news_body), Modifier.weight(1f))
                } else {
                    cards.forEachIndexed { i, n ->
                        NewsCard(state, n, newsArt(i), Modifier.weight(1f))
                    }
                }
            }
        }
        Column(Modifier.width(374.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            FriendsCard(state, Modifier.fillMaxWidth().height(320.dp))
            PromoCard(state, Modifier.fillMaxWidth().weight(1f))
        }
    }
}

/** The three news-card art gradients, matching the pack's amber / blue / pink rotation. */
private fun newsArt(i: Int): List<Color> = when (i % 3) {
    0 -> listOf(Color(0xFF5A2818), Color(0xFFF5963C))
    1 -> listOf(Color(0xFF182660), Color(0xFF4A5ABE))
    else -> listOf(Color(0xFF541C4C), Color(0xFFF06EA0))
}

@Composable
private fun Hero(state: CaramelLauncherState, onLaunch: () -> Unit, onOpenScreenshots: () -> Unit, modifier: Modifier) {
    GlassPanel(modifier, RoundedCornerShape(24.dp)) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(
                Brush.radialGradient(
                    listOf(C.Blue.copy(0.28f), Color.Transparent),
                    Offset(size.width * 0.25f, size.height * 0.5f),
                    size.width * 0.5f,
                ),
                center = Offset(size.width * 0.25f, size.height * 0.5f),
                radius = size.width * 0.5f,
            )
        }
        Image(painterResource(R.drawable.ic_caramel_logo), null, Modifier.align(Alignment.CenterStart).padding(start = 24.dp).size(210.dp))
        Column(
            Modifier.align(Alignment.Center).padding(start = 130.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.caramel_ready), style = T.Body)
            LaunchSplitButton(state, onLaunch)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton(stringResource(R.string.caramel_profiles), { state.launchMenu = true }, icon = Icons.Rounded.Folder, height = 32)
                GlassButton(stringResource(R.string.caramel_instances), { state.instanceSettings = true }, icon = Icons.Rounded.Tune, height = 32)
                GlassButton(stringResource(R.string.caramel_screenshots_title), onOpenScreenshots, icon = Icons.Rounded.PhotoCamera, height = 32)
            }
            val line = state.instanceName.ifEmpty { stringResource(R.string.caramel_no_instance) }
            Text(line, style = T.Caption)
        }
    }
}

/** LAUNCH GAME with a split chevron that opens the launch menu, from the pack's `LaunchSplitButton`. */
@Composable
private fun LaunchSplitButton(state: CaramelLauncherState, onLaunch: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.width(340.dp).height(72.dp).clip(shape)
            .background(Brush.verticalGradient(listOf(C.GreenLight, C.GreenDark)))
            .background(Brush.verticalGradient(listOf(Color.White.copy(0.22f), Color.Transparent, Color.Transparent)))
            .border(1.dp, Color.White.copy(0.3f), shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.weight(1f).fillMaxHeight()
                .clickable(enabled = state.launchEnabled && !state.launching, onClick = onLaunch),
            Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                if (state.launching) {
                    CircularProgressIndicator(color = C.OnGreen, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Text(stringResource(R.string.caramel_launch_game), style = T.Display.copy(fontSize = 19.sp_, color = C.OnGreen))
                    val sub = state.version?.id?.let { stringResource(R.string.caramel_instance_launch_sub, it) } ?: stringResource(R.string.caramel_no_version_installed)
                    Text(sub, style = T.Chip.copy(color = C.OnGreen.copy(0.85f)))
                }
            }
        }
        Box(Modifier.width(1.dp).height(48.dp).background(C.OnGreen.copy(0.3f)))
        Box(
            Modifier.width(52.dp).fillMaxHeight()
                .clickable(remember { MutableInteractionSource() }, null) { state.launchMenu = !state.launchMenu },
            Alignment.Center,
        ) {
            Icon(
                if (state.launchMenu) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                null, Modifier.size(24.dp), C.OnGreen,
            )
        }
    }
}

@Composable
private fun NewsCard(state: CaramelLauncherState, post: CaramelNewsInfo, art: List<Color>, modifier: Modifier) {
    // Tapping the card opens the News tab with this post featured: the three cards used to be
    // static, so two of the three announcements could not be read at all.
    GlassPanel(
        modifier.clickable(remember { MutableInteractionSource() }, null) {
            state.featuredNewsTitle = post.title
            state.tab = org.chimeramc.client.ui.compose.caramel.CaramelTab.News
        },
        RoundedCornerShape(18.dp),
        elevation = 10.dp,
    ) {
        Column(Modifier.padding(6.dp)) {
            Box(
                Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(13.dp))
                    .background(Brush.verticalGradient(art)),
            ) {
                if (post.imageRes != 0) {
                    Image(
                        painterResource(post.imageRes),
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (post.imageRes == 0) {
                    Canvas(Modifier.fillMaxSize()) {
                        val u = 12.dp.toPx()
                        var x = 0f
                        var i = 0
                        while (x < size.width) {
                            val h = size.height * 0.36f + (kotlin.math.sin(i * 0.5) * 18.dp.toPx()).toFloat()
                            drawRect(Color.Black.copy(0.45f), Offset(x, size.height - h), androidx.compose.ui.geometry.Size(u, h))
                            drawRect(Color.White.copy(0.25f), Offset(x, size.height - h), androidx.compose.ui.geometry.Size(u, 3.dp.toPx()))
                            x += u
                            i++
                        }
                    }
                }
            }
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(post.title, style = T.Subtitle.copy(fontSize = 13.5.sp_), maxLines = 2)
                Text(post.meta, style = T.Body, maxLines = 2)
                if (post.actionLabel.isNotEmpty() && post.actionUrl.isNotEmpty()) {
                    // The card's own action, so the announcement is one tap from Home too. Same
                    // tone and icon as the News modal's button, so the one announcement wears one
                    // style wherever it appears.
                    val label = post.actionLabel
                    PrimaryButton(
                        label,
                        { state.webTitle = post.title; state.webUrl = post.actionUrl },
                        tone = Tone.Blue,
                        icon = Icons.Rounded.OpenInNew,
                        height = 32,
                    )
                }
            }
        }
    }
}

/** The empty state a card shows when its data has not loaded, instead of a fabricated row. */
@Composable
private fun EmptyCard(title: String, body: String, modifier: Modifier) {
    GlassPanel(modifier, RoundedCornerShape(18.dp), elevation = 10.dp) {
        Column(
            Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = T.Subtitle.copy(fontSize = 13.5.sp_))
            Spacer(Modifier.height(6.dp))
            Text(body, style = T.Body, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun FriendsCard(state: CaramelLauncherState, modifier: Modifier) {
    GlassPanel(modifier, RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxSize().padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.caramel_friends), style = T.Title, modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.caramel_view_all),
                    style = T.Label.copy(color = C.Text2, fontSize = 11.5.sp_),
                    modifier = Modifier.clickable(remember { MutableInteractionSource() }, null) { state.friendsAll = true },
                )
            }
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (state.friends.isNotEmpty()) {
                    Column(
                        Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Friends first, then the requests, so the card is not filled by people who
                        // have not accepted yet; the row's own label says which is which.
                        val shown = (state.friends.filter { it.relation == CaramelFriendRelation.FRIEND }
                                + state.friends.filter { it.relation != CaramelFriendRelation.FRIEND })
                            .take(4)
                        shown.forEach { friend ->
                            CaramelFriendRow(friend)
                        }
                    }
                } else {
                    Box(
                        Modifier.size(60.dp).clip(CircleShape).background(C.sheen(0.07f))
                            .border(1.dp, C.sheen(0.17f), CircleShape),
                        Alignment.Center,
                    ) { Icon(Icons.Rounded.Group, null, Modifier.size(28.dp), C.Text2) }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.caramel_friends_sub),
                        style = T.Body.copy(fontSize = 12.5.sp_, textAlign = TextAlign.Center),
                        modifier = Modifier.width(200.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    GlassButton(stringResource(R.string.caramel_add_first_friend), { state.addFriend = true }, icon = Icons.Rounded.Add)
                }
            }
        }
    }
}

@Composable
private fun PromoCard(state: CaramelLauncherState, modifier: Modifier) {
    GlassPanel(modifier, RoundedCornerShape(24.dp)) {
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(C.Caramel.copy(0.26f), Color.Transparent))))
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(painterResource(R.drawable.ic_caramel_logo), null, Modifier.size(130.dp))
            Text(stringResource(R.string.caramel_become_a), style = T.Title.copy(fontSize = 19.sp_))
            Text(stringResource(R.string.caramel_plus), style = T.Display.copy(fontSize = 25.sp_, color = C.Caramel))
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.caramel_plus_blurb),
                style = T.Body.copy(fontSize = 12.5.sp_, textAlign = TextAlign.Center),
                modifier = Modifier.width(310.dp),
            )
            Spacer(Modifier.weight(1f))
            // No payment method is attached yet, so the button is drawn unavailable: greyed, and a
            // tap explains why rather than silently doing nothing.
            val noPayment = stringResource(R.string.caramel_plus_no_payment)
            PrimaryButton(
                stringResource(R.string.caramel_get_plus),
                { state.notice = CaramelNotice(noPayment, warning = true) },
                Modifier.fillMaxWidth(),
                Tone.Caramel,
                muted = true,
            )
        }
    }
}

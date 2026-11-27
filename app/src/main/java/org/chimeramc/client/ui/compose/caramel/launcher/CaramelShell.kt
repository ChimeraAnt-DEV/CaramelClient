package org.chimeramc.client.ui.compose.caramel.launcher

import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.chimeramc.client.R
import org.chimeramc.client.ui.compose.caramel.C
import org.chimeramc.client.ui.compose.caramel.CaramelAvatarPicture
import org.chimeramc.client.ui.compose.caramel.CaramelBackdrop
import org.chimeramc.client.ui.compose.caramel.CaramelBackgroundImage
import org.chimeramc.client.ui.compose.caramel.CaramelLauncherState
import org.chimeramc.client.ui.compose.caramel.CaramelPill
import org.chimeramc.client.ui.compose.caramel.CaramelServerInfo
import org.chimeramc.client.ui.compose.caramel.CaramelTab
import org.chimeramc.client.ui.compose.caramel.GlassPanel
import org.chimeramc.client.ui.compose.caramel.ServerIcon
import org.chimeramc.client.ui.compose.caramel.T
import org.chimeramc.client.ui.compose.caramel.sp_

/**
 * The launcher frame, ported from the pack's `launcher/Shell.kt`.
 *
 * <p>The pack mounts its frame inside a desktop `Window` with a `WindowDraggableArea`. On Android
 * the window is the activity, so the drag area is dropped and the title bar is a plain row. The
 * backdrop, the glass title bar and the 56dp sidebar rail are the pack's.
 */
@Composable
fun CaramelLauncherFrame(
    state: CaramelLauncherState,
    onJoinServer: (CaramelServerInfo) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    Box(Modifier.fillMaxSize().background(C.Abyss)) {
        CaramelBackdrop()
        CaramelBackgroundImage()
        Column(Modifier.fillMaxSize()) {
            CaramelTitleBar(state, onJoinServer)
            Row(
                Modifier.fillMaxSize().padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CaramelSidebar(state)
                Box(Modifier.weight(1f).fillMaxHeight()) { content(Modifier.fillMaxSize()) }
            }
        }
    }
}

@Composable
private fun CaramelTitleBar(state: CaramelLauncherState, onJoinServer: (CaramelServerInfo) -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Image(painterResource(R.drawable.ic_caramel_logo), null, Modifier.size(30.dp))
        Text(stringResource(R.string.caramel_brand), style = T.Subtitle)
        val menuShape = RoundedCornerShape(8.dp)
        Box(
            Modifier.size(22.dp).clip(menuShape)
                .background(C.sheen(if (state.appMenu) 0.14f else 0.06f))
                .border(1.dp, C.sheen(0.18f), menuShape)
                .clickable(remember { MutableInteractionSource() }, null) { state.appMenu = !state.appMenu },
            Alignment.Center,
        ) {
            Icon(
                if (state.appMenu) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                null, Modifier.size(15.dp), C.Text2,
            )
        }
        CaramelPill {
            Icon(Icons.Rounded.RocketLaunch, null, Modifier.size(13.dp), C.Text2)
            Text(stringResource(R.string.caramel_launchpad), style = T.Label.copy(fontSize = 11.5.sp_, color = C.Text2))
        }
        Spacer(Modifier.width(12.dp))
        Text(stringResource(R.string.caramel_quick_play), style = T.Caption)
        // Quick play tiles: one per saved/default server, capped so the row never overflows. Each
        // carries its server's own mark inside its own rounded square, and joins that server.
        state.servers.take(8).forEachIndexed { i, s ->
            ServerIcon(
                quickPlayAccent(i),
                26,
                Modifier.clickable(remember { MutableInteractionSource() }, null) { onJoinServer(s) },
                iconKey = s.iconKey,
            )
            Spacer(Modifier.width(6.dp))
        }
        Spacer(Modifier.weight(1f))
        Text(stringResource(R.string.caramel_instances_running, state.instancesCount), style = T.Caption)
        CaramelPill(onClick = { state.accountMenu = !state.accountMenu }) {
            if (state.signedIn) {
                CaramelAvatarPicture(state.accountAvatarUrl, 20, online = true)
                Text(state.accountName, style = T.Label.copy(fontSize = 11.5.sp_))
            } else {
                Text(stringResource(R.string.sign_in), style = T.Label.copy(fontSize = 11.5.sp_))
            }
            Icon(Icons.Rounded.KeyboardArrowDown, null, Modifier.size(14.dp), C.Text3)
        }
    }
}

/** The eight quick-play accents, in order, so a tile keeps its colour as servers are added. */
private fun quickPlayAccent(i: Int): Color = listOf(
    C.Caramel, C.Blue, C.Green, C.Red,
    Color(0xFF966EFF), Color(0xFF4AC8E6), Color(0xFFFF8C3C), Color(0xFF7882FF),
)[i % 8]

private val nav = listOf(
    CaramelTab.Home to Icons.Rounded.SportsEsports,
    CaramelTab.Mods to Icons.Rounded.Extension,
    CaramelTab.Servers to Icons.Rounded.Public,
    CaramelTab.Cosmetics to Icons.Rounded.ViewInAr,
    CaramelTab.News to Icons.Rounded.Article,
    CaramelTab.CurseForge to Icons.Rounded.Extension,
    CaramelTab.Store to Icons.Rounded.ShoppingCart,
)

@Composable
private fun CaramelSidebar(state: CaramelLauncherState) {
    GlassPanel(Modifier.width(56.dp).fillMaxHeight(), RoundedCornerShape(18.dp), elevation = 12.dp) {
        Column(
            Modifier.fillMaxSize().padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            nav.forEach { (tab, icon) ->
                if (tab == CaramelTab.CurseForge) {
                    CaramelCurseForgeNavItem(state.tab == tab) { state.tab = tab }
                } else {
                    CaramelNavItem(icon, state.tab == tab) { state.tab = tab }
                }
            }
            Spacer(Modifier.weight(1f))
            CaramelNavItem(Icons.Rounded.Settings, state.tab == CaramelTab.Settings) { state.tab = CaramelTab.Settings }
        }
    }
}

@Composable
private fun CaramelNavItem(icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    CaramelNavSlot(active, onClick) { tint ->
        Icon(icon, null, Modifier.size(22.dp), tint)
    }
}

/**
 * The CurseForge rail item: the real forge mark, orange while the tab is open.
 *
 * <p>The mark swings and its colour travels from forge orange to red as it swings — the hammer is
 * "struck", which is what makes the tab read as live rather than as another static glyph. The
 * accent bar under it is orange, the tab's own colour.
 */
@Composable
private fun CaramelCurseForgeNavItem(active: Boolean, onClick: () -> Unit) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "curseforge")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1800, easing = androidx.compose.animation.core.LinearEasing),
        ),
        label = "curseforge-swing",
    )
    val swing = kotlin.math.sin(phase * 2.0 * kotlin.math.PI).toFloat()
    val tint = when {
        !active -> C.Text3
        else -> androidx.compose.ui.graphics.lerp(C.Orange, C.Red, (swing + 1f) / 2f)
    }
    CaramelNavSlot(active, onClick, accent = C.Orange) {
        Image(
            painterResource(R.drawable.ic_brand_curseforge),
            contentDescription = null,
            modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = if (active) swing * 11f else 0f },
            colorFilter = ColorFilter.tint(tint),
        )
    }
}

/** The shared rail slot: the hit area, the active fill and the accent bar. */
@Composable
private fun CaramelNavSlot(
    active: Boolean,
    onClick: () -> Unit,
    accent: Color = C.Caramel,
    content: @Composable (Color) -> Unit,
) {
    Box(Modifier.size(44.dp), Alignment.Center) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(13.dp))
                .background(if (active) C.sheen(0.12f) else Color.Transparent)
                .then(if (active) Modifier.border(1.dp, C.sheen(0.2f), RoundedCornerShape(13.dp)) else Modifier)
                .clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
            Alignment.Center,
        ) { content(if (active) C.Text else C.Text3) }
        if (active) {
            Box(
                Modifier.align(Alignment.CenterStart).offset((-14).dp).size(3.dp, 22.dp)
                    .background(accent, RoundedCornerShape(2.dp)),
            )
        }
    }
}

package org.chimeramc.client.ui.compose.caramel.launch

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.chimeramc.client.ui.compose.caramel.C
import org.chimeramc.client.ui.compose.caramel.CaramelAvatar
import org.chimeramc.client.ui.compose.caramel.CaramelPill
import org.chimeramc.client.ui.compose.caramel.GlassButton
import org.chimeramc.client.ui.compose.caramel.GlassPanel
import org.chimeramc.client.ui.compose.caramel.PrimaryButton
import org.chimeramc.client.ui.compose.caramel.StatusLabel
import org.chimeramc.client.ui.compose.caramel.T
import org.chimeramc.client.ui.compose.caramel.Tone
import org.chimeramc.client.ui.compose.caramel.sp_

/**
 * The Caramel launch screen: the pack's title bar, sidebar and Home page, driven by the launcher's
 * real state.
 *
 * <p>Ported from the pack's `launcher/Shell.kt` and `launcher/Home.kt`. The pack is a desktop window
 * with a fake in-app title bar; on Android the title bar is the real one, so what is kept here is
 * the pack's *content* chrome: the brand row (logo, the Caramel Client menu, the Launchpad pill, the
 * Quick play server row, the running-instance count and the account pill), the vertical sidebar, and
 * the hero with its split launch control.
 *
 * <p>Every pack placeholder is replaced by real data: the version dropdown lists the installed
 * versions, the account pill shows the signed-in account, and the split button starts a real
 * session. The pack's example News/Friends/Promo cards are replaced by the two things a launch
 * screen actually needs — the quick toggles and the content library counts — because a fake
 * "Example mod updates" card on the home screen is the same dishonesty as a fake frame counter.
 */
@Composable
fun CaramelLaunchScreen(
    state: CaramelLaunchState,
    onLaunch: () -> Unit,
    onSelectVersion: (String) -> Unit,
    onAccountClick: () -> Unit,
    onOpenMods: () -> Unit,
    onOpenCosmetics: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenContent: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenCurseForge: () -> Unit,
    onOpenModHub: () -> Unit,
    onOpenQuickLaunch: () -> Unit,
    contentWorlds: Int,
    contentPacks: Int,
    contentBehaviors: Int,
    quickToggles: List<CaramelQuickToggle>,
    onToggle: (String, Boolean) -> Unit,
    onPreviewReady: (org.chimeramc.client.core.mods.inbuilt.overlay.CapePreviewView) -> Unit,
    onCycleBackground: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(C.Abyss)) {
        Column(Modifier.fillMaxSize()) {
            TitleBar(state, onAccountClick, onOpenAccounts, onOpenCurseForge, onOpenModHub, onOpenQuickLaunch)
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Sidebar(onOpenMods, onOpenCosmetics, onOpenContent, onOpenSettings)
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    HomePage(
                        state, onLaunch, onSelectVersion, onOpenMods, onOpenCosmetics,
                        onOpenContent, contentWorlds, contentPacks, contentBehaviors,
                        quickToggles, onToggle, onPreviewReady, onCycleBackground,
                    )
                }
            }
        }
    }
}

/** One quick-toggle card: a client feature with a live on/off state. */
class CaramelQuickToggle(
    val id: String,
    val title: String,
    val status: String,
    val on: Boolean,
    val enabled: Boolean,
)

@Composable
private fun TitleBar(
    state: CaramelLaunchState,
    onAccountClick: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenCurseForge: () -> Unit,
    onOpenModHub: () -> Unit,
    onOpenQuickLaunch: () -> Unit,
) {
    var appMenu by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Image(
            painter = painterResource(org.chimeramc.client.R.drawable.ic_caramel_logo),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
        )
        Text("Caramel Client", style = T.Subtitle)
        Box {
            val shape = RoundedCornerShape(8.dp)
            Box(
                Modifier
                    .size(22.dp)
                    .clip(shape)
                    .background(Color.White.copy(0.06f))
                    .border(1.dp, Color.White.copy(0.18f), shape)
                    .clickable { appMenu = !appMenu },
                Alignment.Center,
            ) {
                Icon(
                    if (appMenu) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    null,
                    Modifier.size(15.dp),
                    C.Text2,
                )
            }
            if (appMenu) {
                Popup(
                    alignment = Alignment.TopStart,
                    offset = IntOffset(0, 30),
                    onDismissRequest = { appMenu = false },
                    properties = PopupProperties(focusable = true),
                ) {
                    GlassPanel(Modifier.width(200.dp), RoundedCornerShape(16.dp), popup = true, elevation = 32.dp) {
                        Column(Modifier.padding(8.dp)) {
                            AppMenuRow("Accounts") { appMenu = false; onOpenAccounts() }
                            AppMenuRow("CurseForge") { appMenu = false; onOpenCurseForge() }
                            AppMenuRow("Mod Hub") { appMenu = false; onOpenModHub() }
                            AppMenuRow("Quick Launch") { appMenu = false; onOpenQuickLaunch() }
                        }
                    }
                }
            }
        }
        CaramelPill {
            Icon(Icons.Rounded.RocketLaunch, null, Modifier.size(13.dp), C.Text2)
            Text("Launchpad", style = T.Label.copy(fontSize = 11.5.sp_, color = C.Text2))
        }
        Spacer(Modifier.weight(1f))
        Text("${state.runningInstances} instances", style = T.Caption)
        CaramelPill(onClick = onAccountClick) {
            if (state.avatar != null) {
                Image(
                    state.avatar.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                CaramelAvatar(18)
            }
            Text(
                if (state.signedIn) state.accountName else "Sign in",
                style = T.Label.copy(fontSize = 11.5.sp_),
            )
            Icon(Icons.Rounded.KeyboardArrowDown, null, Modifier.size(14.dp), C.Text3)
        }
    }
}

@Composable
private fun AppMenuRow(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        Alignment.CenterStart,
    ) {
        Text(label, style = T.Label)
    }
}

@Composable
private fun Sidebar(
    onOpenMods: () -> Unit,
    onOpenCosmetics: () -> Unit,
    onOpenContent: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    GlassPanel(
        Modifier.width(56.dp).fillMaxHeight(),
        RoundedCornerShape(18.dp),
        elevation = 12.dp,
    ) {
        Column(
            Modifier.fillMaxSize().padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            NavItem(Icons.Rounded.RocketLaunch, true, onOpenContent)
            NavItem(Icons.Rounded.Folder, false, onOpenMods)
            NavItem(Icons.Rounded.AccountCircle, false, onOpenCosmetics)
            Spacer(Modifier.weight(1f))
            NavItem(Icons.Rounded.Settings, false, onOpenSettings)
        }
    }
}

@Composable
private fun NavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    onClick: () -> Unit,
) {
    Box(Modifier.size(44.dp), Alignment.Center) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(if (active) Color.White.copy(0.12f) else Color.Transparent)
                .then(
                    if (active) {
                        Modifier.border(1.dp, Color.White.copy(0.2f), RoundedCornerShape(13.dp))
                    } else {
                        Modifier
                    },
                )
                .clickable(onClick = onClick),
            Alignment.Center,
        ) {
            Icon(icon, null, Modifier.size(22.dp), if (active) C.Text else C.Text3)
        }
        if (active) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset((-14).dp)
                    .size(3.dp, 22.dp)
                    .background(C.Caramel, RoundedCornerShape(2.dp)),
            )
        }
    }
}

@Composable
private fun HomePage(
    state: CaramelLaunchState,
    onLaunch: () -> Unit,
    onSelectVersion: (String) -> Unit,
    onOpenMods: () -> Unit,
    onOpenCosmetics: () -> Unit,
    onOpenContent: () -> Unit,
    contentWorlds: Int,
    contentPacks: Int,
    contentBehaviors: Int,
    quickToggles: List<CaramelQuickToggle>,
    onToggle: (String, Boolean) -> Unit,
    onPreviewReady: (org.chimeramc.client.core.mods.inbuilt.overlay.CapePreviewView) -> Unit,
    onCycleBackground: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Hero(state, onLaunch, onSelectVersion, onOpenMods, onOpenCosmetics, onPreviewReady,
                onCycleBackground)
        }
        if (quickToggles.isNotEmpty()) {
            item { SectionHeader("Quick actions") }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    quickToggles.forEach { t ->
                        QuickToggleCard(t, Modifier.weight(1f)) { onToggle(t.id, !t.on) }
                    }
                }
            }
        }
        item { SectionHeader("Content library", onOpenContent) }
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CountCard("Worlds", contentWorlds, C.Blue, Modifier.weight(1f), onOpenContent)
                CountCard("Resource packs", contentPacks, C.Green, Modifier.weight(1f), onOpenContent)
                CountCard("Behavior packs", contentBehaviors, C.Caramel, Modifier.weight(1f), onOpenContent)
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun SectionHeader(title: String, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = T.Title, modifier = Modifier.weight(1f))
        if (onAction != null) {
            Text(
                "View all",
                style = T.Label.copy(color = C.Text2, fontSize = 11.5.sp_),
                modifier = Modifier.clickable(onClick = onAction),
            )
        }
    }
}

/** The hero: the pack's split LAUNCH GAME control, the identity line and the live stats. */
@Composable
private fun Hero(
    state: CaramelLaunchState,
    onLaunch: () -> Unit,
    onSelectVersion: (String) -> Unit,
    onOpenMods: () -> Unit,
    onOpenCosmetics: () -> Unit,
    onPreviewReady: (org.chimeramc.client.core.mods.inbuilt.overlay.CapePreviewView) -> Unit,
    onCycleBackground: () -> Unit,
) {
    GlassPanel(
        Modifier.fillMaxWidth().height(300.dp),
        RoundedCornerShape(24.dp),
    ) {
        Canvas(Modifier.fillMaxSize()) {
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
        Row(
            Modifier.fillMaxSize().padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left: identity and the launch control.
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    if (state.instanceName.isEmpty()) "No instance selected" else state.instanceName,
                    style = T.Title,
                    textAlign = TextAlign.Center,
                )
                LaunchSplitButton(state, onLaunch, onSelectVersion)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassButton("Mods", onOpenMods, icon = Icons.Rounded.Folder, height = 32)
                    GlassButton("Cosmetics", onOpenCosmetics, icon = Icons.Rounded.SwapHoriz, height = 32)
                }
                if (state.buildLine.isNotEmpty()) {
                    Text(state.buildLine, style = T.Caption)
                }
            }
            // Right: the live 3D character preview (the real CapePreviewView, not a reimplementation),
            // on the chosen paint-splat background. A tap cycles the background, which is the pack's
            // preview interaction without a modal picker on a phone-sized screen.
            Box(
                Modifier.width(160.dp).fillMaxHeight()
                    .clickable(onClick = onCycleBackground),
                contentAlignment = Alignment.Center,
            ) {
                if (state.backgroundRes != 0) {
                    Image(
                        painter = painterResource(state.backgroundRes),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth(0.92f).aspectRatio(228f / 128f),
                    )
                }
            androidx.compose.ui.viewinterop.AndroidView(
                modifier = Modifier.width(150.dp).fillMaxHeight(),
                factory = { ctx ->
                    org.chimeramc.client.core.mods.inbuilt.overlay.CapePreviewView(ctx).apply {
                        setAlwaysAnimate(true)
                        onPreviewReady(this)
                    }
                },
                onRelease = { view ->
                    try {
                        view.stopPreview()
                        view.release()
                    } catch (t: Throwable) {
                        org.chimeramc.client.util.AppLog.warnOnce(
                            "CaramelClient", "CaramelLaunchScreen:release",
                            "Ignored a failure releasing the character preview", t,
                        )
                    }
                },
            )
            }
        }
    }
}

/**
 * The pack's `LaunchSplitButton`: one green face that starts a session, and a chevron that opens the
 * launch options. Two tap targets on one surface, so "start" and "change what starts" cannot be
 * confused — which is the bug the old dock's two overlapping listeners used to cause.
 */
@Composable
private fun LaunchSplitButton(
    state: CaramelLaunchState,
    onLaunch: () -> Unit,
    onSelectVersion: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)
    Box {
        Row(
            Modifier
                .width(320.dp)
                .height(68.dp)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(C.GreenLight, C.GreenDark)))
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(0.22f), Color.Transparent, Color.Transparent),
                    ),
                )
                .border(1.dp, Color.White.copy(0.3f), shape),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(enabled = state.launchEnabled && !state.launching, onClick = onLaunch),
                Alignment.Center,
            ) {
                if (state.launching) {
                    CircularProgressIndicator(Modifier.size(26.dp), color = C.OnGreen, strokeWidth = 3.dp)
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            "LAUNCH GAME",
                            style = T.Display.copy(fontSize = 18.sp_, color = C.OnGreen),
                        )
                        Text(
                            state.buildLine.ifEmpty { state.versionId },
                            style = T.Chip.copy(color = C.OnGreen.copy(0.85f)),
                        )
                    }
                }
            }
            Box(Modifier.width(1.dp).height(46.dp).background(C.OnGreen.copy(0.3f)))
            Box(
                Modifier
                    .width(50.dp)
                    .fillMaxHeight()
                    .clickable { open = !open },
                Alignment.Center,
            ) {
                Icon(
                    if (open) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    null,
                    Modifier.size(24.dp),
                    C.OnGreen,
                )
            }
        }
        if (open) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, 76),
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true),
            ) {
                GlassPanel(
                    Modifier.width(300.dp),
                    RoundedCornerShape(18.dp),
                    popup = true,
                    elevation = 32.dp,
                ) {
                    Column(
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Version", style = T.Title.copy(fontSize = 15.sp_))
                        if (state.versions.isEmpty()) {
                            Text("No versions installed", style = T.Body)
                        } else {
                            LazyColumn(Modifier.heightIn(max = 260.dp)) {
                                items(state.versions, key = { it.id }) { v ->
                                    val selected = v.id == state.versionId
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(38.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(
                                                if (selected) C.Blue.copy(0.26f) else Color.Transparent,
                                            )
                                            .clickable {
                                                onSelectVersion(v.id)
                                                open = false
                                            }
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(v.id, style = T.Label, modifier = Modifier.weight(1f))
                                        StatusLabel(
                                            if (v.beta) "Beta" else "Release",
                                            if (v.beta) C.Orange else C.Green,
                                        )
                                    }
                                }
                            }
                        }
                        PrimaryButton(
                            "Launch Game",
                            { open = false; onLaunch() },
                            Modifier.fillMaxWidth(),
                            icon = Icons.Rounded.RocketLaunch,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickToggleCard(t: CaramelQuickToggle, modifier: Modifier, onToggle: () -> Unit) {
    GlassPanel(modifier.height(104.dp), RoundedCornerShape(18.dp), elevation = 8.dp) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(t.title, style = T.Subtitle.copy(fontSize = 14.sp_))
            Text(
                t.status,
                style = T.Body,
                modifier = Modifier.weight(1f),
            )
            org.chimeramc.client.ui.compose.caramel.CaramelToggle(
                checked = t.on,
                onChange = { if (t.enabled) onToggle() },
            )
        }
    }
}

@Composable
private fun CountCard(
    label: String,
    count: Int,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    GlassPanel(
        modifier.height(88.dp).clickable(onClick = onClick),
        RoundedCornerShape(18.dp),
        elevation = 8.dp,
    ) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text("$count", style = T.Display.copy(fontSize = 22.sp_, color = accent))
            Text(label, style = T.Body)
        }
    }
}

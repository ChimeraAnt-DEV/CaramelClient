package org.chimeramc.client.ui.compose.caramel.launcher

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewInAr
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.chimeramc.client.R
import org.chimeramc.client.ui.compose.caramel.C
import org.chimeramc.client.ui.compose.caramel.CaramelAvatarPicture
import org.chimeramc.client.ui.compose.caramel.CaramelFriendRow
import org.chimeramc.client.ui.compose.caramel.CaramelUserChip
import org.chimeramc.client.ui.compose.caramel.CaramelFoundUser
import org.chimeramc.client.ui.compose.caramel.CaramelLauncherState
import org.chimeramc.client.ui.compose.caramel.CaramelModal
import org.chimeramc.client.ui.compose.caramel.CaramelNotice
import org.chimeramc.client.ui.compose.caramel.CaramelSettingsPanel
import org.chimeramc.client.ui.compose.caramel.CaramelSettingsProvider
import org.chimeramc.client.ui.compose.caramel.CaramelProfileDropdown
import org.chimeramc.client.ui.compose.caramel.CaramelScreenshotInfo
import org.chimeramc.client.ui.compose.caramel.CaramelTab
import org.chimeramc.client.ui.compose.caramel.CaramelTextField
import org.chimeramc.client.ui.compose.caramel.CaramelToggle
import org.chimeramc.client.ui.compose.caramel.CaramelVersionDropdown
import org.chimeramc.client.ui.compose.caramel.FilterChip
import org.chimeramc.client.ui.compose.caramel.GlassButton
import org.chimeramc.client.ui.compose.caramel.GlassPanel

import org.chimeramc.client.ui.compose.caramel.PrimaryButton
import org.chimeramc.client.ui.compose.caramel.SearchField
import org.chimeramc.client.ui.compose.caramel.SettingRow
import org.chimeramc.client.ui.compose.caramel.SliderRow
import org.chimeramc.client.ui.compose.caramel.StatusLabel
import org.chimeramc.client.ui.compose.caramel.T
import org.chimeramc.client.ui.compose.caramel.Tone
import org.chimeramc.client.ui.compose.caramel.sp_

/**
 * The launcher's modals and popups, ported from the pack's `launcher/Modals.kt`, `Popups.kt` and
 * `InstanceSettings.kt`.
 *
 * <p>The pack fills these with example rows. Here every list, name and count comes from
 * [CaramelLauncherState], which the activity fills from the live managers, and every action calls
 * back into the activity through [CaramelLauncherHost.Actions]. Where the pack drew a value the
 * client cannot measure (a ping, a "2 days ago"), the row is left out rather than invented.
 */

/** The Caramel Client app menu: the dropdown by the title bar's arrow. Quit lives here. */
@Composable
fun CaramelAppMenu(state: CaramelLauncherState, actions: CaramelLauncherHost.Actions) {
    if (!state.appMenu) return
    CaramelPopup(Alignment.TopStart, 14, 42, onDismiss = { state.appMenu = false }) {
        GlassPanel(Modifier.width(262.dp), RoundedCornerShape(18.dp), popup = true, elevation = 36.dp) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CaramelMenuItem(stringResource(R.string.caramel_menu_settings), Icons.Rounded.Settings, true) {
                    state.appMenu = false; state.tab = CaramelTab.Settings
                }
                CaramelMenuItem(stringResource(R.string.caramel_menu_instance_settings), Icons.Rounded.Tune) {
                    state.appMenu = false; state.instanceSettings = true
                }
                CaramelMenuItem(stringResource(R.string.caramel_menu_release_notes), Icons.Rounded.Article) {
                    state.appMenu = false; state.releaseNotesOpen = true
                }
                CaramelMenuItem(stringResource(R.string.caramel_menu_about), Icons.Rounded.Info) {
                    state.appMenu = false; state.aboutOpen = true
                }
                Box(Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 6.dp).height(1.dp).background(C.sheen(0.1f)))
                CaramelMenuItem(stringResource(R.string.caramel_menu_quit), Icons.Rounded.Close, danger = true) {
                    state.appMenu = false; actions.quit()
                }
            }
        }
    }
}

/** The account menu under the title-bar pill: the active account and its actions. */
@Composable
fun CaramelAccountMenu(state: CaramelLauncherState, actions: CaramelLauncherHost.Actions) {
    if (!state.accountMenu) return
    CaramelPopup(Alignment.TopEnd, -150, 44, onDismiss = { state.accountMenu = false }) {
        GlassPanel(Modifier.width(238.dp), RoundedCornerShape(20.dp), popup = true, elevation = 36.dp) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    Modifier.padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CaramelAvatarPicture(state.accountAvatarUrl, 44, online = state.signedIn)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (state.signedIn) state.accountName else stringResource(R.string.not_signed_in),
                            style = T.Subtitle,
                        )
                        if (state.signedIn) StatusLabel(stringResource(R.string.caramel_account_online), C.Green)
                    }
                }
                if (state.signedIn) {
                    CaramelMenuItem(stringResource(R.string.caramel_skin_cosmetics), Icons.Rounded.ViewInAr) {
                        state.accountMenu = false; state.tab = CaramelTab.Cosmetics
                    }
                    CaramelMenuItem(stringResource(R.string.caramel_account_settings_title), Icons.Rounded.Settings) {
                        state.accountMenu = false; state.accountSettings = true
                    }
                    CaramelMenuItem(stringResource(R.string.caramel_sign_out), Icons.Rounded.SwapHoriz, danger = true) {
                        state.accountMenu = false; actions.signOut()
                    }
                } else {
                    CaramelMenuItem(stringResource(R.string.caramel_add_account), Icons.Rounded.Add) {
                        state.accountMenu = false; state.addAccount = true
                    }
                    CaramelMenuItem(stringResource(R.string.caramel_menu_instance_settings), Icons.Rounded.Tune) {
                        state.accountMenu = false; state.instanceSettings = true
                    }
                }
            }
        }
    }
}

/** The Screenshots modal: a grid over the instance's real screenshot folder. */
@Composable
fun CaramelScreenshotsModal(state: CaramelLauncherState, actions: CaramelLauncherHost.Actions) {
    CaramelModal(state.screenshotsModal, { state.screenshotsModal = false }, Modifier.size(980.dp, 700.dp)) {
        var chip by remember { mutableStateOf(0) }
        val shots = state.screenshots
        Column(Modifier.fillMaxSize()) {
            CaramelModalHeader(
                stringResource(R.string.caramel_screenshots_title),
                stringResource(R.string.caramel_screenshots_sub, shots.size, state.profile.title),
                { state.screenshotsModal = false },
            ) {
                GlassButton(stringResource(R.string.caramel_open_folder), actions::openScreenshotsFolder, icon = Icons.Rounded.Folder, height = 32)
            }
            Row(Modifier.padding(start = 32.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(R.string.caramel_filter_all, R.string.caramel_filter_today, R.string.caramel_filter_this_week)
                    .forEachIndexed { i, res -> FilterChip(stringResource(res), chip == i, { chip = i }) }
            }
            if (shots.isEmpty()) {
                EmptyState(
                    stringResource(R.string.caramel_no_screenshots_title),
                    stringResource(R.string.caramel_no_screenshots_body),
                    Modifier.fillMaxWidth().weight(1f).padding(horizontal = 32.dp),
                )
            } else {
                LazyVerticalGrid(
                    GridCells.Fixed(4),
                    Modifier.weight(1f).padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(shots, key = { it.path }) { s ->
                        val selected = state.selectedScreenshot?.path == s.path
                        Column(
                            Modifier.clickable(remember { MutableInteractionSource() }, null) { state.selectedScreenshot = s },
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                Modifier.fillMaxWidth().height(118.dp).clip(RoundedCornerShape(14.dp))
                                    .border(if (selected) 2.dp else 1.dp, if (selected) C.Blue else C.sheen(0.16f), RoundedCornerShape(14.dp)),
                            ) {
                                CaramelScreenshotThumbnail(s.path, s.name, Modifier.fillMaxSize())
                            }
                            Text(s.name, style = T.Caption, maxLines = 1)
                        }
                    }
                }
            }
            CaramelRule()
            Row(
                Modifier.fillMaxWidth().padding(24.dp, 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val sel = state.selectedScreenshot
                Text(
                    stringResource(R.string.caramel_selected_count, if (sel != null) 1 else 0),
                    style = T.Label,
                    modifier = Modifier.weight(1f),
                )
                GlassButton(stringResource(R.string.caramel_open_folder), actions::openScreenshotsFolder, icon = Icons.Rounded.Folder)
                GlassButton(
                    stringResource(R.string.delete),
                    { sel?.let(actions::deleteScreenshot) },
                    icon = Icons.Rounded.Delete,
                    danger = true,
                )
            }
        }
    }
}

/**
 * The "all news" modal.
 *
 * <p>"View all news" used to be a label with no click, so the press did nothing. It now opens this
 * list of every changelog entry the Home row truncates to three; an unreadable feed leaves the list
 * empty and the modal shows its empty state rather than a fabricated post.
 */
@Composable
fun CaramelNewsModal(state: CaramelLauncherState, actions: CaramelLauncherHost.Actions) {
    CaramelModal(state.newsAll, { state.newsAll = false }, Modifier.size(760.dp, 700.dp)) {
        val posts = state.news
        Column(Modifier.fillMaxSize()) {
            CaramelModalHeader(
                stringResource(R.string.caramel_news_title),
                stringResource(R.string.caramel_news_sub),
                { state.newsAll = false },
            ) {
                GlassButton(
                    stringResource(R.string.caramel_view_all),
                    { state.newsAll = false; state.tab = CaramelTab.News },
                    icon = Icons.Rounded.Article,
                    height = 32,
                )
            }
            if (posts.isEmpty()) {
                EmptyState(
                    stringResource(R.string.caramel_no_news_title),
                    stringResource(R.string.caramel_no_news_body),
                    Modifier.fillMaxWidth().weight(1f).padding(horizontal = 32.dp),
                )
            } else {
                val tints = listOf(C.Caramel, C.Blue, C.Green, Color(0xFF966EFF), C.Red, Color(0xFF4AC8E6))
                LazyColumn(
                    Modifier.weight(1f).padding(horizontal = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(posts, key = { it.title + it.meta }) { post ->
                        val tint = tints[(post.title.hashCode() and 0x7fffffff) % tints.size]
                        GlassPanel(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), elevation = 6.dp) {
                            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Box(
                                    Modifier.size(150.dp, 106.dp).background(
                                        Brush.verticalGradient(listOf(tint.copy(0.5f), tint.copy(0.1f))),
                                        RoundedCornerShape(12.dp),
                                    ),
                                ) {
                                    if (post.imageRes != 0) {
                                        // Fit: the row's thumbnail is narrower than a banner, and
                                        // cropping it would cut the banner's own words.
                                        androidx.compose.foundation.Image(
                                            androidx.compose.ui.res.painterResource(post.imageRes),
                                            contentDescription = null,
                                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                        )
                                    }
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (post.severityLabel.isNotEmpty()) {
                                        StatusLabel(
                                            post.severityLabel,
                                            if (post.emergency) C.Red else Color(0xFFFFB300),
                                        )
                                    } else if (post.featured) {
                                        StatusLabel(stringResource(R.string.caramel_featured), C.Caramel)
                                    }
                                    Text(post.title, style = T.Subtitle.copy(fontSize = 15.sp_), maxLines = 2)
                                    Text(post.summary, style = T.Body, maxLines = 3)
                                    Text(post.meta, style = T.Caption)
                                    if (post.actionLabel.isNotEmpty() && post.actionUrl.isNotEmpty()) {
                                        PrimaryButton(
                                            post.actionLabel,
                                            { state.webTitle = post.title; state.webUrl = post.actionUrl },
                                            tone = Tone.Blue,
                                            icon = Icons.Rounded.OpenInNew,
                                            height = 34,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * The Release notes popup: what the build the user is running actually changed.
 *
 * <p>The menu item used to jump to the News tab, which is a feed of posts rather than this release's
 * notes, so "Release notes" did not show release notes. This reads the same bundled
 * `launcher/changelog.json` the first-run card does and groups it as Added / Changed / Fixed, with
 * the version, headline and date from the document. An unreadable document shows the empty line
 * rather than a fabricated list.
 */
@Composable
fun CaramelReleaseNotesModal(state: CaramelLauncherState) {
    CaramelModal(state.releaseNotesOpen, { state.releaseNotesOpen = false }, Modifier.size(680.dp, 640.dp)) {
        val notes = state.releaseNotes
        Column(Modifier.fillMaxSize()) {
            CaramelModalHeader(
                stringResource(R.string.caramel_menu_release_notes),
                if (notes.version.isEmpty()) null
                else stringResource(R.string.changelog_version, notes.version),
                { state.releaseNotesOpen = false },
            )
            if (notes.isEmpty) {
                EmptyState(
                    stringResource(R.string.changelog_title),
                    stringResource(R.string.changelog_popup_empty),
                    Modifier.fillMaxWidth().weight(1f).padding(horizontal = 32.dp),
                )
            } else {
                LazyColumn(
                    Modifier.weight(1f).padding(horizontal = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (notes.headline.isNotEmpty()) {
                                Text(notes.headline, style = T.Subtitle.copy(fontSize = 17.sp_))
                            }
                            if (notes.dateLine.isNotEmpty()) Text(notes.dateLine, style = T.Caption)
                            if (notes.summary.isNotEmpty()) Text(notes.summary, style = T.Body)
                        }
                    }
                    items(notes.sections, key = { it.title }) { section ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(releaseSectionLabel(section.title), style = T.Label.copy(color = C.Caramel))
                            section.lines.forEach { line ->
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(
                                        Modifier.padding(top = 7.dp).size(5.dp)
                                            .background(C.Text3, CircleShape),
                                    )
                                    Text(line, style = T.Body, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }
}

/** The translated heading for a release-note section key. */
@Composable
private fun releaseSectionLabel(key: String): String = stringResource(
    when (key) {
        CaramelReleaseNotesLoader.KIND_ADDED -> R.string.changelog_popup_added
        CaramelReleaseNotesLoader.KIND_CHANGED -> R.string.changelog_popup_changed
        CaramelReleaseNotesLoader.KIND_FIXED -> R.string.changelog_popup_fixed
        else -> R.string.changelog_popup_more
    },
)

/** The Friends modal: the active account's real Xbox Live friends, loaded by the activity. */
@Composable
fun CaramelFriendsModal(state: CaramelLauncherState, actions: CaramelLauncherHost.Actions) {
    CaramelModal(state.friendsAll, { state.friendsAll = false }, Modifier.size(620.dp, 700.dp)) {
        var q by remember { mutableStateOf("") }
        Column(Modifier.fillMaxSize()) {
            CaramelModalHeader(
                stringResource(R.string.caramel_friends),
                stringResource(R.string.caramel_friends_sub),
                { state.friendsAll = false },
            ) {
                GlassButton(null, { actions.refreshFriends() }, icon = Icons.Rounded.Refresh, height = 34)
                PrimaryButton(
                    stringResource(R.string.caramel_add_friend),
                    { state.friendsAll = false; state.addFriend = true },
                    Modifier.width(146.dp),
                    Tone.Blue,
                    Icons.Rounded.Add,
                    34,
                )
            }
            SearchField(q, { q = it }, stringResource(R.string.caramel_search_friends), Modifier.fillMaxWidth().padding(32.dp, 14.dp))
            if (!state.signedIn) {
                EmptyState(
                    stringResource(R.string.caramel_no_friends_title),
                    stringResource(R.string.caramel_no_friends_body),
                    Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp),
                )
            } else {
                val query = q.trim()
                val visible = if (query.isEmpty()) state.friends
                else state.friends.filter { it.gamertag.contains(query, ignoreCase = true) }
                if (visible.isEmpty()) {
                    val noneAtAll = state.friends.isEmpty()
                    val title = stringResource(
                        if (noneAtAll) R.string.caramel_no_friends_title
                        else R.string.caramel_no_friends_match
                    )
                    val matchBody = stringResource(R.string.caramel_no_friends_match_body)
                    val body = if (noneAtAll) {
                        stringResource(R.string.caramel_no_friends_body)
                    } else {
                        state.friendsStatus.ifEmpty { matchBody }
                    }
                    EmptyState(
                        title,
                        body,
                        Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp),
                    )
                } else {
                    LazyColumn(
                        Modifier.weight(1f).padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (state.friendsStatus.isNotEmpty()) {
                            item {
                                Text(state.friendsStatus, style = T.Body, modifier = Modifier.padding(start = 8.dp, top = 6.dp))
                            }
                        }
                        items(visible) { friend -> CaramelFriendRow(friend) }
                    }
                }
            }
        }
    }
}

/**
 * The Microsoft sign-in modal.
 *
 * <p>It carries the whole device-code flow: the button starts it, the code and the pre-filled link
 * appear as soon as Microsoft issues them, and a failure shows why instead of closing silently.
 * Before this the modal only ever showed a code the flow never had time to hand over, so the button
 * looked dead and the modal closed on its own.
 */
@Composable
fun CaramelAddAccountModal(state: CaramelLauncherState, actions: CaramelLauncherHost.Actions) {
    CaramelModal(state.addAccount, { state.addAccount = false }, Modifier.size(460.dp, 640.dp)) {
        Column(Modifier.fillMaxSize()) {
            CaramelModalHeader(
                stringResource(R.string.caramel_add_account_title),
                stringResource(R.string.caramel_add_account_sub),
                { state.addAccount = false },
            )
            Column(
                Modifier.weight(1f).padding(horizontal = 32.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp))
                        .background(if (state.signInRunning) Color.White.copy(0.6f) else Color.White)
                        .clickable(
                            remember { MutableInteractionSource() }, null,
                            enabled = !state.signInRunning,
                            onClick = actions::startSignIn,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        if (state.signInRunning) stringResource(R.string.caramel_sign_in_starting)
                        else stringResource(R.string.caramel_sign_in_microsoft),
                        style = T.Label.copy(color = Color(0xFF1C2446), fontSize = 14.5.sp_),
                    )
                }
                Text(
                    stringResource(R.string.caramel_or_device_code),
                    style = T.Caption,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0x78040A1A))
                        .border(1.dp, C.sheen(0.16f), RoundedCornerShape(18.dp)).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val code = state.deviceCode.ifEmpty { "\u2014\u2014\u2014\u2014" }
                    Text(code, style = T.Display.copy(fontSize = 30.sp_, letterSpacing = 3.sp_))
                    if (state.busyMessage.isNotEmpty()) Text(state.busyMessage, style = T.Body)
                    if (state.deviceCode.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GlassButton(
                                stringResource(R.string.ms_device_code_copy),
                                actions::copyDeviceCode,
                                icon = Icons.Rounded.ContentCopy,
                                height = 34,
                            )
                        }
                    }
                }
                if (state.signInError.isNotEmpty()) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .background(C.Red.copy(0.14f))
                            .border(1.dp, C.Red.copy(0.45f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Rounded.ErrorOutline, null, Modifier.size(16.dp), C.Red)
                        Text(
                            stringResource(R.string.ms_login_failed_detail, state.signInError),
                            style = T.Body.copy(color = C.Red),
                        )
                    }
                }
                Text(
                    stringResource(R.string.caramel_device_code_hint),
                    style = T.Body.copy(fontSize = 12.5.sp_, textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth(),
                )
                // The page Microsoft issued for this code, hosted in the app so the user never
                // leaves the launcher. A short "open in browser" escape hatch stays under it for the
                // sign-in steps an embedded WebView cannot finish.
                if (state.signInWebOpen && state.deviceCodeUrl.isNotEmpty()) {
                    Text(
                        stringResource(R.string.caramel_sign_in_page_title),
                        style = T.Label,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    CaramelSignInWebView(
                        state.deviceCodeUrl,
                        Modifier.fillMaxWidth().height(320.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, C.sheen(0.16f), RoundedCornerShape(16.dp)),
                    )
                    GlassButton(
                        stringResource(R.string.ms_device_code_open_browser),
                        actions::openDeviceCodePage,
                        icon = Icons.Rounded.Public,
                        height = 34,
                    )
                }
            }
            CaramelRule()
            Row(Modifier.fillMaxWidth().padding(24.dp, 18.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassButton(stringResource(R.string.cancel), { state.addAccount = false })
                Spacer(Modifier.weight(1f))
                Text(
                    stringResource(
                        if (state.signInRunning) R.string.caramel_waiting_sign_in
                        else R.string.caramel_sign_in_idle
                    ),
                    style = T.Body,
                )
            }
        }
    }
}

/** The account settings modal: privacy toggles over the signed-in account. */
@Composable
fun CaramelAccountSettingsModal(state: CaramelLauncherState, actions: CaramelLauncherHost.Actions) {
    CaramelModal(state.accountSettings, { state.accountSettings = false }, Modifier.size(720.dp, 680.dp)) {
        var status by remember { mutableStateOf(true) }
        var requests by remember { mutableStateOf(true) }
        var activity by remember { mutableStateOf(false) }
        Column(Modifier.fillMaxSize()) {
            CaramelModalHeader(
                stringResource(R.string.caramel_account_settings_title),
                null,
                { state.accountSettings = false },
            )
            CaramelRule()
            Column(Modifier.weight(1f).padding(32.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    CaramelAvatarPicture(state.accountAvatarUrl, 88, online = state.signedIn)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (state.signedIn) state.accountName else stringResource(R.string.not_signed_in),
                            style = T.Display.copy(fontSize = 19.sp_),
                        )
                        if (state.signedIn) StatusLabel(stringResource(R.string.caramel_microsoft_linked), C.Green)
                    }
                    GlassButton(stringResource(R.string.caramel_skin_cosmetics), { state.tab = CaramelTab.Cosmetics; state.accountSettings = false }, icon = Icons.Rounded.Public)
                }
                Text(stringResource(R.string.caramel_privacy), style = T.Subtitle.copy(color = C.Text2))
                SettingRow(stringResource(R.string.caramel_show_online_status), stringResource(R.string.caramel_show_online_status_desc)) {
                    CaramelToggle(status, { status = it })
                }
                SettingRow(stringResource(R.string.caramel_allow_requests), stringResource(R.string.caramel_allow_requests_desc)) {
                    CaramelToggle(requests, { requests = it })
                }
                SettingRow(stringResource(R.string.caramel_share_activity), stringResource(R.string.caramel_share_activity_desc)) {
                    CaramelToggle(activity, { activity = it })
                }
            }
            CaramelRule()
            Row(Modifier.fillMaxWidth().padding(24.dp, 18.dp)) {
                GlassButton(stringResource(R.string.caramel_sign_out), actions::signOut, icon = Icons.Rounded.SwapHoriz, danger = true)
                Spacer(Modifier.weight(1f))
                PrimaryButton(stringResource(R.string.save), { state.accountSettings = false }, Modifier.width(148.dp), height = 40)
            }
        }
    }
}

/**
 * The add-friend modal.
 *
 * <p>It used to be a shell: the field accepted text, a green "found" banner sat under it whether or
 * not anything had been found, and "Send request" only closed the modal. Now the lookup resolves the
 * typed gamertag against the real Xbox Live profile service, the found banner appears only when an
 * account was actually resolved, a Caramel badge appears beside it only when that account's xuid has
 * been heard on a Caramel-only protocol (see [org.chimeramc.client.core.cosmetics.CaramelUserDirectory]),
 * and "Send request" issues the real follow call.
 */
@Composable
fun CaramelAddFriendModal(state: CaramelLauncherState, actions: CaramelLauncherHost.Actions) {
    CaramelModal(state.addFriend, { state.addFriend = false; actions.resetFriendSearch() }, Modifier.size(430.dp, 420.dp)) {
        Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.caramel_add_friend_title), style = T.Title.copy(fontSize = 18.sp_))
                    Text(stringResource(R.string.caramel_add_friend_sub), style = T.Body)
                }
                GlassButton(null, { state.addFriend = false; actions.resetFriendSearch() }, icon = Icons.Rounded.Close, height = 32)
            }
            CaramelTextField(
                state.friendQuery,
                {
                    state.friendQuery = it
                    // A new name invalidates the previous result, so the found banner and the badge
                    // never describe the account that was typed before.
                    state.friendFound = null
                    state.friendFoundIsCaramel = false
                    state.friendSearchError = ""
                    state.friendRequestResult = ""
                },
                Modifier.fillMaxWidth(),
                stringResource(R.string.caramel_username),
                Icons.Rounded.Person,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton(
                    stringResource(if (state.friendSearchRunning) R.string.caramel_searching else R.string.caramel_search),
                    { actions.searchFriend(state.friendQuery) },
                    Modifier.weight(1f),
                    height = 38,
                )
            }
            val found = state.friendFound
            when {
                state.friendSearchError.isNotEmpty() -> ResultBanner(state.friendSearchError, C.Red, Icons.Rounded.Close)
                state.friendSearchRunning -> ResultBanner(stringResource(R.string.caramel_searching), C.Blue, Icons.Rounded.Search)
                found != null -> FoundAccountRow(found, state.friendFoundIsCaramel)
                state.friendSearchError.isEmpty() && state.friendQuery.trim().isNotEmpty() -> Unit
            }
            if (state.friendRequestResult.isNotEmpty()) {
                ResultBanner(
                    state.friendRequestResult,
                    if (state.friendRequestSucceeded) C.Green else C.Red,
                    if (state.friendRequestSucceeded) Icons.Rounded.Check else Icons.Rounded.Close,
                )
            }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                GlassButton(stringResource(R.string.cancel), { state.addFriend = false; actions.resetFriendSearch() }, Modifier.width(140.dp), height = 38)
                PrimaryButton(
                    stringResource(R.string.caramel_send_request),
                    { actions.sendFriendRequest() },
                    Modifier.weight(1f),
                    height = 38,
                )
            }
        }
    }
}

/** The resolved account: its picture, gamertag and, only when confirmed, the Caramel badge. */
@Composable
private fun FoundAccountRow(user: CaramelFoundUser, isCaramel: Boolean) {
    Row(
        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(12.dp)).background(C.Green.copy(0.11f))
            .border(1.dp, C.Green.copy(0.4f), RoundedCornerShape(12.dp)).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CaramelAvatarPicture(user.avatarUrl, 30)
        Text(user.gamertag, style = T.Label.copy(fontWeight = FontWeight.Medium), modifier = Modifier.weight(1f))
        if (isCaramel) CaramelUserBadge()
        Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), C.Green)
    }
}

/** The CaramelClient mark beside a confirmed Caramel user's name. */
@Composable
private fun CaramelUserBadge() {
    CaramelUserChip()
}

/** A one-line result banner with a leading icon, used for lookup and request outcomes. */
@Composable
private fun ResultBanner(text: String, tint: Color, icon: ImageVector) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(tint.copy(0.11f))
            .border(1.dp, tint.copy(0.4f), RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, null, Modifier.size(15.dp), tint)
        Text(text, style = T.Label.copy(fontWeight = FontWeight.Medium, fontSize = 12.5.sp_))
    }
}

/**
 * Superseded by [CaramelSettingsTabScreen]: settings is a sidebar tab now, so the gear selects a
 * real destination instead of opening a popup over the active tab. The panel is the same
 * [CaramelSettingsPanel] over the activity's [CaramelSettingsProvider], so nothing about the controls
 * changed — only where they are hosted.
 */

/** The instance settings modal: a section rail over the real instance's options. */
@Composable
fun CaramelInstanceSettingsModal(state: CaramelLauncherState, actions: CaramelLauncherHost.Actions) {
    CaramelModal(state.instanceSettings, { state.instanceSettings = false }, Modifier.size(900.dp, 670.dp)) {
        var section by remember { mutableStateOf(0) }
        var name by remember(state.instanceName) { mutableStateOf(state.instanceName) }
        val sections = listOf(
            R.string.caramel_section_general to Icons.Rounded.Tune,
            R.string.caramel_section_game to Icons.Rounded.SportsEsports,
            R.string.caramel_section_performance to Icons.Rounded.Bolt,
            R.string.caramel_section_mods to Icons.Rounded.Extension,
            R.string.caramel_section_advanced to Icons.Rounded.Settings,
        )
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 32.dp, end = 24.dp, top = 24.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.caramel_menu_instance_settings), style = T.Display.copy(fontSize = 20.sp_, fontWeight = FontWeight.Medium))
                    Text(
                        stringResource(R.string.caramel_instance_settings_sub, state.instanceName, state.profile.title),
                        style = T.Body,
                    )
                }
                GlassButton(null, { state.instanceSettings = false }, icon = Icons.Rounded.Close, height = 32)
            }
            CaramelRule()
            Row(Modifier.weight(1f)) {
                Column(Modifier.width(216.dp).padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sections.forEachIndexed { i, (titleRes, icon) ->
                        CaramelSideItem(stringResource(titleRes), icon, i == section) { section = i }
                    }
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(C.sheen(0.08f)))
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(28.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    when (section) {
                        0 -> {
                            CaramelField(stringResource(R.string.caramel_instance_name)) {
                                CaramelTextField(name, { name = it }, Modifier.fillMaxWidth())
                            }
                            SettingRow(stringResource(R.string.caramel_game_version)) {
                                CaramelVersionDropdown(
                                    state.versions,
                                    state.version,
                                    onSelect = { actions.selectVersion(it.id) },
                                    onImportInstance = { actions.importInstance() },
                                )
                            }
                            SettingRow(stringResource(R.string.caramel_loader)) {
                                val loaderUnavailable =
                                    stringResource(R.string.caramel_loader_unavailable)
                                val roadLoaderUnavailable =
                                    stringResource(R.string.caramel_roadloader_soon)
                                org.chimeramc.client.ui.compose.caramel.CaramelLoaderPicker(
                                    state.loader,
                                    { state.loader = it },
                                    onUnavailable = {
                                        state.notice = CaramelNotice(loaderUnavailable, warning = true)
                                    },
                                    onInDevelopment = {
                                        state.notice = CaramelNotice(roadLoaderUnavailable, warning = true)
                                    },
                                )
                            }
                            SettingRow(stringResource(R.string.caramel_profile_mode), stringResource(R.string.caramel_profile_open_desc)) {
                                CaramelProfileDropdown(state.profile, { state.profile = it }, width = 240)
                            }
                        }
                        1 -> {
                            Text(stringResource(R.string.caramel_section_game), style = T.Title)
                            SettingRow(
                                stringResource(R.string.caramel_instance_launch_vertical),
                                stringResource(R.string.caramel_instance_launch_vertical_desc),
                            ) { CaramelToggle(state.instanceLaunchVertical, { actions.setInstanceLaunchVertical(it) }) }
                            SettingRow(
                                stringResource(R.string.caramel_instance_pack_changer),
                                stringResource(R.string.caramel_instance_pack_changer_desc),
                            ) { CaramelToggle(state.instancePackChanger, { actions.setInstancePackChanger(it) }) }
                        }
                        2 -> CaramelPerformanceTab(state, actions)
                        3 -> {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    stringResource(R.string.enabled_mods) + " " +
                                        state.mods.count { it.enabled } + "/" + state.mods.size,
                                    style = T.Label,
                                )
                                Box(Modifier.weight(1f))
                                GlassButton(stringResource(R.string.caramel_open_mods_folder), actions::openModsFolder, icon = Icons.Rounded.Folder)
                            }
                            if (state.mods.isEmpty()) {
                                EmptyState(
                                    stringResource(R.string.empty_mods_title),
                                    stringResource(R.string.empty_mods_message),
                                    Modifier.fillMaxWidth(),
                                )
                            } else {
                                state.mods.forEach { m ->
                                    Row(
                                        Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(C.sheen(0.06f))
                                            .border(1.dp, C.sheen(0.12f), RoundedCornerShape(13.dp)).padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Box(Modifier.size(28.dp).background(C.Blue.copy(0.25f), RoundedCornerShape(8.dp)), Alignment.Center) {
                                            Icon(Icons.Rounded.Extension, null, Modifier.size(15.dp), C.Blue)
                                        }
                                        Column(Modifier.weight(1f)) {
                                            Text(m.name, style = T.Label)
                                            Text(
                                                if (m.version.isEmpty()) m.author else stringResource(R.string.caramel_mod_version, m.version),
                                                style = T.Caption,
                                            )
                                        }
                                        CaramelToggle(m.enabled, { actions.toggleMod(m, it) })
                                    }
                                }
                            }
                        }
                        else -> {
                            Text(stringResource(R.string.caramel_section_advanced), style = T.Title)
                            SettingRow(
                                stringResource(R.string.caramel_instance_shader_compat),
                                stringResource(R.string.caramel_instance_shader_compat_desc),
                            ) { CaramelToggle(state.instanceShaderCompat, { actions.setInstanceShaderCompat(it) }) }
                            SettingRow(
                                stringResource(R.string.caramel_instance_isolation),
                                stringResource(R.string.caramel_instance_isolation_desc),
                            ) { CaramelToggle(state.instanceIsolation, { actions.setInstanceIsolation(it) }) }
                            Text(stringResource(R.string.caramel_instance_advanced_note), style = T.Body)
                        }
                    }
                }
            }
            CaramelRule()
            Row(
                Modifier.fillMaxWidth().padding(24.dp, 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassButton(stringResource(R.string.caramel_open_folder), actions::openInstanceFolder, icon = Icons.Rounded.Folder)
                if (state.instanceEditable) {
                    GlassButton(stringResource(R.string.caramel_repair), actions::repairInstance, icon = Icons.Rounded.Build)
                    GlassButton(stringResource(R.string.delete), actions::deleteInstance, icon = Icons.Rounded.Delete, danger = true)
                }
                Spacer(Modifier.weight(1f))
                PrimaryButton(stringResource(R.string.caramel_save_changes), {
                    if (name.isNotBlank() && name != state.instanceName) actions.renameInstance(name)
                    state.instanceSettings = false
                }, Modifier.width(148.dp), height = 40)
            }
        }
    }
}

/** A glass popup anchored at [x]/[y], from the pack's `Popup` helper. */
@Composable
private fun CaramelPopup(
    alignment: Alignment,
    x: Int,
    y: Int,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    androidx.compose.ui.window.Popup(
        alignment = alignment,
        offset = androidx.compose.ui.unit.IntOffset(x, y),
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.PopupProperties(focusable = true),
        content = { content() },
    )
}

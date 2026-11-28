package org.chimeramc.client.ui.compose.caramel.launcher

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.chimeramc.client.ui.compose.caramel.CaramelLauncherState
import org.chimeramc.client.ui.compose.caramel.CaramelModal
import org.chimeramc.client.ui.compose.caramel.CaramelSettingsProvider
import org.chimeramc.client.ui.compose.caramel.CaramelTab
import org.chimeramc.client.ui.compose.caramel.CaramelToast

/**
 * The whole Caramel launcher, ported from the pack's `CaramelApp` in `launcher/Shell.kt`.
 *
 * <p>The frame (backdrop, title bar, sidebar) hosts the active tab, and the overlays and modals sit
 * above it. This is the single composable the activity mounts through [CaramelLauncherHost].
 */
@Composable
fun CaramelLauncherScreen(
    state: CaramelLauncherState,
    actions: CaramelLauncherHost.Actions,
    skinViewer: (@Composable (Modifier) -> Unit)? = null,
    settings: CaramelSettingsProvider? = null,
) {
    val cosmeticsSkinViewer = skinViewer ?: { m: Modifier -> CaramelSkinPreview(state, m) }
    Box(Modifier.fillMaxSize()) {
        CaramelLauncherFrame(state, actions::joinServer) { contentModifier ->
            Box(contentModifier) {
                when (state.tab) {
                    CaramelTab.Home -> CaramelHomeScreen(state, actions::launch, actions::openScreenshots)
                    CaramelTab.Mods -> CaramelModsScreen(
                        state,
                        onToggle = actions::toggleMod,
                        onOpenFolder = actions::openModsFolder,
                    )
                    CaramelTab.Servers -> CaramelServersScreen(
                        state,
                        onJoin = actions::joinServer,
                        onFavourite = actions::toggleServerFavourite,
                        onAddServer = actions::addServer,
                        onDeleteServer = actions::deleteServer,
                    )
                    CaramelTab.Cosmetics -> CaramelCosmeticsScreen(
                        state,
                        onToggleFavourite = actions::toggleFavourite,
                        onEquip = actions::equipCosmetic,
                        onResetView = {},
                        skinViewer = cosmeticsSkinViewer,
                    )
                    CaramelTab.News -> CaramelNewsScreen(state)
                    CaramelTab.CurseForge -> CaramelCurseForgeScreen(
                        state,
                        onBrowse = { section, query -> actions.searchCurseForge(query, section.classId) },
                        onDownload = actions::downloadCurseForge,
                    )
                    CaramelTab.Store -> CaramelStoreScreen(state)
                    CaramelTab.Settings -> CaramelSettingsTabScreen(settings)
                }
            }
        }
        CaramelLaunchMenuOverlay(
            state,
            actions::launch,
            onSelectVersion = actions::selectVersion,
            onImportInstance = actions::importInstance,
        )
        CaramelModDetailModal(state, actions::toggleMod)
        CaramelAppMenu(state, actions)
        CaramelAccountMenu(state, actions)
        CaramelScreenshotsModal(state, actions)
        CaramelNewsModal(state, actions)
        CaramelReleaseNotesModal(state)
        CaramelAboutModal(state)
        CaramelFriendsModal(state, actions)
        CaramelAddAccountModal(state, actions)
        CaramelAccountSettingsModal(state, actions)
        CaramelAddFriendModal(state, actions)
        CaramelModsFolderModal(state, actions)
        CaramelAddServerModal(state, actions)
        CaramelInstanceSettingsModal(state, actions)
        CaramelWebModal(state)
        CaramelImportProgressModal(state)
        CaramelToast(state)
    }
}

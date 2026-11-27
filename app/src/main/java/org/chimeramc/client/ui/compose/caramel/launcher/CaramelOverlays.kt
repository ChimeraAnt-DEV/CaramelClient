package org.chimeramc.client.ui.compose.caramel.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.chimeramc.client.R
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.chimeramc.client.ui.compose.caramel.C
import org.chimeramc.client.ui.compose.caramel.CaramelLauncherState
import org.chimeramc.client.ui.compose.caramel.CaramelLoader
import org.chimeramc.client.ui.compose.caramel.CaramelModInfo
import org.chimeramc.client.ui.compose.caramel.CaramelToggle
import org.chimeramc.client.ui.compose.caramel.CaramelModal
import org.chimeramc.client.ui.compose.caramel.CaramelProfileDropdown
import org.chimeramc.client.ui.compose.caramel.CaramelVersionDropdown
import org.chimeramc.client.ui.compose.caramel.GlassButton
import org.chimeramc.client.ui.compose.caramel.GlassPanel
import org.chimeramc.client.ui.compose.caramel.CaramelLoaderPicker
import org.chimeramc.client.ui.compose.caramel.CaramelNotice
import org.chimeramc.client.ui.compose.caramel.PrimaryButton
import org.chimeramc.client.ui.compose.caramel.T
import org.chimeramc.client.ui.compose.caramel.sp_

/**
 * The pack's launch menu, its mod-detail modal and the Caramel Client app menu.
 *
 * <p>The pack anchors the launch menu to a fixed desktop offset. On a phone the surface is a
 * different shape, so the menu is centred as a popup; its contents (version, loaders, profile
 * mode, launch) are the pack's.
 */
@Composable
fun CaramelLaunchMenuOverlay(
    state: CaramelLauncherState,
    onLaunch: () -> Unit,
    onSelectVersion: (String) -> Unit,
    onImportInstance: () -> Unit,
) {
    if (!state.launchMenu) return
    // A tap outside a menu must not throw it away — the same rule the modals follow, and the one the
    // user asked for: a menu closes from its own X, or from the system back gesture. The scrim still
    // swallows the tap so it cannot reach the screen behind.
    BackHandler(enabled = state.launchMenu) { state.launchMenu = false }
    Popup(
        alignment = Alignment.Center,
        onDismissRequest = {},
        properties = PopupProperties(focusable = true),
    ) {
        GlassPanel(Modifier.width(372.dp), RoundedCornerShape(22.dp), popup = true, elevation = 40.dp) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.caramel_version), style = T.Title.copy(fontSize = 15.sp_), modifier = Modifier.weight(1f))
                    CaramelVersionDropdown(
                        state.versions,
                        state.version,
                        onSelect = { onSelectVersion(it.id) },
                        onImportInstance = onImportInstance,
                    )
                    Spacer(Modifier.width(8.dp))
                    GlassButton(null, { state.launchMenu = false }, icon = Icons.Rounded.Close, height = 32)
                }
                // The label is top-aligned with the tile row, not centred on the tiles *plus* the
                // note: centring on the whole picker pushed "Loaders" down beside the note, which
                // read as if the title sat below the icons.
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        stringResource(R.string.caramel_loaders),
                        style = T.Title.copy(fontSize = 15.sp_),
                        modifier = Modifier.weight(1f).padding(top = 13.dp),
                    )
                    Box(Modifier.width(240.dp)) {
                        val loaderUnavailable =
                            stringResource(R.string.caramel_loader_unavailable)
                        val roadLoaderUnavailable =
                            stringResource(R.string.caramel_roadloader_soon)
                        CaramelLoaderPicker(
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
                }
                CaramelProfileDropdown(state.profile, { state.profile = it }, width = 328)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassButton(null, { state.launchMenu = false; state.instanceSettings = true }, icon = Icons.Rounded.Settings, height = 44)
                    PrimaryButton(stringResource(R.string.caramel_launch_game_action), { state.launchMenu = false; onLaunch() }, Modifier.weight(1f), icon = Icons.Rounded.RocketLaunch)
                }
            }
        }
    }
}

/** The mod detail modal, opened by tapping a Mods card. */
@Composable
fun CaramelModDetailModal(state: CaramelLauncherState, onToggle: (CaramelModInfo, Boolean) -> Unit) {
    val mod = state.selectedMod
    CaramelModal(mod != null, { state.selectedMod = null }, Modifier.width(520.dp)) {
        if (mod == null) return@CaramelModal
        Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(mod.name, style = T.Display.copy(fontSize = 20.sp_, fontWeight = FontWeight.Medium))
                    Text(stringResource(R.string.caramel_mod_by, mod.author) + " \u00b7 " + stringResource(R.string.caramel_mod_version, mod.version), style = T.Body)
                }
                GlassButton(null, { state.selectedMod = null }, icon = Icons.Rounded.Close, height = 32)
            }
            Text(mod.description, style = T.Body.copy(fontSize = 13.sp_))
            if (!mod.runnable) {
                Text(stringResource(R.string.caramel_state_preview_only_note), style = T.Body.copy(fontSize = 12.sp_, color = C.Orange))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(if (mod.enabled) R.string.caramel_state_enabled else R.string.caramel_state_disabled), style = T.Label, modifier = Modifier.weight(1f))
                CaramelToggle(mod.enabled, { onToggle(mod, it) })
            }
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassButton(stringResource(R.string.caramel_open_config), {}, Modifier.weight(1f), height = 40)
                PrimaryButton(stringResource(if (mod.enabled) R.string.caramel_state_disabled else R.string.caramel_state_enabled), { onToggle(mod, !mod.enabled) }, Modifier.weight(1f), height = 40)
            }
        }
    }
}

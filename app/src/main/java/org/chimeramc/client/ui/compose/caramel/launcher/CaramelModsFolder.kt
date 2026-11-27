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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.chimeramc.client.R
import org.chimeramc.client.ui.compose.caramel.C
import org.chimeramc.client.ui.compose.caramel.CaramelFileEntry
import org.chimeramc.client.ui.compose.caramel.CaramelLauncherState
import org.chimeramc.client.ui.compose.caramel.CaramelModal
import org.chimeramc.client.ui.compose.caramel.GlassButton
import org.chimeramc.client.ui.compose.caramel.T
import org.chimeramc.client.ui.compose.caramel.sp_

/**
 * The mods-folder browser.
 *
 * <p>"Open mods folder" used to hand the directory to a file manager; on a device with none the
 * press did nothing, and even where one existed the user only ever saw the mods the loader
 * recognised. This draws the folder's real contents in-app, with a breadcrumb, an up button and a
 * row per file or sub-directory, so every file in the instance is visible and reachable.
 */
@Composable
fun CaramelModsFolderModal(state: CaramelLauncherState, actions: CaramelLauncherHost.Actions) {
    CaramelModal(state.modsFolderOpen, { state.modsFolderOpen = false }, Modifier.size(760.dp, 620.dp)) {
        Column(Modifier.fillMaxSize()) {
            CaramelModalHeader(
                stringResource(R.string.caramel_mods_folder_title),
                stringResource(R.string.caramel_mods_folder_sub),
                { state.modsFolderOpen = false },
            ) {
                GlassButton(
                    stringResource(R.string.caramel_mods_folder_in_file_manager),
                    actions::openModsFolder,
                    icon = Icons.Rounded.FolderOpen,
                    height = 32,
                )
            }
            CaramelRule()
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GlassButton(
                    stringResource(R.string.caramel_mods_folder_up),
                    actions::openModsFolderUp,
                    icon = Icons.Rounded.ArrowUpward,
                    height = 32,
                    enabled = state.modsFolderCanGoUp,
                )
                Text(
                    "mods/" + state.modsFolderPath.trimStart('/'),
                    style = T.Body.copy(fontSize = 12.5.sp_),
                    modifier = Modifier.weight(1f),
                )
            }
            val entries = state.modsFolderEntries
            if (entries.isEmpty()) {
                Box(Modifier.weight(1f).padding(28.dp)) {
                    EmptyState(
                        stringResource(R.string.caramel_mods_folder_empty),
                        stringResource(R.string.caramel_mods_folder_empty_body),
                        Modifier.fillMaxSize(),
                    )
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f).padding(horizontal = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(entries, key = { it.path }) { entry -> ModsFolderRow(entry, actions) }
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun ModsFolderRow(entry: CaramelFileEntry, actions: CaramelLauncherHost.Actions) {
    Row(
        Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(13.dp))
            .background(C.sheen(0.06f))
            .border(1.dp, C.sheen(0.12f), RoundedCornerShape(13.dp))
            .clickable(remember { MutableInteractionSource() }, null) { actions.openModsFolderEntry(entry) }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(30.dp).background(
                (if (entry.directory) C.Caramel else C.Blue).copy(0.22f),
                RoundedCornerShape(9.dp),
            ),
            Alignment.Center,
        ) {
            Icon(
                if (entry.directory) Icons.Rounded.Folder else Icons.Rounded.Description,
                null,
                Modifier.size(16.dp),
                if (entry.directory) C.Caramel else C.Blue,
            )
        }
        Text(entry.name, style = T.Label, modifier = Modifier.weight(1f))
        Text(
            if (entry.directory) stringResource(R.string.caramel_folder_label) else entry.sizeLabel,
            style = T.Caption,
        )
    }
}

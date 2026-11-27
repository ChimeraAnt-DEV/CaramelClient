package org.chimeramc.client.ui.compose.caramel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import org.chimeramc.client.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * The Caramel dropdowns, ported from the pack's `components/Dropdowns.kt`.
 *
 * <p>A closed trigger plus a glass popup. Compose's `Popup` exists on Android, so the pack's
 * mechanism ports unchanged; only the glass surface underneath it is the Android [GlassPanel]
 * rather than the pack's Haze one.
 */

/** A closed dropdown trigger plus a generic glass popup, anchored under the trigger. */
@Composable
fun CaramelDropdown(
    value: String,
    modifier: Modifier = Modifier,
    width: Int = 180,
    panelWidth: Int = width,
    panel: @Composable (close: () -> Unit) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        CaramelDropdownField(value, open, Modifier.width(width.dp)) { open = !open }
        if (open) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, 46),
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true),
            ) {
                GlassPanel(Modifier.width(panelWidth.dp), RoundedCornerShape(16.dp), popup = true, elevation = 32.dp) {
                    panel { open = false }
                }
            }
        }
    }
}

/** The closed trigger itself: a value, a chevron, and an open/closed border. */
@Composable
fun CaramelDropdownField(text: String, open: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(CaramelDimens.RadiusControl)
    Row(
        modifier
            .height(38.dp)
            .clip(shape)
            .background(C.sheen(if (open) 0.13f else 0.08f))
            .border(1.dp, if (open) C.Blue.copy(0.7f) else C.sheen(0.18f), shape)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = T.Label.copy(fontWeight = FontWeight.Medium), modifier = Modifier.weight(1f))
        Icon(if (open) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, null, Modifier.size(18.dp), C.Text2)
    }
}

/** A plain string list dropdown. */
@Composable
fun CaramelSimpleDropdown(
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    width: Int = 190,
) = CaramelDropdown(value, modifier, width) { close ->
    Column(Modifier.padding(8.dp)) {
        options.forEach { o ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (o == value) C.Blue.copy(0.24f) else Color.Transparent)
                    .clickable { onSelect(o); close() }
                    .padding(horizontal = 12.dp),
                Alignment.CenterStart,
            ) { Text(o, style = T.Label) }
        }
    }
}

/**
 * The version picker: a search box over a scrolling list, each row carrying its channel chip.
 *
 * <p>The pack filters a hardcoded list; this filters the list the activity supplies, so the picker
 * can only ever show a version that is actually installed. [onImportInstance] adds the import row
 * under the list, which is where a user looks for "a version I do not have yet" — the launcher
 * imports an `.apk` / `.xapk` package into a new sandboxed instance.
 */
@Composable
fun CaramelVersionDropdown(
    versions: List<CaramelVersion>,
    selected: CaramelVersion?,
    onSelect: (CaramelVersion) -> Unit,
    modifier: Modifier = Modifier,
    onImportInstance: (() -> Unit)? = null,
) = CaramelDropdown(selected?.id ?: "", modifier, width = 124, panelWidth = 300) { close ->
    var q by remember { mutableStateOf("") }
    val list = remember(q, versions) { versions.filter { it.id.contains(q.trim()) } }
    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SearchField(q, { q = it }, "Search versions", Modifier.fillMaxWidth())
        LazyColumn(Modifier.heightIn(max = 252.dp)) {
            items(list, key = { it.id }) { v ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (v == selected) C.Blue.copy(0.26f) else Color.Transparent)
                        .clickable { onSelect(v); close() }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(v.id, style = T.Label, modifier = Modifier.weight(1f))
                    ChannelLabel(v.channel.label, v.channel.color)
                }
            }
            if (list.isEmpty()) item { Text(stringResource(R.string.caramel_no_versions_match), style = T.Body, modifier = Modifier.padding(12.dp)) }
        }
        if (onImportInstance != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(C.Blue.copy(0.16f))
                    .clickable { close(); onImportInstance() }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Rounded.FileDownload, null, Modifier.size(18.dp), C.Blue)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.caramel_import_instance), style = T.Label)
                    Text(stringResource(R.string.caramel_import_instance_sub), style = T.Caption)
                }
            }
        }
    }
}

/** The profile picker: an Open (green, unlocked) or Isolated (red, locked) instance profile. */
@Composable
fun CaramelProfileDropdown(
    selected: CaramelProfileMode,
    onSelect: (CaramelProfileMode) -> Unit,
    modifier: Modifier = Modifier,
    width: Int = 328,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        val shape = RoundedCornerShape(CaramelDimens.RadiusControl)
        Row(
            Modifier
                .width(width.dp)
                .height(38.dp)
                .clip(shape)
                .background(C.sheen(if (open) 0.13f else 0.08f))
                .border(1.dp, if (open) C.Blue.copy(0.7f) else C.sheen(0.18f), shape)
                .clickable(remember { MutableInteractionSource() }, null) { open = !open }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Rounded.Folder, null, Modifier.size(17.dp), Color(0xFF50BEFF))
            Text(
                if (selected == CaramelProfileMode.Open) stringResource(R.string.caramel_profile_open) else stringResource(R.string.caramel_profile_isolated),
                style = T.Label.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Rounded.SwapHoriz, null, Modifier.size(16.dp), C.Text3)
        }
        if (open) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, 46),
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true),
            ) {
                GlassPanel(Modifier.width(width.dp), RoundedCornerShape(16.dp), popup = true, elevation = 32.dp) {
                    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        CaramelProfileMode.entries.forEach { m ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(CaramelDimens.RadiusControl))
                                    .background(if (m == selected) C.Blue.copy(0.22f) else Color.Transparent)
                                    .clickable { onSelect(m); open = false }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(m.title, style = T.Label)
                                    Text(m.desc, style = T.Body)
                                }
                                ProfileLabel(m.locked)
                            }
                        }
                    }
                }
            }
        }
    }
}

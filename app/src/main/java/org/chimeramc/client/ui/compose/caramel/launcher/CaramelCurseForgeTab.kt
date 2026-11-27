package org.chimeramc.client.ui.compose.caramel.launcher

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.chimeramc.client.R
import org.chimeramc.client.ui.compose.caramel.C
import org.chimeramc.client.ui.compose.caramel.CaramelCurseForgeItem
import org.chimeramc.client.ui.compose.caramel.CaramelLauncherState
import org.chimeramc.client.ui.compose.caramel.GlassButton
import org.chimeramc.client.ui.compose.caramel.GlassPanel
import org.chimeramc.client.ui.compose.caramel.SearchField
import org.chimeramc.client.ui.compose.caramel.T
import org.chimeramc.client.ui.compose.caramel.Tone
import org.chimeramc.client.ui.compose.caramel.PrimaryButton
import org.chimeramc.client.ui.compose.caramel.sp_
import java.util.Locale

/**
 * The CurseForge tab: search the public CurseForge catalogue and import what it returns.
 *
 * <p>CurseForge needs an API key, but not from the user: the client ships its own default key and
 * only exposes the key field under Developer mode. The search itself is a plain text query — no
 * class filter — because Bedrock content is spread across several CurseForge categories and a
 * filter would hide the very add-ons this tab exists for.
 *
 * <p>A result is downloadable only when the API returned a file with a URL. The button is disabled
 * otherwise rather than downloading nothing.
 */
@Composable
fun CaramelCurseForgeScreen(
    state: CaramelLauncherState,
    onSearch: (String) -> Unit,
    onDownload: (CaramelCurseForgeItem) -> Unit,
) {
    var query by remember { mutableStateOf(state.curseForgeQuery) }
    CaramelPage(stringResource(R.string.caramel_curseforge_title), stringResource(R.string.caramel_curseforge_sub)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SearchField(query, { query = it }, stringResource(R.string.caramel_curseforge_search_hint), Modifier.width(360.dp))
            PrimaryButton(
                stringResource(R.string.caramel_curseforge_search),
                { onSearch(query.trim()) },
                Modifier.width(150.dp),
                tone = Tone.Caramel,
                icon = Icons.Rounded.Download,
                height = 40,
                muted = state.curseForgeBusy,
            )
            if (state.curseForgeBusy) {
                CircularProgressIndicator(
                    color = C.Caramel,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        if (state.curseForgeStatus.isNotEmpty()) {
            Text(state.curseForgeStatus, style = T.Body.copy(color = if (state.curseForgeResults.isEmpty()) C.Text2 else C.Text3))
        }
        if (state.curseForgeResults.isEmpty()) {
            EmptyState(
                stringResource(
                    if (state.curseForgeSearched) R.string.caramel_curseforge_no_results_title
                    else R.string.caramel_curseforge_empty_title
                ),
                stringResource(
                    if (state.curseForgeSearched) R.string.caramel_curseforge_no_results_body
                    else R.string.caramel_curseforge_empty_body
                ),
                Modifier.fillMaxWidth().weight(1f),
            )
        } else {
            LazyColumn(
                Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.curseForgeResults, key = { it.id }) { item ->
                    CurseForgeRow(
                        item,
                        downloading = state.curseForgeDownloadingId == item.id,
                        enabled = !state.curseForgeBusy,
                        onDownload = { onDownload(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CurseForgeRow(
    item: CaramelCurseForgeItem,
    downloading: Boolean,
    enabled: Boolean,
    onDownload: () -> Unit,
) {
    GlassPanel(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), elevation = 6.dp) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(13.dp))
                    .background(C.Orange.copy(0.16f))
                    .border(1.dp, C.Orange.copy(0.5f), RoundedCornerShape(13.dp)),
                Alignment.Center,
            ) {
                Image(
                    painterResource(R.drawable.ic_brand_curseforge),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        item.name,
                        style = T.Subtitle.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        stringResource(R.string.caramel_curseforge_downloads, formatCount(item.downloads)),
                        style = T.Caption,
                    )
                }
                Text(
                    if (item.author.isNotEmpty()) stringResource(R.string.caramel_mod_by, item.author) else "",
                    style = T.Caption,
                )
                Text(item.summary, style = T.Body, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(6.dp))
            if (downloading) {
                CircularProgressIndicator(
                    color = C.Caramel,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp),
                )
            } else {
                GlassButton(
                    stringResource(R.string.caramel_curseforge_download),
                    onDownload,
                    icon = Icons.Rounded.Download,
                    active = item.downloadable,
                    enabled = enabled && item.downloadable,
                    height = 36,
                )
            }
        }
    }
}

/** "1.2M", "34.5k", "912": a download count a person can read at a glance. */
private fun formatCount(value: Long): String {
    if (value >= 1_000_000L) return String.format(Locale.ROOT, "%.1fM", value / 1_000_000.0)
    if (value >= 1_000L) return String.format(Locale.ROOT, "%.1fk", value / 1_000.0)
    return value.toString()
}

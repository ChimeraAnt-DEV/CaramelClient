package org.chimeramc.client.ui.compose.modmenu

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.chimeramc.client.R
import org.chimeramc.client.ui.compose.theme.GlowDimens
import org.chimeramc.client.ui.compose.theme.glowColors
import org.chimeramc.client.ui.compose.theme.glowMotion

/**
 * The rebuilt Mod Menu: a glassmorphic sidebar with four destinations and a content pane.
 *
 * <p>Every surface is translucent glass rather than a flat fill, so the game behind the overlay
 * stays legible; the rim catches light from above and the sheen falls off downward, which is what
 * makes a translucent panel read as glass instead of as a washed-out rectangle. The sidebar icons
 * are Material's detailed outlined set at a legible size, not the small glyphs the old rail used.
 *
 * <p>Pure presentation: the state arrives as one immutable [ModMenuState] and every interaction is
 * a callback, so the menu can be rendered and reviewed without a device or a running game.
 */
@Composable
fun ModMenu(
    state: ModMenuState,
    onDestination: (ModMenuDestination) -> Unit,
    onSearch: (String) -> Unit,
    onToggleModule: (String, Boolean) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleFavoritesOnly: () -> Unit,
    onToggleSetting: (String, Boolean) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = glowColors
    // The whole panel is one translucent glass sheet. 25% by default, the value the design calls
    // for: enough to read the game through it, enough tint to keep the text legible.
    val alpha = (state.opacityPercent.coerceIn(5, 100) / 100f)
    val panelBase = (if (state.glassTint != 0) Color(state.glassTint) else colors.surface)
        .copy(alpha = alpha)

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(GlowDimens.SPACE_4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(GlowDimens.CARD_RADIUS.dp))
                .background(panelBase)
                .background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.06f),
                        0.45f to Color.Transparent,
                    ),
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(GlowDimens.CARD_RADIUS.dp),
                ),
        ) {
            ModMenuSidebar(
                destination = state.destination,
                onDestination = onDestination,
                onClose = onClose,
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(Color.White.copy(alpha = 0.10f)),
            )
            Column(modifier = Modifier.fillMaxSize()) {
                ModMenuTopBar(
                    title = if (state.title.isNotEmpty()) state.title
                    else stringResource(R.string.mod_menu_title),
                    state = state,
                    onSearch = onSearch,
                    onToggleFavoritesOnly = onToggleFavoritesOnly,
                    onClose = onClose,
                )
                Box(modifier = Modifier.fillMaxSize()) {
                    when (state.destination) {
                        ModMenuDestination.MODULES -> ModMenuModuleList(
                            state = state,
                            onToggleModule = onToggleModule,
                            onToggleFavorite = onToggleFavorite,
                        )
                        ModMenuDestination.COSMETICS -> ModMenuPlaceholder(
                            icon = Icons.Outlined.Person,
                            text = stringResource(R.string.mod_menu_tab_cosmetics),
                        )
                        ModMenuDestination.SAVE -> ModMenuSaveList(state)
                        ModMenuDestination.SETTINGS -> ModMenuSettingsList(
                            state = state,
                            onToggleSetting = onToggleSetting,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The sidebar: the four destinations, and Settings pinned to the bottom.
 *
 * Settings sits apart from the other three because it configures the menu itself rather than the
 * game; putting it in the same visual group as Modules invites the mis-click the old rail had.
 */
@Composable
private fun ModMenuSidebar(
    destination: ModMenuDestination,
    onDestination: (ModMenuDestination) -> Unit,
    onClose: () -> Unit,
) {
    val colors = glowColors
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(196.dp)
            .padding(vertical = GlowDimens.SPACE_4.dp),
        verticalArrangement = Arrangement.spacedBy(GlowDimens.SPACE_2.dp),
    ) {
        ModMenuSidebarItem(
            icon = Icons.Outlined.Inventory2,
            label = stringResource(R.string.mod_menu_tab_modules),
            selected = destination == ModMenuDestination.MODULES,
            onClick = { onDestination(ModMenuDestination.MODULES) },
        )
        ModMenuSidebarItem(
            icon = Icons.Outlined.Person,
            label = stringResource(R.string.mod_menu_tab_cosmetics),
            selected = destination == ModMenuDestination.COSMETICS,
            onClick = { onDestination(ModMenuDestination.COSMETICS) },
        )
        ModMenuSidebarItem(
            icon = Icons.Outlined.Save,
            label = stringResource(R.string.mod_menu_tab_save),
            selected = destination == ModMenuDestination.SAVE,
            onClick = { onDestination(ModMenuDestination.SAVE) },
        )
        Spacer(modifier = Modifier.weight(1f))
        ModMenuSidebarItem(
            icon = Icons.Filled.Settings,
            label = stringResource(R.string.mod_menu_tab_settings),
            selected = destination == ModMenuDestination.SETTINGS,
            onClick = { onDestination(ModMenuDestination.SETTINGS) },
        )
    }
}

@Composable
private fun ModMenuSidebarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = glowColors
    val accent = MaterialTheme.colorScheme.primary
    val tint by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(glowMotion.fastMs),
        label = "sidebarTint",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GlowDimens.SPACE_2.dp)
            .clip(RoundedCornerShape(GlowDimens.RADIUS_MEDIUM.dp))
            .background(accent.copy(alpha = 0.16f * tint))
            .clickable(onClick = onClick)
            .padding(
                horizontal = GlowDimens.SPACE_3.dp,
                vertical = GlowDimens.SPACE_3.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GlowDimens.SPACE_3.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) accent else colors.textSecondary,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) colors.textPrimary else colors.textSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The top bar: the destination title, the module count, the search box and the close button. */
@Composable
private fun ModMenuTopBar(
    title: String,
    state: ModMenuState,
    onSearch: (String) -> Unit,
    onToggleFavoritesOnly: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = glowColors
    val accent = MaterialTheme.colorScheme.primary
    Column(modifier = Modifier.padding(GlowDimens.SPACE_4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GlowDimens.SPACE_3.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (state.destination == ModMenuDestination.MODULES) {
                Text(
                    text = stringResource(R.string.mod_menu_active_count, state.activeCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                )
            }
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.mod_menu_close),
                    tint = colors.textSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        AnimatedVisibility(visible = state.destination == ModMenuDestination.MODULES) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = GlowDimens.SPACE_3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(GlowDimens.SPACE_2.dp),
            ) {
                TextField(
                    value = state.search,
                    onValueChange = onSearch,
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.mod_menu_search_hint),
                            color = colors.textDisabled,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = colors.textDisabled,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.06f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = accent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    shape = RoundedCornerShape(GlowDimens.RADIUS_MEDIUM.dp),
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onToggleFavoritesOnly) {
                    Icon(
                        imageVector = if (state.favoritesOnly) Icons.Filled.Star
                        else Icons.Outlined.StarBorder,
                        contentDescription = stringResource(R.string.mod_menu_favorites),
                        tint = if (state.favoritesOnly) accent else colors.textSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

/** The Modules destination: one flat list of modules with a switch, a star and a badge. */
@Composable
private fun ModMenuModuleList(
    state: ModMenuState,
    onToggleModule: (String, Boolean) -> Unit,
    onToggleFavorite: (String) -> Unit,
) {
    val colors = glowColors
    val rows = state.visibleRows
    if (rows.isEmpty()) {
        ModMenuPlaceholder(
            icon = Icons.Outlined.Inventory2,
            text = stringResource(R.string.mod_menu_empty_modules),
        )
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = GlowDimens.SPACE_4.dp),
        verticalArrangement = Arrangement.spacedBy(GlowDimens.SPACE_2.dp),
    ) {
        items(rows, key = { it.stableKey }) { row ->
            ModMenuModuleRow(
                row = row,
                onToggle = { onToggleModule(row.stableKey, it) },
                onFavorite = { onToggleFavorite(row.stableKey) },
            )
        }
        item { Spacer(modifier = Modifier.height(GlowDimens.SPACE_4.dp)) }
    }
}

@Composable
private fun ModMenuModuleRow(
    row: ModRow,
    onToggle: (Boolean) -> Unit,
    onFavorite: () -> Unit,
) {
    val colors = glowColors
    val accent = MaterialTheme.colorScheme.primary
    val unavailable = row.unavailableReason != null
    val shape = RoundedCornerShape(GlowDimens.RADIUS_LARGE.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = 0.045f))
            .border(1.dp, Color.White.copy(alpha = 0.09f), shape)
            .alpha(if (unavailable) 0.55f else 1f)
            .padding(
                horizontal = GlowDimens.SPACE_4.dp,
                vertical = GlowDimens.SPACE_3.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GlowDimens.SPACE_3.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckBox,
            contentDescription = null,
            tint = if (row.enabled) accent else colors.textDisabled,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.name,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = row.unavailableReason ?: row.description.ifEmpty {
                    stringResource(R.string.mod_menu_no_description)
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (unavailable) colors.warning else colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onFavorite, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = if (row.favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = stringResource(R.string.mod_menu_favorites),
                tint = if (row.favorite) accent else colors.textDisabled,
                modifier = Modifier.size(18.dp),
            )
        }
        Switch(
            checked = row.enabled,
            onCheckedChange = if (unavailable) null else onToggle,
            enabled = !unavailable,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accent,
                uncheckedThumbColor = colors.textDisabled,
                uncheckedTrackColor = Color.White.copy(alpha = 0.12f),
            ),
        )
    }
}

/** The Save destination: the saved configurations the player can switch to at once. */
@Composable
private fun ModMenuSaveList(state: ModMenuState) {
    val colors = glowColors
    if (state.loadouts.isEmpty()) {
        ModMenuPlaceholder(
            icon = Icons.Outlined.Save,
            text = stringResource(R.string.mod_menu_save_empty),
            secondary = stringResource(R.string.mod_menu_save_hint),
        )
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = GlowDimens.SPACE_4.dp),
        verticalArrangement = Arrangement.spacedBy(GlowDimens.SPACE_2.dp),
    ) {
        items(state.loadouts, key = { it.id }) { loadout ->
            val shape = RoundedCornerShape(GlowDimens.RADIUS_LARGE.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(Color.White.copy(alpha = 0.045f))
                    .border(1.dp, Color.White.copy(alpha = 0.09f), shape)
                    .padding(GlowDimens.SPACE_4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(GlowDimens.SPACE_3.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Save,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = loadout.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.mod_menu_module_count, loadout.enabledCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

/** The Settings destination: the menu's own toggles and sliders. */
@Composable
private fun ModMenuSettingsList(
    state: ModMenuState,
    onToggleSetting: (String, Boolean) -> Unit,
) {
    val colors = glowColors
    val accent = MaterialTheme.colorScheme.primary
    if (state.settings.isEmpty()) {
        ModMenuPlaceholder(
            icon = Icons.Outlined.Tune,
            text = stringResource(R.string.mod_menu_tab_settings),
        )
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = GlowDimens.SPACE_4.dp),
        verticalArrangement = Arrangement.spacedBy(GlowDimens.SPACE_2.dp),
    ) {
        items(state.settings, key = { it.key }) { row ->
            val shape = RoundedCornerShape(GlowDimens.RADIUS_LARGE.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(Color.White.copy(alpha = 0.045f))
                    .border(1.dp, Color.White.copy(alpha = 0.09f), shape)
                    .padding(
                        horizontal = GlowDimens.SPACE_4.dp,
                        vertical = GlowDimens.SPACE_3.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(GlowDimens.SPACE_3.dp),
            ) {
                Text(
                    text = row.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (row.kind == SettingsKind.TOGGLE) {
                    val on = row.value.equals("true", ignoreCase = true)
                    Switch(
                        checked = on,
                        onCheckedChange = { onToggleSetting(row.key, it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accent,
                            uncheckedThumbColor = colors.textDisabled,
                            uncheckedTrackColor = Color.White.copy(alpha = 0.12f),
                        ),
                    )
                } else {
                    Text(
                        text = row.value,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
}

/** The shared empty/placeholder state. */
@Composable
private fun ModMenuPlaceholder(
    icon: ImageVector,
    text: String,
    secondary: String? = null,
) {
    val colors = glowColors
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(GlowDimens.SPACE_3.dp),
            modifier = Modifier.padding(GlowDimens.SPACE_6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textDisabled,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(10.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textSecondary,
            )
            if (secondary != null) {
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textDisabled,
                )
            }
        }
    }
}

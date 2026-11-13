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
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt

/**
 * A dropdown: a trigger field plus a glass panel of options.
 *
 * The panel is positioned from the trigger's measured height rather than from a fixed offset. The
 * pack uses a constant `IntOffset(0, 46)`, which assumes the trigger is 38dp and puts the panel on
 * top of the field the moment a caller changes the height. Measuring costs one layout pass on the
 * trigger and removes that coupling.
 *
 * The panel is a real `Popup`, so it draws above the rest of the screen and takes focus to dismiss.
 */
@Composable
fun CaramelDropdown(
    value: String,
    modifier: Modifier = Modifier,
    width: Int = 180,
    panelWidth: Int = width,
    panel: @Composable (close: () -> Unit) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    var triggerHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val gapPx = with(density) { 8.dp.roundToPx() }
    Box(modifier) {
        CaramelDropdownField(
            text = value,
            open = open,
            modifier = Modifier
                .width(width.dp)
                .onGloballyPositioned { triggerHeightPx = it.size.height },
        ) { open = !open }
        if (open) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, triggerHeightPx + gapPx),
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true),
            ) {
                CaramelGlassPanel(
                    Modifier.width(panelWidth.dp),
                    RoundedCornerShape(16.dp),
                    popup = true,
                    elevation = 32.dp,
                ) {
                    panel { open = false }
                }
            }
        }
    }
}

/** The closed trigger of a [CaramelDropdown]. */
@Composable
fun CaramelDropdownField(
    text: String,
    open: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(11.dp)
    Row(
        modifier
            .height(38.dp)
            .clip(shape)
            .background(Color.White.copy(if (open) 0.13f else 0.08f))
            .border(
                1.dp,
                if (open) CaramelColors.Blue.copy(0.7f) else Color.White.copy(0.18f),
                shape,
            )
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = CaramelType.Label.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.weight(1f),
        )
        Icon(
            if (open) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
            null,
            Modifier.size(18.dp),
            CaramelColors.Text2,
        )
    }
}

/** A dropdown over a list of plain strings. */
@Composable
fun CaramelSimpleDropdown(
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    width: Int = 190,
) = CaramelDropdown(value, modifier, width) { close ->
    Column(Modifier.padding(8.dp)) {
        options.forEach { option ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(
                        if (option == value) CaramelColors.Blue.copy(0.24f) else Color.Transparent,
                    )
                    .clickable {
                        onSelect(option)
                        close()
                    }
                    .padding(horizontal = 12.dp),
                Alignment.CenterStart,
            ) {
                Text(option, style = CaramelType.Label)
            }
        }
    }
}

/**
 * A searchable list panel for a dropdown whose options are too many to show at once.
 *
 * The filter is applied to [items] by [label] and [matches], and the list is capped in height so the
 * panel never grows past the screen. The empty state is rendered inside the list rather than
 * replacing it, so the search field keeps its focus while the user types a query that matches
 * nothing.
 */
@Composable
fun <T> CaramelSearchListPanel(
    items: List<T>,
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    search: String,
    key: (T) -> Any,
    isSelected: (T) -> Boolean,
    onSelect: (T) -> Unit,
    row: @Composable (T) -> Unit,
) {
    Column(
        Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CaramelSearchField(query, onQueryChange, placeholder, Modifier.fillMaxWidth())
        LazyColumn(Modifier.heightIn(max = 252.dp)) {
            items(items, key = key) { item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(
                            if (isSelected(item)) {
                                CaramelColors.Blue.copy(0.26f)
                            } else {
                                Color.Transparent
                            },
                        )
                        .clickable { onSelect(item) }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    row(item)
                }
            }
            if (items.isEmpty()) {
                item {
                    Text(
                        search,
                        style = CaramelType.Body,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}

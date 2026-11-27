package org.chimeramc.client.ui.compose.caramel.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.chimeramc.client.R
import org.chimeramc.client.ui.compose.caramel.C
import org.chimeramc.client.ui.compose.caramel.GlassButton
import org.chimeramc.client.ui.compose.caramel.T
import org.chimeramc.client.ui.compose.caramel.sp_

/**
 * The shared modal chrome, ported from the pack's `Modals.kt` header helpers.
 *
 * <p>A title, an optional subtitle, trailing actions and a close button; a hairline rule; and the
 * menu row every popup uses. Kept in one file so a modal cannot drift from the others' padding.
 */
@Composable
internal fun CaramelModalHeader(
    title: String,
    sub: String? = null,
    onClose: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 32.dp, end = 24.dp, top = 24.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = T.Title.copy(fontSize = 20.sp_))
            if (sub != null) Text(sub, style = T.Body)
        }
        actions()
        GlassButton(null, onClose, icon = Icons.Rounded.Close, height = 32)
    }
}

@Composable
internal fun CaramelRule() =
    Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(1.dp).background(C.sheen(0.09f)))

/** A labelled group: a caption over its content, from the pack's `Field`. */
@Composable
internal fun CaramelField(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = T.Label)
        content()
    }
}

/** One row of an app/account menu: an icon, a label, an optional active tint or danger colour. */
@Composable
internal fun CaramelMenuItem(
    text: String,
    icon: ImageVector,
    active: Boolean = false,
    danger: Boolean = false,
    onClick: () -> Unit = {},
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier.fillMaxWidth().height(34.dp).clip(shape)
            .background(if (active) C.Blue.copy(0.2f) else Color.Transparent)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, Modifier.size(16.dp), if (danger) C.Red else C.Text2)
        Text(
            text,
            style = T.Label.copy(color = if (danger) C.Red else C.Text, fontWeight = FontWeight.Medium),
        )
    }
}

/** A left-rail item in a big modal (instance settings), from the pack's `SideItem`. */
@Composable
internal fun CaramelSideItem(text: String, icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(11.dp)
    Row(
        Modifier.fillMaxWidth().height(38.dp).clip(shape)
            .background(if (active) C.Blue.copy(0.24f) else Color.Transparent)
            .then(if (active) Modifier.border(1.dp, C.Blue.copy(0.45f), shape) else Modifier)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, Modifier.size(17.dp), if (active) C.Text else C.Text3)
        Text(text, style = T.Label.copy(color = if (active) C.Text else C.Text2))
    }
}

/** The close button every modal shares, so its label is translated in one place. */
@Composable
internal fun CaramelCloseButton(onClose: () -> Unit, height: Int = 32) =
    GlassButton(stringResource(R.string.caramel_close), onClose, icon = Icons.Rounded.Close, height = height)

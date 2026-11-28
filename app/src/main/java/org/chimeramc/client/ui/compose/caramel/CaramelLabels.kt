package org.chimeramc.client.ui.compose.caramel

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.chimeramc.client.R
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The Caramel label and chip set, ported from the pack's `components/Labels.kt`.
 *
 * <p>Labels carry status (Release/Beta, Locked/Unlocked, Equipped, a server's version range). The
 * pack's rule is one tinted pill per status with a matching hairline, so a status reads the same
 * wherever it appears. Keeping the rule in one function is what stops a "Beta" pill drifting to a
 * different orange in one screen.
 */
@Composable
fun StatusLabel(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .height(20.dp)
            .clip(shape)
            .background(color.copy(0.16f))
            .border(1.dp, color.copy(0.5f), shape)
            .padding(horizontal = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(11.dp), color)
        Text(text, style = T.Chip.copy(color = color))
    }
}

/** A "Release" (green) or "Beta" (orange) pill. */
@Composable
fun ChannelLabel(label: String, color: Color, modifier: Modifier = Modifier) = StatusLabel(label, color, modifier)

/** An "Unlocked" (green, open padlock) or "Locked" (red, closed padlock) pill. */
@Composable
fun ProfileLabel(locked: Boolean, modifier: Modifier = Modifier) = StatusLabel(
    if (locked) stringResource(R.string.caramel_locked) else stringResource(R.string.caramel_unlocked),
    if (locked) C.Red else C.Green,
    modifier,
    if (locked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
)

/** A selectable filter chip; the selected one is a blue gradient. */
@Composable
fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .height(26.dp)
            .clip(shape)
            .background(
                if (selected) {
                    Brush.verticalGradient(listOf(C.BlueLight, C.BlueDark))
                } else {
                    SolidColor(C.sheen(0.07f))
                },
            )
            .border(1.dp, if (selected) Color.Transparent else C.sheen(0.16f), shape)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick)
            .padding(horizontal = 13.dp),
        Alignment.Center,
    ) {
        Text(text, style = T.Chip.copy(fontSize = 11.5.sp_, color = if (selected) Color.White else C.Text2))
    }
}

/** The pack's `SegmentedTabs`: a pill row with a sliding blue selection. */
@Composable
fun SegmentedTabs(
    items: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .height(36.dp)
            .clip(shape)
            .background(C.sheen(0.06f))
            .border(1.dp, C.sheen(0.14f), shape)
            .padding(3.dp),
    ) {
        items.forEachIndexed { i, t ->
            Box(
                Modifier
                    .width(96.dp)
                    .fillMaxHeight()
                    .clip(shape)
                    .background(
                        if (i == selected) {
                            Brush.verticalGradient(listOf(C.BlueLight, C.BlueDark))
                        } else {
                            SolidColor(Color.Transparent)
                        },
                    )
                    .clickable(remember { MutableInteractionSource() }, null) { onSelect(i) },
                Alignment.Center,
            ) {
                Text(
                    t.uppercase(),
                    style = T.Chip.copy(
                        letterSpacing = androidx.compose.ui.unit.TextUnit(1f, androidx.compose.ui.unit.TextUnitType.Sp),
                        color = if (i == selected) Color.White else C.Text2,
                    ),
                )
            }
        }
    }
}

/** The account avatar slot. The profile picture is drawn by the caller over this. */
@Composable
fun CaramelAvatar(size: Int = 36, online: Boolean = false, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size.dp)
            .background(C.sheen(0.09f), CircleShape)
            .border(if (online) 2.dp else 1.dp, if (online) C.Green else C.sheen(0.3f), CircleShape),
    )
}

/**
 * The account avatar: the placeholder with the real Xbox (or GitHub) picture drawn over it.
 *
 * <p>The picture is fetched off the main thread and cached; until it arrives — or if it never does —
 * the placeholder ring shows, so the slot is never empty and a network failure is invisible rather
 * than a broken frame.
 */
@Composable
fun CaramelAvatarPicture(
    url: String?,
    size: Int = 36,
    online: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val bitmap = produceState<Bitmap?>(initialValue = null, url) {
        value = if (CaramelAvatarLoader.isLoadable(url)) {
            withContext(Dispatchers.IO) { CaramelAvatarLoader.load(context, url) }
        } else null
    }.value
    Box(modifier.size(size.dp), Alignment.Center) {
        CaramelAvatar(size, online)
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(size.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

/**
 * The server-icon slot used by Quick play and the Servers list.
 *
 * <p>When [iconKey] names a server the client can draw, its own mark fills a rounded square; the
 * generic tinted square is the fallback for a server with no known mark. The square is clipped
 * either way, so a wide logo never bleeds past its tile.
 */
@Composable
fun ServerIcon(
    color: Color,
    size: Int = 26,
    modifier: Modifier = Modifier,
    iconKey: String = "",
) {
    val shape = RoundedCornerShape((size * 0.3f).dp)
    val icon = ServerIcons.drawableFor(iconKey)
    Box(
        modifier
            .size(size.dp)
            .clip(shape)
            .background(if (icon != 0) C.sheen(0.06f) else color.copy(0.22f))
            .border(1.dp, color.copy(0.6f), shape),
        Alignment.Center,
    ) {
        if (icon != 0) {
            Image(
                painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size((size * 0.72f).dp),
                contentScale = ContentScale.Fit,
            )
        } else {
            Box(Modifier.size((size * 0.4f).dp).background(color, RoundedCornerShape(2.dp)))
        }
    }
}

/**
 * One friend row: the real picture, gamertag and presence, as the Friends card and modal draw it.
 */
@Composable
fun CaramelFriendRow(friend: CaramelFriendInfo, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(C.sheen(0.05f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CaramelAvatarPicture(friend.avatarUrl, 32, online = friend.online)
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(friend.gamertag, style = T.Label)
                // Only a friend confirmed on a Caramel-only channel wears the mark; an ordinary Xbox
                // account shows as itself, so the badge stays a fact rather than decoration.
                if (friend.caramel) CaramelUserChip()
            }
            // A request says so instead of showing presence: "Offline" next to someone asking to be
            // added is the line that made a request look like a friend who is not online.
            if (friend.relation != CaramelFriendRelation.FRIEND) {
                val label = if (friend.relation == CaramelFriendRelation.REQUEST_RECEIVED) {
                    stringResource(R.string.caramel_friend_row_request_received)
                } else {
                    stringResource(R.string.caramel_friend_row_request_sent)
                }
                Text(label, style = T.Caption.copy(color = C.Caramel))
            } else {
                val presence = when {
                    friend.online && friend.presence.isNotEmpty() -> friend.presence
                    friend.online -> stringResource(R.string.caramel_account_online)
                    else -> stringResource(R.string.caramel_offline)
                }
                Text(presence, style = T.Caption.copy(color = if (friend.online) C.Green else C.Text3))
            }
        }
    }
}

/**
 * The CaramelClient mark beside a confirmed Caramel user's name.
 *
 * <p>Shared by the add-friend result and every friend row, so a confirmed account looks the same
 * wherever it is shown. The row only draws it when the account was heard on a Caramel-only channel;
 * an ordinary Xbox account shows without it.
 */
@Composable
fun CaramelUserChip(modifier: Modifier = Modifier) {
    val painter = painterResource(R.drawable.ic_caramel_badge)
    Row(
        modifier.height(20.dp).clip(RoundedCornerShape(50)).background(C.Caramel.copy(0.16f))
            .border(1.dp, C.Caramel.copy(0.5f), RoundedCornerShape(50)).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Image(
            painter,
            contentDescription = stringResource(R.string.caramel_user_badge),
            modifier = Modifier.size(13.dp),
        )
        Text(stringResource(R.string.caramel_user_badge), style = T.Chip.copy(color = C.Caramel))
    }
}

/** A "1.21.132" measured-version label for a server; blank versions draw nothing. */
@Composable
fun VersionLabel(version: String, modifier: Modifier = Modifier) {
    if (version.isEmpty()) return
    StatusLabel(version, Color(0xFF78A0FF), modifier)
}

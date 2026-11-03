package org.chimeramc.client.ui.compose.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.chimeramc.client.R
import org.chimeramc.client.ui.compose.theme.GlowDimens
import org.chimeramc.client.ui.compose.theme.glowColors

/**
 * The account section: the signed-in account's email, Xbox gamertag and Minecraft name, and the
 * friends list.
 *
 * <p>The friends list is honest. This build has no Xbox Live PeopleHub integration, so it cannot
 * read Minecraft friends; rather than render an empty list that looks broken, the section says so and
 * offers the Add Friend action, which opens the account's Xbox friends page.
 */
@Composable
fun AccountSection(
    state: AccountSectionState,
    onAddFriend: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = glowColors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(GlowDimens.SCREEN_PADDING.dp),
        verticalArrangement = Arrangement.spacedBy(GlowDimens.SPACE_4.dp),
    ) {
        Text(
            text = stringResource(R.string.account_section_title),
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold,
        )

        if (!state.signedIn) {
            Surface(
                shape = RoundedCornerShape(GlowDimens.CARD_RADIUS.dp),
                color = colors.surface,
                border = BorderStroke(GlowDimens.CARD_BORDER_WIDTH.dp, colors.stroke),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(GlowDimens.CARD_PADDING.dp)) {
                    Text(
                        text = stringResource(R.string.account_signed_out),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                    Spacer(Modifier.height(GlowDimens.SPACE_4.dp))
                    Button(onClick = onSignIn) {
                        Text(stringResource(R.string.account_sign_in))
                    }
                }
            }
            return@Column
        }

        // The identity card: avatar + rows.
        Surface(
            shape = RoundedCornerShape(GlowDimens.CARD_RADIUS.dp),
            color = colors.surface,
            border = BorderStroke(GlowDimens.CARD_BORDER_WIDTH.dp, colors.stroke),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(GlowDimens.CARD_PADDING.dp)) {
                val rows = state.rows(
                    stringResource(R.string.account_email),
                    stringResource(R.string.account_gamertag),
                    stringResource(R.string.account_minecraft_name),
                    stringResource(R.string.account_xuid),
                )
                for (row in rows) {
                    AccountRow(label = row.label, value = row.value)
                }
                if (rows.isEmpty()) {
                    Text(
                        text = stringResource(R.string.account_details_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                }
            }
        }

        // The friends card.
        Surface(
            shape = RoundedCornerShape(GlowDimens.CARD_RADIUS.dp),
            color = colors.surface,
            border = BorderStroke(GlowDimens.CARD_BORDER_WIDTH.dp, colors.stroke),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(GlowDimens.CARD_PADDING.dp)) {
                Text(
                    text = stringResource(R.string.account_friends_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(GlowDimens.SPACE_2.dp))
                Text(
                    text = stringResource(R.string.account_friends_not_available),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
                Spacer(Modifier.height(GlowDimens.SPACE_4.dp))
                Button(
                    onClick = onAddFriend,
                    enabled = state.canAddFriend(),
                ) {
                    Text(stringResource(R.string.account_add_friend))
                }
            }
        }
    }
}

@Composable
private fun AccountRow(label: String, value: String) {
    val colors = glowColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = GlowDimens.SPACE_2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textSecondary,
            modifier = Modifier.width(110.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

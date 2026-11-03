package org.chimeramc.client.ui.compose.account

import androidx.compose.ui.platform.ComposeView
import org.chimeramc.client.ui.compose.theme.GlowTheme

/**
 * The Java-facing entry point for the Compose account section.
 *
 * <p>The activity builds the [AccountSectionState] from the account store and hands over two
 * callbacks; everything Compose-shaped stays in Kotlin, since a {@code @Composable} cannot be called
 * from Java.
 */
object AccountSectionHost {

    @JvmStatic
    fun setContent(
        composeView: ComposeView,
        state: AccountSectionState,
        onAddFriend: Runnable,
        onSignIn: Runnable,
    ) {
        composeView.setContent {
            GlowTheme {
                AccountSection(
                    state = state,
                    onAddFriend = { onAddFriend.run() },
                    onSignIn = { onSignIn.run() },
                )
            }
        }
    }
}

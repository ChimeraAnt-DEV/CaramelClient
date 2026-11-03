package org.chimeramc.client.ui.compose.account

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import org.chimeramc.client.ui.compose.theme.GlowTheme

/**
 * The Java-facing entry point for the Compose account section.
 *
 * <p>The activity builds the [AccountSectionState], hosts it, and pushes a friends update into the
 * same [MutableState] once the PeopleHub read returns; everything Compose-shaped stays in Kotlin.
 */
object AccountSectionHost {

    @JvmStatic
    fun state(initial: AccountSectionState): MutableState<AccountSectionState> =
        mutableStateOf(initial)

    @JvmStatic
    fun setContent(
        composeView: ComposeView,
        state: MutableState<AccountSectionState>,
        onAddFriend: Runnable,
        onSignIn: Runnable,
    ) {
        composeView.setContent {
            GlowTheme {
                AccountSection(
                    state = state.value,
                    onAddFriend = { onAddFriend.run() },
                    onSignIn = { onSignIn.run() },
                )
            }
        }
    }
}

package org.chimeramc.client.ui.compose.account;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

/**
 * The account section's rules, pinned without a device.
 *
 * <p>The section shows the email, gamertag, Minecraft name and XUID, omits an unknown field rather
 * than showing a blank, and is honest that the friends list is unavailable on this build.
 */
public class AccountSectionStateTest {

    private static AccountSectionState signedIn() {
        return new AccountSectionState(true, "a@b.com", "Gamertag", "Steve", "12345", "url",
                AccountSectionState.FriendsStatus.LOADING, java.util.Collections.emptyList(), "");
    }

    @Test
    public void theRowsAreInOrderAndOmitUnknownFields() {
        List<AccountSectionState.Row> rows = signedIn()
                .rows("Email", "Gamertag", "Minecraft", "XUID");
        assertEquals(4, rows.size());
        assertEquals("a@b.com", rows.get(0).value);
        assertEquals("Gamertag", rows.get(1).value);
        assertEquals("Steve", rows.get(2).value);
        assertEquals("12345", rows.get(3).value);

        AccountSectionState partial = new AccountSectionState(true, "a@b.com", "", "Steve", "",
                "", AccountSectionState.FriendsStatus.LOADING, java.util.Collections.emptyList(), "");
        List<AccountSectionState.Row> partialRows = partial
                .rows("Email", "Gamertag", "Minecraft", "XUID");
        assertEquals("an empty field must be omitted, not shown blank", 2, partialRows.size());
        assertEquals("a@b.com", partialRows.get(0).value);
        assertEquals("Steve", partialRows.get(1).value);
    }

    @Test
    public void nullFieldsAreTreatedAsEmpty() {
        AccountSectionState s = new AccountSectionState(true, null, null, null, null, null,
                AccountSectionState.FriendsStatus.LOADING, null, null);
        assertTrue(s.rows("a", "b", "c", "d").isEmpty());
        assertEquals("", s.email);
    }

    @Test
    public void addFriendNeedsASignedInAccount() {
        assertTrue(signedIn().canAddFriend());
        assertFalse(AccountSectionState.signedOut().canAddFriend());
        assertEquals(AccountSectionState.FriendsStatus.SIGNED_OUT,
                AccountSectionState.signedOut().friendsStatus);
        assertFalse(AccountSectionState.signedOut().signedIn);
    }

    @Test
    public void aFriendsResultUpdatesInPlace() {
        // The account details survive a friends update; only the friends fields change.
        AccountSectionState updated = signedIn().withFriends(
                AccountSectionState.FriendsStatus.LOADED,
                java.util.List.of(new org.chimeramc.client.core.xbox.XboxPeopleHub.Friend(
                        "1", "Steve", "", "", "Online", "Playing")),
                "");
        assertEquals("a@b.com", updated.email);
        assertEquals(AccountSectionState.FriendsStatus.LOADED, updated.friendsStatus);
        assertEquals(1, updated.friends.size());
        assertTrue(updated.friends.get(0).isOnline());
    }

    @Test
    public void anErrorCarriesItsReason() {
        AccountSectionState failed = signedIn().withFriends(
                AccountSectionState.FriendsStatus.ERROR, null, "Unauthorized");
        assertEquals("Unauthorized", failed.friendsError);
        assertTrue(failed.friends.isEmpty());
    }
}

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
                AccountSectionState.FriendsStatus.NOT_AVAILABLE);
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
                "", AccountSectionState.FriendsStatus.NOT_AVAILABLE);
        List<AccountSectionState.Row> partialRows = partial
                .rows("Email", "Gamertag", "Minecraft", "XUID");
        assertEquals("an empty field must be omitted, not shown blank", 2, partialRows.size());
        assertEquals("a@b.com", partialRows.get(0).value);
        assertEquals("Steve", partialRows.get(1).value);
    }

    @Test
    public void nullFieldsAreTreatedAsEmpty() {
        AccountSectionState s = new AccountSectionState(true, null, null, null, null, null,
                AccountSectionState.FriendsStatus.NOT_AVAILABLE);
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
    public void theFriendsStatusIsHonestOnASignedInAccount() {
        assertEquals("friends are not listable on this build, so the section says so",
                AccountSectionState.FriendsStatus.NOT_AVAILABLE, signedIn().friendsStatus);
    }
}

package org.chimeramc.client.ui.compose.account;

import java.util.ArrayList;
import java.util.List;

/**
 * The account section's content, as data.
 *
 * <p>The section shows the signed-in account: the Microsoft email, the Xbox gamertag and the
 * Minecraft name, plus the friends list. The friends list is honest about what this build can
 * actually read — the launcher has no Xbox Live PeopleHub integration, so it cannot list Minecraft
 * friends. Rather than show an empty list that looks broken, the section carries an explicit
 * "not available" reason, which the UI renders as an explanation with the Add Friend action.
 */
public final class AccountSectionState {

    /** Why the friends list has no entries. */
    public enum FriendsStatus {
        /** A signed-in account whose friends could not be listed (no PeopleHub in this build). */
        NOT_AVAILABLE,
        /** No account is signed in. */
        SIGNED_OUT,
    }

    public final boolean signedIn;
    /** The Microsoft account email, or empty when unknown. */
    public final String email;
    /** The Xbox gamertag, or empty. */
    public final String gamertag;
    /** The Minecraft in-game name, or empty. */
    public final String minecraftName;
    /** The Xbox Live XUID, or empty. */
    public final String xuid;
    public final String avatarUrl;
    public final FriendsStatus friendsStatus;

    public AccountSectionState(boolean signedIn, String email, String gamertag,
                               String minecraftName, String xuid, String avatarUrl,
                               FriendsStatus friendsStatus) {
        this.signedIn = signedIn;
        this.email = safe(email);
        this.gamertag = safe(gamertag);
        this.minecraftName = safe(minecraftName);
        this.xuid = safe(xuid);
        this.avatarUrl = safe(avatarUrl);
        this.friendsStatus = friendsStatus;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    /** The signed-out state. */
    public static AccountSectionState signedOut() {
        return new AccountSectionState(false, "", "", "", "", "", FriendsStatus.SIGNED_OUT);
    }

    /**
     * The rows to show for the account, in order: email, gamertag, Minecraft name and XUID.
     *
     * <p>A row whose value is unknown is omitted rather than shown blank, so the section never shows
     * an empty field.
     */
    public List<Row> rows(String emailLabel, String gamertagLabel, String minecraftLabel,
                          String xuidLabel) {
        List<Row> out = new ArrayList<>();
        if (!email.isEmpty()) out.add(new Row(emailLabel, email));
        if (!gamertag.isEmpty()) out.add(new Row(gamertagLabel, gamertag));
        if (!minecraftName.isEmpty()) out.add(new Row(minecraftLabel, minecraftName));
        if (!xuid.isEmpty()) out.add(new Row(xuidLabel, xuid));
        return out;
    }

    /** Whether the Add Friend action can be offered (it needs a signed-in account). */
    public boolean canAddFriend() {
        return signedIn;
    }

    /** One label/value row. */
    public static final class Row {
        public final String label;
        public final String value;

        public Row(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }
}

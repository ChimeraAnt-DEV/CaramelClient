package org.chimeramc.client.launcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;

/**
 * The Friends card and modal draw the account's real Xbox Live friends.
 *
 * <p>{@code XboxFriendsClient} and {@code XboxPeopleHub} existed and were unit-tested, but nothing
 * ever called them, so the card drew a fixed empty state and the modal drew the empty-state body as
 * a list item. These pin the wiring: the activity loads friends and the screens render rows.
 */
public class FriendsListWiringTest {

    private static String read(String relative) {
        File dir = new File("").getAbsoluteFile();
        for (int i = 0; i < 6 && dir != null; i++) {
            File f = new File(dir, relative);
            if (f.isFile()) {
                try {
                    return new String(java.nio.file.Files.readAllBytes(f.toPath()),
                            java.nio.charset.StandardCharsets.UTF_8);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
            dir = dir.getParentFile();
        }
        throw new IllegalStateException("could not find " + relative);
    }

    @Test
    public void theActivityLoadsFriendsThroughTheRealClient() {
        String activity = read(
                "app/src/main/java/org/chimeramc/client/launcher/ui/activities/MainActivity.java");
        assertTrue("the activity must call the Xbox friends client for the whole graph",
                activity.contains("friendsClient.fetchGraph("));
        assertTrue("a received request must be kept apart from a friend",
                activity.contains("CaramelFriendRelation.REQUEST_RECEIVED"));
        assertTrue("a sent request must be kept apart from a friend",
                activity.contains("CaramelFriendRelation.REQUEST_SENT"));
        assertTrue("the friends read must use the account's XSTS token",
                activity.contains("MsftAuthManager.xboxLiveXsts("));
        assertTrue("a rebuild must not refetch while the same account is signed in",
                activity.contains("friendsAccountId"));
    }

    /**
     * A friends read that failed must be retried, not treated as the final answer.
     *
     * <p>The first read after a sign-in often runs before the network or a renewed XSTS token is
     * ready, so it fails and writes an error line. The account id alone marked that attempt as
     * "loaded", so the list stayed on its error line for the rest of the session — the reported
     * "friends never sync". A read only counts as loaded when it came back without an error, and a
     * manual refresh must be able to force another read.
     */
    @Test
    public void aFailedFriendsReadIsRetriedRatherThanStuck() {
        String activity = read(
                "app/src/main/java/org/chimeramc/client/launcher/ui/activities/MainActivity.java");
        assertTrue("a read only counts as loaded when it succeeded",
                activity.contains("friendsLoaded = graph != null;"));
        assertTrue("a token that was not ready must not mark the account loaded",
                activity.contains("friendsLoaded = false;"));
        assertTrue("the automatic path retries an unloaded account",
                activity.contains("!friendsLoaded && !friendsLoading"));
        assertTrue("the user can force another read from the Friends modal",
                activity.contains("public void refreshFriends()"));
    }

    @Test
    public void theFriendsScreensRenderRows() {
        String modalsRow = read(
                "app/src/main/java/org/chimeramc/client/ui/compose/caramel/CaramelLabels.kt");
        String home = read(
                "app/src/main/java/org/chimeramc/client/ui/compose/caramel/launcher/CaramelHome.kt");
        assertTrue("the card must draw real friends", home.contains("CaramelFriendRow(friend)"));
        String modals = read(
                "app/src/main/java/org/chimeramc/client/ui/compose/caramel/launcher/CaramelModals.kt");
        assertTrue("the modal must list real friends",
                modals.contains("items(visible) { friend -> CaramelFriendRow(friend) }"));
        assertTrue("a request must be labelled in the row, not drawn as presence",
                modalsRow.contains("caramel_friend_row_request_received"));
        assertTrue("a sent request must be labelled too",
                modalsRow.contains("caramel_friend_row_request_sent"));
        assertFalse("the modal must not draw the empty-state body as a row",
                modals.contains("stringResource(R.string.caramel_no_friends_body),\n                            style = T.Body,\n                            modifier = Modifier.padding(start = 8.dp, top = 6.dp)"));
    }

    @Test
    public void aFriendWearsTheBadgeOnlyWhenConfirmedOnACaramelChannel() {
        String activity = read(
                "app/src/main/java/org/chimeramc/client/launcher/ui/activities/MainActivity.java");
        assertTrue("a friend row's badge must come from the Caramel-user directory",
                activity.contains("CaramelUserDirectory.isCaramelUser(this, f.xuid)"));
        String labels = read(
                "app/src/main/java/org/chimeramc/client/ui/compose/caramel/CaramelLabels.kt");
        assertTrue("the friend row must draw the Caramel chip only for a confirmed account",
                labels.contains("if (friend.caramel) CaramelUserChip()"));
        String models = read(
                "app/src/main/java/org/chimeramc/client/ui/compose/caramel/CaramelModels.kt");
        assertTrue("CaramelFriendInfo must carry the caramel flag",
                models.contains("val caramel: Boolean = false"));
    }
}

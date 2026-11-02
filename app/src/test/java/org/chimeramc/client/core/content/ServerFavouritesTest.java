package org.chimeramc.client.core.content;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

/**
 * Pins the favourite-server rules (item 254).
 *
 * <p>The star is presentation state over a file the game owns, so the two things that can go wrong
 * are a key that does not identify a server and an ordering that moves an unstarred server for no
 * reason. Both are asserted directly, with no device and no store.
 */
public class ServerFavouritesTest {

    private static ServerItem server(String name, String ip, int port) {
        return new ServerItem(name, ip, port);
    }

    @Test
    public void theKeyIsTheAddressNotTheName() {
        // Two rows for the same address with different names are the same server; a rename must not
        // lose the star.
        assertEquals(ServerFavourites.key(server("Home", "1.2.3.4", 19132)),
                ServerFavourites.key(server("Renamed", "1.2.3.4", 19132)));
        assertEquals("1.2.3.4:19132", ServerFavourites.key(server("x", "1.2.3.4", 19132)));
    }

    @Test
    public void aNullServerHasAnEmptyKeyRatherThanThrowing() {
        assertEquals("", ServerFavourites.key(null));
    }

    @Test
    public void toggleAddsThenRemoves() {
        Set<String> one = ServerFavourites.toggle(Collections.emptySet(), "a:1");
        assertTrue(one.contains("a:1"));
        Set<String> two = ServerFavourites.toggle(one, "a:1");
        assertFalse(two.contains("a:1"));
    }

    @Test
    public void toggleDoesNotMutateTheSetItWasHanded() {
        Set<String> original = new HashSet<>(Arrays.asList("a:1"));
        ServerFavourites.toggle(original, "b:2");
        assertEquals(Collections.singleton("a:1"), original);
    }

    @Test
    public void aBlankKeyIsIgnored() {
        assertEquals(Collections.emptySet(), ServerFavourites.toggle(Collections.emptySet(), ""));
        assertEquals(Collections.emptySet(), ServerFavourites.toggle(Collections.emptySet(), null));
    }

    @Test
    public void favouritesFloatToTheTopAndKeepTheirRelativeOrder() {
        ServerItem a = server("A", "a", 1);
        ServerItem b = server("B", "b", 2);
        ServerItem c = server("C", "c", 3);
        List<ServerItem> stored = Arrays.asList(a, b, c);
        Set<String> stars = new HashSet<>(Arrays.asList("c:3", "b:2"));
        List<ServerItem> ordered = ServerFavourites.order(stored, stars);
        assertEquals(Arrays.asList(b, c, a), ordered);
    }

    @Test
    public void anUnstarredServerReturnsToItsStoredPlace() {
        ServerItem a = server("A", "a", 1);
        ServerItem b = server("B", "b", 2);
        List<ServerItem> stored = Arrays.asList(a, b);
        assertEquals(stored, ServerFavourites.order(stored, Collections.emptySet()));
    }

    @Test
    public void orderIsStableSoUnstarringDoesNotShuffleTheRest() {
        ServerItem a = server("A", "a", 1);
        ServerItem b = server("B", "b", 2);
        ServerItem c = server("C", "c", 3);
        List<ServerItem> stored = Arrays.asList(a, b, c);
        List<ServerItem> withStar = ServerFavourites.order(stored,
                Collections.singleton("c:3"));
        assertEquals(Arrays.asList(c, a, b), withStar);
        List<ServerItem> withoutStar = ServerFavourites.order(stored, Collections.emptySet());
        assertEquals(stored, withoutStar);
    }

    @Test
    public void anEmptyOrNullListOrdersToEmpty() {
        assertTrue(ServerFavourites.order(null, Collections.emptySet()).isEmpty());
        assertTrue(ServerFavourites.order(Collections.emptyList(), null).isEmpty());
    }

    @Test
    public void isOrderedReportsWhenNothingNeedsToChange() {
        ServerItem a = server("A", "a", 1);
        List<ServerItem> list = Collections.singletonList(a);
        assertTrue(ServerFavourites.isOrdered(list, list));
        assertFalse(ServerFavourites.isOrdered(list, Collections.emptyList()));
    }
}

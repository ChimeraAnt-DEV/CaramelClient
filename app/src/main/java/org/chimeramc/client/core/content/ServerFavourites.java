package org.chimeramc.client.core.content;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Which servers the user starred, and how a starred list is ordered (item 254).
 *
 * <p>The game's own {@code external_servers.txt} is a flat, ordered file with no room for a
 * favourite flag, and rewriting it with an extra field would make the game reject rows it does not
 * understand. So the stars live beside it, keyed by {@code ip:port} - the same key the ping results
 * use, because a server is the address, not the name a user typed for it.
 *
 * <p>The rules are pure and Android-free so they are unit-tested: the key of a server, whether a
 * set of keys contains one, the toggle, and the stable ordering that floats favourites to the top
 * without disturbing the stored order inside each group.
 */
public final class ServerFavourites {

    private ServerFavourites() {
    }

    /** The stable identity of a server: its address, not the display name. */
    public static String key(ServerItem server) {
        if (server == null) return "";
        return server.ip + ":" + server.port;
    }

    /**
     * Adds the key when absent, removes it when present. Returns a new set so a caller cannot
     * mutate the set it was handed - a UI adapter may be holding that reference.
     */
    public static Set<String> toggle(Set<String> current, String key) {
        Set<String> next = new HashSet<>(current != null ? current : Collections.emptySet());
        if (key == null || key.isEmpty()) return next;
        if (!next.remove(key)) next.add(key);
        return next;
    }

    /**
     * The list with favourites first, otherwise in the order given.
     *
     * <p>A stable partition, not a comparator sort: two favourites keep their relative order, so
     * un-favouriting a server puts it back where it was rather than at the end.
     */
    public static List<ServerItem> order(List<ServerItem> servers, Set<String> favourites) {
        if (servers == null || servers.isEmpty()) return Collections.emptyList();
        if (favourites == null || favourites.isEmpty()) return new ArrayList<>(servers);
        List<ServerItem> starred = new ArrayList<>();
        List<ServerItem> rest = new ArrayList<>();
        for (ServerItem server : servers) {
            if (favourites.contains(key(server))) starred.add(server);
            else rest.add(server);
        }
        starred.addAll(rest);
        return starred;
    }

    /**
     * True when {@code ordered} is already in the favourites-first order of {@code stored}, so a
     * caller can skip a needless adapter update.
     */
    public static boolean isOrdered(List<ServerItem> stored, List<ServerItem> ordered) {
        if (stored == null || ordered == null) return stored == ordered;
        if (stored.size() != ordered.size()) return false;
        for (int i = 0; i < stored.size(); i++) {
            if (stored.get(i) != ordered.get(i)) return false;
        }
        return true;
    }
}

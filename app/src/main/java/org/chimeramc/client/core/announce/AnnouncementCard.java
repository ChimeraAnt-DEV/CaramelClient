package org.chimeramc.client.core.announce;

/**
 * One announcement the launcher can show on the Launch tab (item 255).
 *
 * <p>The feed is a small JSON document the project hosts, so an announcement can be posted without
 * shipping a new APK. That makes the <em>version gate</em> the important part: an announcement that
 * only affects builds below a certain version code must not be shown to a build that already has the
 * fix, or the card lies about a problem the user does not have. {@link #matches} is that rule, and
 * it is pure so the boundary is unit-tested rather than being a comparison written inline at the
 * call site.
 *
 * <p>A blank {@code minVersionCode} means "no floor" and a blank {@code maxVersionCode} means "no
 * ceiling", which is how a general announcement and an "only on this range" notice are both spelled.
 */
public final class AnnouncementCard {

    /** No version bound. */
    public static final long NO_BOUND = 0L;

    public final String id;
    public final String title;
    public final String body;
    public final String url;
    /** The lowest version code this announcement applies to, or {@link #NO_BOUND}. */
    public final long minVersionCode;
    /** The highest version code this announcement applies to, or {@link #NO_BOUND}. */
    public final long maxVersionCode;

    public AnnouncementCard(String id, String title, String body, String url,
                            long minVersionCode, long maxVersionCode) {
        this.id = id == null ? "" : id.trim();
        this.title = title == null ? "" : title.trim();
        this.body = body == null ? "" : body.trim();
        this.url = url == null ? "" : url.trim();
        this.minVersionCode = Math.max(NO_BOUND, minVersionCode);
        this.maxVersionCode = Math.max(NO_BOUND, maxVersionCode);
    }

    /** Whether the card has the minimum content to render: an id, a title and a body. */
    public boolean isRenderable() {
        return !id.isEmpty() && !title.isEmpty() && !body.isEmpty();
    }

    /**
     * Whether this announcement applies to a build at {@code versionCode}.
     *
     * <p>A card that is not {@link #isRenderable()} never matches, so a malformed entry in the feed
     * is skipped rather than shown as an empty box. When both bounds are set and the range is
     * inverted (a feed mistake), the card matches nothing rather than everything.
     */
    public boolean matches(long versionCode) {
        if (!isRenderable()) return false;
        if (minVersionCode != NO_BOUND && versionCode < minVersionCode) return false;
        if (maxVersionCode != NO_BOUND && versionCode > maxVersionCode) return false;
        if (minVersionCode != NO_BOUND && maxVersionCode != NO_BOUND
                && maxVersionCode < minVersionCode) {
            return false;
        }
        return true;
    }

    /** Whether the card carries a link the user can open. */
    public boolean hasLink() {
        return url.startsWith("https://") || url.startsWith("http://");
    }
}

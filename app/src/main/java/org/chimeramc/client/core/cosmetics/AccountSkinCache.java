package org.chimeramc.client.core.cosmetics;

import org.chimeramc.client.util.AppLog;

import java.io.File;

/**
 * The on-disk cache of the account-looked-up skin, as a plain PNG under {@code files/cosmetics/}.
 *
 * <p>Kept separate from {@link LocalSkinCache} on purpose: that file is written by the game's own
 * skin capture ({@code GameSkinFeed}), and two writers on one file would clobber each other — a
 * capture would erase the account skin, or the reverse. A PNG is also the natural format here
 * because the texture is already a PNG download and it can be inspected by eye on a device.
 *
 * <p>The cache is keyed by the account identifier in a sidecar name, and readers must check it with
 * {@link #isValidFor} — the key alone does not stop a previous account's skin from being shown;
 * comparing it is what does.
 */
public final class AccountSkinCache {

    /** The directory, under the app's files dir. */
    public static final String DIR = "cosmetics";
    /** The cached atlas file name. */
    public static final String PNG_NAME = "account_skin.png";

    /**
     * How long a cached account skin is trusted before the service re-syncs it.
     *
     * <p>Bedrock skins change server-side and the client is never told, so a cache with no expiry
     * meant a player who changed their skin kept seeing the old one until they cleared app data.
     * A day is short enough to notice a change and long enough that the third-party lookup is not
     * hammered on every launch.
     */
    public static final long MAX_AGE_MS = 24L * 60L * 60L * 1000L;

    private AccountSkinCache() {
    }

    /** A cache entry: the atlas source bytes plus the account key it belongs to. */
    public static final class Entry {
        public final byte[] png;
        public final String accountKey;
        /**
         * When the entry was fetched, in wall-clock ms, or 0 when unknown.
         *
         * <p>A 0 stamp (an entry written before the stamp existed) reads as stale, so it is
         * refreshed once rather than trusted forever.
         */
        public final long fetchedAtMs;

        public Entry(byte[] png, String accountKey) {
            this(png, accountKey, 0L);
        }

        public Entry(byte[] png, String accountKey, long fetchedAtMs) {
            this.png = png;
            this.accountKey = accountKey == null ? "" : accountKey;
            this.fetchedAtMs = fetchedAtMs;
        }
    }

    /** The cache key for a XUID: the account identifier the entry belongs to. */
    public static String accountKeyFor(String xuid) {
        if (xuid == null || xuid.isEmpty()) return "";
        return "account:" + xuid;
    }

    /**
     * Whether a cached entry actually belongs to [xuid].
     *
     * <p>The account key was written from the start but never compared, so a cache populated for one
     * account was served to whichever account signed in next — the "my skin is the wrong account's"
     * report. An entry with no key (written before the key was honoured) is treated as not belonging
     * to any account, so it is refreshed rather than shown. Pure, so both halves are testable.
     */
    public static boolean isValidFor(Entry entry, String xuid) {
        if (entry == null || entry.png == null || entry.png.length == 0) return false;
        String expected = accountKeyFor(xuid);
        if (expected.isEmpty()) return false;
        return expected.equals(entry.accountKey);
    }

    /**
     * Whether a cached entry should be re-synced from the service.
     *
     * <p>The one rule the constant sync needs, so the service does not re-implement it: refresh when
     * there is no usable entry for this account, or when the entry is older than {@code maxAgeMs}. A
     * non-positive {@code maxAgeMs} disables the age check (so the cache is trusted indefinitely),
     * which is what the "off" state and the tests use; a wrong account is still refreshed either way.
     */
    public static boolean shouldRefresh(Entry entry, String xuid, long nowMs, long maxAgeMs) {
        if (!isValidFor(entry, xuid)) return true;
        if (maxAgeMs <= 0L) return false;
        if (entry.fetchedAtMs <= 0L) return true;  // no stamp: refresh once
        return nowMs - entry.fetchedAtMs >= maxAgeMs;
    }

    public static File file(File filesRoot) {
        if (filesRoot == null) return null;
        File dir = new File(filesRoot, DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }
        return new File(dir, PNG_NAME);
    }

    /** Writes the PNG and its account key. Returns true on success. */
    public static boolean write(File filesRoot, Entry entry) {
        File target = file(filesRoot);
        if (target == null || entry == null || entry.png == null || entry.png.length == 0) {
            return false;
        }
        try (java.io.FileOutputStream out = new java.io.FileOutputStream(target)) {
            out.write(entry.png);
        } catch (java.io.IOException e) {
            return false;
        }
        // The account key lives in a sibling name so the PNG stays a plain, inspectable file.
        try (java.io.FileOutputStream out =
                     new java.io.FileOutputStream(new File(target.getParentFile(), PNG_NAME + ".key"))) {
            out.write(entry.accountKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.io.IOException ignored) {
            AppLog.warnOnce("GlowberryClient", "AccountSkinCache:86", "Ignored a failure at AccountSkinCache:86", ignored);
        }
        // The fetch time lives in its own sibling too, so the PNG and key stay untouched by it.
        try (java.io.FileOutputStream out =
                     new java.io.FileOutputStream(new File(target.getParentFile(), PNG_NAME + ".time"))) {
            out.write(Long.toString(entry.fetchedAtMs)
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.io.IOException ignored) {
            AppLog.warnOnce("GlowberryClient", "AccountSkinCache:time", "Ignored a failure writing the skin fetch time", ignored);
        }
        return true;
    }

    /** Reads the cached entry, or null when there is none. */
    public static Entry read(File filesRoot) {
        File target = file(filesRoot);
        if (target == null || !target.exists()) return null;
        try {
            byte[] png = java.nio.file.Files.readAllBytes(target.toPath());
            if (png.length == 0) return null;
            String key = "";
            File keyFile = new File(target.getParentFile(), PNG_NAME + ".key");
            if (keyFile.exists()) {
                key = new String(java.nio.file.Files.readAllBytes(keyFile.toPath()),
                        java.nio.charset.StandardCharsets.UTF_8).trim();
            }
            long fetchedAtMs = 0L;
            File timeFile = new File(target.getParentFile(), PNG_NAME + ".time");
            if (timeFile.exists()) {
                try {
                    fetchedAtMs = Long.parseLong(new String(
                            java.nio.file.Files.readAllBytes(timeFile.toPath()),
                            java.nio.charset.StandardCharsets.UTF_8).trim());
                } catch (NumberFormatException ignored) {
                    fetchedAtMs = 0L;
                }
            }
            return new Entry(png, key, fetchedAtMs);
        } catch (java.io.IOException e) {
            return null;
        }
    }

    /** Deletes the cached skin. Returns true when nothing remains. */
    public static boolean clear(File filesRoot) {
        File target = file(filesRoot);
        if (target == null) return true;
        boolean ok = !target.exists() || target.delete();
        File keyFile = new File(target.getParentFile(), PNG_NAME + ".key");
        if (keyFile.exists()) ok &= keyFile.delete();
        File timeFile = new File(target.getParentFile(), PNG_NAME + ".time");
        if (timeFile.exists()) ok &= timeFile.delete();
        return ok;
    }
}

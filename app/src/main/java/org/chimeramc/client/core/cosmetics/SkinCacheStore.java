package org.chimeramc.client.core.cosmetics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A bounded, least-recently-used store of captured player skins.
 *
 * <p>The single-entry {@link LocalSkinCache} is enough for "your own skin", but cosmetic sync and
 * the account skin path can hold several atlases, and an unbounded cache of 512x512 atlases grows
 * without limit over a long-lived install. This store keeps each skin in its own subdirectory
 * (reusing {@link LocalSkinCache}'s format verbatim, so there is still one on-disk layout) and evicts
 * the least-recently-used entry once the byte cap is exceeded.
 *
 * <p><b>Byte-budgeted, not count-budgeted.</b> A count cap treats a 64x64 atlas and a 512x512 atlas
 * as equal, so a handful of HD skins could sit far under a "64 entries" cap while consuming
 * hundreds of megabytes. The budget is on bytes, which is what actually fills a device.
 *
 * <p>Pure apart from the filesystem: it takes a root directory and the budget as parameters, so the
 * eviction rule is unit-tested with real temp directories and no Android.
 */
public final class SkinCacheStore {

    /** The subdirectory under the cache root that holds the per-key slots. */
    public static final String DIR = "skin_slots";
    /** The default budget: 24 MiB of atlases, roughly a hundred 128x128 skins. */
    public static final long DEFAULT_BUDGET_BYTES = 24L * 1024L * 1024L;

    /** The bytes one entry occupies on disk (the atlas plus its small metadata file). */
    private static final String SLOT_SEPARATOR = "__";

    private SkinCacheStore() {
    }

    /** A slot's identity: the cache key and the directory that holds it. */
    public static final class Slot {
        public final String key;
        public final File dir;
        /** The last-touched wall-clock time, used for the eviction order. */
        public final long touchedAtMs;

        Slot(String key, File dir, long touchedAtMs) {
            this.key = key;
            this.dir = dir;
            this.touchedAtMs = touchedAtMs;
        }
    }

    /** The slots root for a files dir, created if missing. */
    public static File root(File filesRoot) {
        File d = new File(filesRoot, DIR);
        if (!d.exists()) d.mkdirs();
        return d;
    }

    /** The slot directory for a cache key, sanitised so it cannot escape the root. */
    public static File slotDir(File filesRoot, String key) {
        return new File(root(filesRoot), sanitizeKey(key));
    }

    /**
     * A filesystem-safe slot name for a key.
     *
     * <p>A key is an identity hash, but it may arrive from a caller that passes something else, and
     * a key containing a path separator or ".." would let a slot escape the cache root. Only
     * {@code [A-Za-z0-9._-]} survives; everything else becomes {@code _}, so the result is always a
     * single path segment.
     */
    public static String sanitizeKey(String key) {
        if (key == null || key.isEmpty()) return "default";
        StringBuilder sb = new StringBuilder(key.length());
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '.' || c == '_' || c == '-';
            sb.append(ok ? c : '_');
        }
        String out = sb.toString();
        // "." and ".." are legal by the character rule but are not legal slot names.
        if (out.equals(".") || out.equals("..")) return "default";
        return out.length() > 96 ? out.substring(0, 96) : out;
    }

    /**
     * Writes a skin into its slot and evicts least-recently-used slots until the budget is met.
     *
     * <p>The just-written slot is never the one evicted, even if it alone exceeds the budget: the
     * skin the caller just asked to store is the one most likely to be needed next, and evicting it
     * immediately would make the store appear to fail.
     */
    public static boolean put(File filesRoot, String key, LocalSkinCache.Entry entry,
                              long budgetBytes, long nowMs) {
        if (filesRoot == null || entry == null) return false;
        File dir = slotDir(filesRoot, key);
        if (!LocalSkinCache.writeEntry(dir, entry)) return false;
        // Touch the slot so it is the most recently used for the eviction pass below.
        touch(dir, nowMs);
        enforceBudget(filesRoot, budgetBytes, nowMs, sanitizeKey(key));
        return true;
    }

    /** Reads a slot, touching it so the LRU order reflects the read. */
    public static LocalSkinCache.Entry get(File filesRoot, String key, long nowMs) {
        if (filesRoot == null) return null;
        File dir = slotDir(filesRoot, key);
        LocalSkinCache.Entry entry = LocalSkinCache.readEntry(dir);
        if (entry != null) touch(dir, nowMs);
        return entry;
    }

    /** True when a slot holds a readable entry. */
    public static boolean contains(File filesRoot, String key) {
        return filesRoot != null && LocalSkinCache.existsIn(slotDir(filesRoot, key));
    }

    /** Every slot currently on disk, oldest-touched first. */
    public static List<Slot> list(File filesRoot) {
        List<Slot> out = new ArrayList<>();
        File root = root(filesRoot);
        File[] children = root.listFiles();
        if (children == null) return out;
        for (File child : children) {
            if (child == null || !child.isDirectory()) continue;
            out.add(new Slot(child.getName(), child, lastModified(child)));
        }
        out.sort((a, b) -> Long.compare(a.touchedAtMs, b.touchedAtMs));
        return out;
    }

    /** The total bytes the store occupies on disk. */
    public static long totalBytes(File filesRoot) {
        long total = 0L;
        for (Slot slot : list(filesRoot)) {
            total += sizeOf(slot.dir);
        }
        return total;
    }

    /**
     * Evicts least-recently-used slots until the total is within {@code budgetBytes}.
     *
     * <p>A non-positive budget means "unbounded", so a caller can disable eviction deliberately
     * rather than having to pass a very large number.
     */
    public static void enforceBudget(File filesRoot, long budgetBytes, long nowMs, String protectKey) {
        if (budgetBytes <= 0L) return;
        for (Slot slot : list(filesRoot)) {
            if (totalBytes(filesRoot) <= budgetBytes) return;
            if (slot.key.equals(protectKey)) continue;
            LocalSkinCache.clearEntry(slot.dir);
            slot.dir.delete();
        }
    }

    /** Deletes every slot. */
    public static void clear(File filesRoot) {
        for (Slot slot : list(filesRoot)) {
            LocalSkinCache.clearEntry(slot.dir);
            slot.dir.delete();
        }
    }

    /** Touches a slot's modification time so the LRU order can be read back after a restart. */
    private static void touch(File dir, long nowMs) {
        if (dir == null) return;
        try {
            // setLastModified with a real clock is the portable way to record a touch on the file
            // itself, so the order survives a process restart without a sidecar file.
            dir.setLastModified(nowMs > 0L ? nowMs : System.currentTimeMillis());
        } catch (Throwable ignored) {
        }
    }

    private static long lastModified(File dir) {
        try {
            return dir.lastModified();
        } catch (Throwable t) {
            return 0L;
        }
    }

    private static long sizeOf(File dir) {
        return org.chimeramc.client.launcher.util.StorageUsage.sizeOf(dir);
    }

    /** The separator used when a composite key is flattened; kept for callers that build one. */
    public static String composeKey(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) sb.append(SLOT_SEPARATOR);
            sb.append(part == null ? "" : part);
        }
        return sb.toString();
    }
}

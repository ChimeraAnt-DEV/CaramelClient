package org.chimeramc.client.launcher.util;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Measures how much disk an instance actually uses, and formats it for the UI (item 265).
 *
 * <p>Kept free of Android types so the walk and the formatting are unit-tested with real temp
 * directories. Two rules matter:
 *
 * <ul>
 *   <li>The walk is iterative and never follows a symbolic link. A recursive call can blow the stack
 *       on a deep tree, and following a link can walk out of the instance (or loop forever).</li>
 *   <li>A missing file or an unreadable directory contributes {@code 0} rather than throwing: the
 *       number is a display value, and a partially readable tree should still show a figure.</li>
 * </ul>
 */
public final class StorageUsage {

    private StorageUsage() {
    }

    /**
     * The total size in bytes of everything under {@code root}.
     *
     * @return the byte count, or {@code 0} for a null or missing root.
     */
    public static long sizeOf(File root) {
        if (root == null) return 0L;
        long total = 0L;
        Deque<File> pending = new ArrayDeque<>();
        pending.add(root);
        while (!pending.isEmpty()) {
            File current = pending.poll();
            if (current == null || isSymlink(current)) continue;
            if (current.isFile()) {
                total += Math.max(0L, current.length());
                continue;
            }
            File[] children = current.listFiles();
            if (children == null) continue;
            for (File child : children) {
                if (child != null) pending.add(child);
            }
        }
        return total;
    }

    /** The combined size of several roots; null entries are skipped. */
    public static long sizeOf(File... roots) {
        long total = 0L;
        if (roots == null) return total;
        for (File root : roots) {
            total += sizeOf(root);
        }
        return total;
    }

    private static boolean isSymlink(File file) {
        try {
            File canonical = file.getCanonicalFile();
            return !canonical.equals(file.getAbsoluteFile());
        } catch (Exception e) {
            // Unresolvable path: treat as a link so the walk does not enter it.
            return true;
        }
    }

    /** A short human-readable size, e.g. {@code 812 KB} or {@code 1.4 GB}. */
    public static String format(long bytes) {
        if (bytes < 1024L) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024.0) return String.format(java.util.Locale.US, "%.0f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024.0) return String.format(java.util.Locale.US, "%.1f MB", mb);
        return String.format(java.util.Locale.US, "%.2f GB", mb / 1024.0);
    }
}

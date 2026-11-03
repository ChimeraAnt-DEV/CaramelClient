package org.chimeramc.client.core.mods;

import java.io.File;
import java.io.IOException;

/**
 * The rule for where a native library may be loaded from.
 *
 * <p>Native code is loaded with the launcher's own privileges, so the path it comes from is a
 * security boundary: a library under a shared, world-writable or externally-chosen directory can be
 * replaced by another app between the checksum pass and the load. Every native entry is therefore
 * required to sit inside a directory the launcher itself owns before it reaches {@code System.load}.
 *
 * <p>The allowed roots are the app's private internal directory and its app-specific external
 * directory ({@code getExternalFilesDir}), both of which are owned by the app's UID and are not
 * shared with other apps under scoped storage. A path outside them, a path that escapes a root with
 * {@code ..}, and a symlink that resolves outside are all refused.
 *
 * <p>Pure and Android-free apart from {@link File}, so the containment rules are unit-tested without
 * a device.
 */
public final class NativeLoadPolicy {

    private NativeLoadPolicy() {
    }

    /**
     * Whether {@code candidate} is inside one of {@code allowedRoots}.
     *
     * <p>Both sides are canonicalised first, so a {@code ..} segment or a symlink that points out of
     * a root is caught rather than merely looking contained as a string. A null candidate, a
     * candidate that does not exist, or an empty root list is refused: a path that cannot be resolved
     * cannot be proven safe.
     */
    public static boolean isWithinAllowedRoots(File candidate, File... allowedRoots) {
        if (candidate == null || allowedRoots == null || allowedRoots.length == 0) return false;
        final String target;
        try {
            target = candidate.getCanonicalPath();
        } catch (IOException e) {
            return false;
        }
        for (File root : allowedRoots) {
            if (root == null) continue;
            final String rootPath;
            try {
                rootPath = root.getCanonicalPath();
            } catch (IOException e) {
                continue;
            }
            if (target.equals(rootPath)) return true;
            if (target.startsWith(rootPath + File.separator)) return true;
        }
        return false;
    }

    /**
     * Returns a human-readable refusal reason, or null when the path is loadable.
     *
     * <p>Kept separate from {@link #isWithinAllowedRoots} so a caller can log or show why a library
     * was refused instead of a bare boolean.
     */
    public static String describeRefusal(File candidate, File... allowedRoots) {
        if (candidate == null) return "the library path is null";
        if (allowedRoots == null || allowedRoots.length == 0) {
            return "no allowed library root is configured";
        }
        if (isWithinAllowedRoots(candidate, allowedRoots)) return null;
        return "the library is outside the app's private directories: " + candidate.getAbsolutePath();
    }
}

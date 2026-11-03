package org.chimeramc.client.core.antegg;

import java.io.File;
import java.io.IOException;

/**
 * Sandbox-root validation for {@code .AntEgg} extraction.
 *
 * <p>The sandbox root normally comes from {@link AntEggLoader#sandboxRoot}, which derives it from
 * the app's own context, so it is private by construction. This class exists because the native
 * loader takes the root as an argument and writes files to it: the rule that the destination must
 * live inside the app's private directory belongs on both sides of the JNI boundary, so a bug in a
 * caller cannot turn the extractor into a writer anywhere on the filesystem.
 *
 * <p>The rules are pure apart from the canonical-path check, so the containment test is pinned by
 * {@code AntEggSandboxTest} with no device.
 */
public final class AntEggSandbox {

    private AntEggSandbox() {}

    /**
     * True when {@code candidate} is {@code root} or lives under it, on canonical paths.
     *
     * <p>Uses {@link File#getCanonicalPath} so a symlink cannot make a path look contained when it
     * resolves elsewhere. A missing candidate is resolved lexically (its parent is canonicalised
     * and the name appended), so a not-yet-created target directory can still be validated.
     */
    public static boolean isInside(File root, File candidate) {
        if (root == null || candidate == null) return false;
        try {
            String rootPath = root.getCanonicalPath();
            String candidatePath = canonicalise(candidate);
            if (candidatePath == null) return false;
            if (candidatePath.equals(rootPath)) return true;
            // Compare with a separator so /data/app/foo does not appear to contain /data/app/foobar.
            return candidatePath.startsWith(rootPath + File.separator);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * The canonical path of a file, resolving lexically when the file does not exist yet.
     *
     * @return the canonical path, or null when it cannot be resolved
     */
    private static String canonicalise(File file) throws IOException {
        if (file.exists()) {
            return file.getCanonicalPath();
        }
        File parent = file.getParentFile();
        if (parent == null) {
            return file.getCanonicalPath();
        }
        return new File(parent.getCanonicalFile(), file.getName()).getPath();
    }

    /**
     * Validates that {@code sandboxRoot} is a real, non-empty directory path inside
     * {@code allowedRoot}.
     *
     * @return null when the root is acceptable, otherwise a reason to show the user
     */
    public static String describeRefusal(File sandboxRoot, File allowedRoot) {
        if (sandboxRoot == null) {
            return "no sandbox directory was chosen";
        }
        if (allowedRoot == null) {
            return "the app's private directory is unavailable";
        }
        if (!sandboxRoot.isAbsolute()) {
            return "the sandbox directory must be an absolute path";
        }
        if (!isInside(allowedRoot, sandboxRoot)) {
            return "the sandbox directory must live inside the app's private storage";
        }
        return null;
    }
}

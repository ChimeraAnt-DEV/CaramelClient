package org.chimeramc.client.core.versions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns an unpacked game package into the shape {@code VersionManager} installs.
 *
 * <p>Two package layouts reach the importer and only one used to work. A single {@code .apk} was
 * copied to {@code apk/base.apk}; an {@code .xapk} (or a {@code .zip} an instance was exported to)
 * unpacks to its <em>own</em> layout — {@code base.apk}, {@code split_config.*.apk},
 * {@code Android/obb/<pkg>/main.<n>.obb} and {@code manifest.json} at the root, or all of it inside
 * one folder. The installer only reads {@code apk/} and {@code obb/}, so an archive imported as an
 * empty instance: it appeared in the list and refused to launch.
 *
 * <p>This class collects the parts wherever they were extracted and puts them where the installer
 * looks. It is plain {@code java.io} so the layout rules are unit-tested against a real directory
 * tree rather than a device.
 */
public final class ImportedPackageLayout {

    public static final String APK_DIR = "apk";
    public static final String OBB_DIR = "obb";
    public static final String BASE_APK = "base.apk";
    public static final String MANIFEST = "manifest.json";

    /** How deep to look for the parts. An exported instance nests at most two folders deep. */
    private static final int MAX_DEPTH = 4;

    private ImportedPackageLayout() {
    }

    /** What a picked file is. */
    public enum Kind {
        /** A single installable package. */
        APK,
        /** A zip container: an XAPK, or an exported instance folder. */
        ARCHIVE,
        /** Neither, by name or by signature. */
        UNKNOWN,
    }

    /** The outcome of {@link #normalise(File)}, for the caller's error message and its logging. */
    public static final class Result {
        public int apks;
        public int obbs;
        public boolean manifest;
        public boolean hasBase;

        /** Whether the package is complete enough to install. */
        public boolean installable() {
            return hasBase;
        }
    }

    /**
     * Decides what {@code file} is from its name and its first bytes.
     *
     * <p>The extension alone is not enough: a {@code .zip} can hold a game, and an APK renamed to
     * {@code .zip} is still an APK to the package manager. Both are zip containers, so the only
     * distinction that matters here is "one package" versus "a container to unpack".
     */
    public static Kind detect(File file) {
        if (file == null || !file.isFile()) return Kind.UNKNOWN;
        String name = file.getName().toLowerCase(java.util.Locale.ROOT);
        if (name.endsWith(".apk")) return Kind.APK;
        if (name.endsWith(".xapk") || name.endsWith(".zip") || name.endsWith(".apks")) {
            return hasZipSignature(file) ? Kind.ARCHIVE : Kind.UNKNOWN;
        }
        return hasZipSignature(file) ? Kind.ARCHIVE : Kind.UNKNOWN;
    }

    /** A zip starts with "PK\3\4"; an empty zip only has the end record, which is not an install. */
    static boolean hasZipSignature(File file) {
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] head = new byte[4];
            int read = in.read(head);
            return read == 4 && head[0] == 'P' && head[1] == 'K' && head[2] == 3 && head[3] == 4;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Whether a zip entry would be written outside the directory it is extracted into.
     *
     * <p>An entry named {@code ../../files/…} escapes the target once joined, which is a write
     * anywhere the app can reach. The name is checked rather than the joined path so a Windows-style
     * separator cannot slip past either.
     */
    public static boolean isUnsafeEntry(String entryName) {
        if (entryName == null || entryName.isEmpty()) return true;
        String normalised = entryName.replace('\\', '/');
        if (normalised.startsWith("/")) return true;
        for (String segment : normalised.split("/")) {
            if ("..".equals(segment)) return true;
        }
        return false;
    }

    /**
     * Moves every APK, OBB and manifest found under {@code root} into {@code apk/}, {@code obb/}
     * and {@code root/manifest.json}.
     *
     * <p>The largest APK becomes {@code base.apk} because that is the one the launcher boots; the
     * others keep their names so a split is still recognisable. An already-correct layout is left
     * alone, so this is safe to run twice.
     */
    public static Result normalise(File root) throws IOException {
        Result result = new Result();
        if (root == null || !root.isDirectory()) return result;

        List<File> apks = new ArrayList<>();
        List<File> obbs = new ArrayList<>();
        List<File> manifests = new ArrayList<>();
        collect(root, 0, apks, obbs, manifests);

        apks.removeIf(file -> APK_DIR.equals(file.getParentFile() == null
                ? "" : file.getParentFile().getName()) && BASE_APK.equals(file.getName()));
        result.apks = apks.size();
        result.obbs = obbs.size();
        result.manifest = !manifests.isEmpty();

        File apkDir = new File(root, APK_DIR);
        File obbDir = new File(root, OBB_DIR);
        if (!apks.isEmpty()) ensureDir(apkDir);
        if (!obbs.isEmpty()) ensureDir(obbDir);

        File base = largest(apks);
        for (File apk : apks) {
            String targetName = apk.equals(base) ? BASE_APK : apk.getName();
            move(apk, new File(apkDir, targetName));
        }
        for (File obb : obbs) {
            move(obb, new File(obbDir, obb.getName()));
        }
        for (File manifest : manifests) {
            File target = new File(root, MANIFEST);
            if (!target.equals(manifest)) move(manifest, target);
        }

        File existingBase = new File(apkDir, BASE_APK);
        result.hasBase = existingBase.isFile() && existingBase.length() > 0;
        return result;
    }

    private static void collect(File dir, int depth, List<File> apks, List<File> obbs,
                                List<File> manifests) {
        if (depth > MAX_DEPTH) return;
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) {
                collect(child, depth + 1, apks, obbs, manifests);
                continue;
            }
            String name = child.getName().toLowerCase(java.util.Locale.ROOT);
            if (name.endsWith(".apk")) {
                apks.add(child);
            } else if (name.endsWith(".obb")) {
                obbs.add(child);
            } else if (MANIFEST.equals(name)) {
                manifests.add(child);
            }
        }
    }

    private static File largest(List<File> files) {
        File best = null;
        for (File file : files) {
            if (best == null || file.length() > best.length()) best = file;
        }
        return best;
    }

    private static void ensureDir(File dir) throws IOException {
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IOException("could not create " + dir.getName());
        }
    }

    private static void move(File source, File target) throws IOException {
        if (source.equals(target)) return;
        File parent = target.getParentFile();
        if (parent != null) ensureDir(parent);
        try (FileInputStream in = new FileInputStream(source);
             FileOutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
        }
        if (!source.delete()) {
            // A leftover copy is harmless: the installer reads the moved one.
            source.deleteOnExit();
        }
    }
}

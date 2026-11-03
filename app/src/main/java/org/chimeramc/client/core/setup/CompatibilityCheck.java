package org.chimeramc.client.core.setup;

import java.util.ArrayList;
import java.util.List;

/**
 * The first-run compatibility checks.
 *
 * <p>A new install fails for a small, knowable set of reasons -- not enough free space, an APK that
 * cannot load in this process, the Play Store build instead of an imported one, a permission the
 * user declined -- and each has its own fix. Reporting them one at a time, after the user has
 * already picked a version and pressed Play, wastes the one moment where they are willing to read.
 * This evaluates all of them up front.
 *
 * <p>Pure and Android-free: the caller gathers the facts and this maps them to a severity and a
 * fix. That keeps the thresholds and the "which fix" decision unit-tested, because a wrong
 * threshold here is a user told to free space they do not need, or told everything is fine right
 * before a launch fails.
 */
public final class CompatibilityCheck {

    /** A free-space reading that could not be taken. Distinct from 0, which means "full". */
    public static final long UNKNOWN_BYTES = -1L;

    /**
     * The free space a launch needs to have a reasonable chance.
     *
     * <p>A Bedrock instance is extracted (base APK plus splits) and a large one runs to a couple of
     * gigabytes, so this is the floor below which an extraction will fail part-way; it is a
     * blocking check. {@link #LOW_FREE_BYTES} is the "will work, but you are close" warning.
     */
    public static final long MIN_FREE_BYTES = 2L * 1024L * 1024L * 1024L;
    public static final long LOW_FREE_BYTES = 4L * 1024L * 1024L * 1024L;

    private CompatibilityCheck() {
    }

    /** The checks, in the order they are shown. */
    public enum Id {
        FREE_STORAGE,
        ABI,
        PLAY_STORE_INSTALL,
        STORAGE_PERMISSION,
    }

    /** How much a check matters. */
    public enum Severity {
        /** Passed. */
        OK,
        /** The launcher can continue, but the user should know. */
        WARNING,
        /** A launch will fail until this is fixed. */
        BLOCKING,
    }

    /** The action the row offers. {@link #NONE} means there is nothing to do. */
    public enum Fix {
        NONE,
        /** Open the system storage settings, where the user can free space. */
        OPEN_STORAGE,
        /** Open the app's permission settings so the user can grant access. */
        OPEN_PERMISSIONS,
        /** Open the Library so the user can import a game file. */
        IMPORT_GAME,
        /** Re-run extraction/repair for the selected instance. */
        REPAIR,
    }

    /** The facts a compatibility pass reads. Every default is the benign value. */
    public static final class Facts {
        /** Free bytes on the volume the game data lives on, or {@link #UNKNOWN_BYTES}. */
        public long freeBytes = UNKNOWN_BYTES;
        /** True when the selected APK ships an ABI this 64-bit process can load. */
        public boolean abiCompatible = true;
        /** True when the selected instance is the device-installed Play Store package. */
        public boolean playStoreInstall = false;
        /** True when the launcher holds the storage access it needs. */
        public boolean storagePermissionGranted = true;
        /** True when a version is selected at all; an ABI/storage check is meaningless without one. */
        public boolean versionSelected = true;

        public Facts freeBytes(long v) { freeBytes = v; return this; }
        public Facts abiCompatible(boolean v) { abiCompatible = v; return this; }
        public Facts playStoreInstall(boolean v) { playStoreInstall = v; return this; }
        public Facts storagePermissionGranted(boolean v) { storagePermissionGranted = v; return this; }
        public Facts versionSelected(boolean v) { versionSelected = v; return this; }
    }

    /** One check's outcome. */
    public static final class Result {
        public final Id id;
        public final Severity severity;
        public final Fix fix;

        Result(Id id, Severity severity, Fix fix) {
            this.id = id;
            this.severity = severity;
            this.fix = fix;
        }

        public boolean isBlocking() {
            return severity == Severity.BLOCKING;
        }
    }

    /**
     * Runs every check and returns the results in display order.
     *
     * <p>A null facts reads as an empty, all-OK pass rather than throwing: the caller is a screen
     * that must render, and "no information" is not a compatibility failure.
     */
    public static List<Result> evaluate(Facts facts) {
        Facts f = facts != null ? facts : new Facts();
        List<Result> out = new ArrayList<>(4);

        // Free space. An unknown reading is a WARNING, not a pass and not a failure: the check
        // could not be taken, and claiming either would be a guess.
        if (f.freeBytes == UNKNOWN_BYTES) {
            out.add(new Result(Id.FREE_STORAGE, Severity.WARNING, Fix.OPEN_STORAGE));
        } else if (f.freeBytes < MIN_FREE_BYTES) {
            out.add(new Result(Id.FREE_STORAGE, Severity.BLOCKING, Fix.OPEN_STORAGE));
        } else if (f.freeBytes < LOW_FREE_BYTES) {
            out.add(new Result(Id.FREE_STORAGE, Severity.WARNING, Fix.OPEN_STORAGE));
        } else {
            out.add(new Result(Id.FREE_STORAGE, Severity.OK, Fix.NONE));
        }

        // ABI. Only meaningful once a version is chosen; before that there is no APK to judge.
        if (!f.versionSelected || f.abiCompatible) {
            out.add(new Result(Id.ABI, Severity.OK, Fix.NONE));
        } else {
            out.add(new Result(Id.ABI, Severity.BLOCKING, Fix.IMPORT_GAME));
        }

        // The Play Store build. A warning, not a blocker: the user's fix is to import an APK
        // instead, and the import step of the wizard is the natural place to say so.
        out.add(new Result(Id.PLAY_STORE_INSTALL,
                f.playStoreInstall ? Severity.WARNING : Severity.OK,
                f.playStoreInstall ? Fix.IMPORT_GAME : Fix.NONE));

        // Storage permission. Blocking when denied, because the extraction cannot write without it.
        if (f.storagePermissionGranted) {
            out.add(new Result(Id.STORAGE_PERMISSION, Severity.OK, Fix.NONE));
        } else {
            out.add(new Result(Id.STORAGE_PERMISSION, Severity.BLOCKING, Fix.OPEN_PERMISSIONS));
        }

        return out;
    }

    /** True when at least one check blocks a launch. */
    public static boolean hasBlocking(List<Result> results) {
        if (results == null) return false;
        for (Result r : results) {
            if (r != null && r.isBlocking()) return true;
        }
        return false;
    }

    /** True when every check passed with no warning and no blocker. */
    public static boolean allOk(List<Result> results) {
        if (results == null) return true;
        for (Result r : results) {
            if (r != null && r.severity != Severity.OK) return false;
        }
        return true;
    }

    /** A human-readable size, for the free-space row. */
    public static String formatBytes(long bytes) {
        if (bytes < 0L) return "unknown";
        if (bytes < 1024L) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024.0) return String.format(java.util.Locale.US, "%.0f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024.0) return String.format(java.util.Locale.US, "%.0f MB", mb);
        double gb = mb / 1024.0;
        return String.format(java.util.Locale.US, "%.1f GB", gb);
    }
}

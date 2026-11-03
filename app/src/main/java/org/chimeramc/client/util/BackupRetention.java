package org.chimeramc.client.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Keeps a bounded number of instance backups.
 *
 * <p>Backups are large zips written to the shared Downloads folder, so without a cap every backup a
 * player ever took stays on disk forever. The rule is "keep the newest N for each instance"; the
 * newest backup of an instance is never deleted, so a retention of 0 or a malformed setting cannot
 * wipe the only copy a player has.
 *
 * <p>Pure and Android-free: it takes the backup files and the instance each belongs to, and returns
 * the files to delete, so the rule is unit-tested without a device or a real zip.
 */
public final class BackupRetention {

    /** The number of backups kept per instance when the setting is unset. */
    public static final int DEFAULT_KEEP = 5;

    /** A hard cap so a bad setting cannot leave thousands of archives on a device. */
    public static final int MAX_KEEP = 50;

    /** One backup file, as far as retention cares: the file, its instance and when it was written. */
    public static final class Candidate {
        public final File file;
        /** The instance this backup belongs to (its {@code directoryName}, else {@code profileId}). */
        public final String instanceKey;
        /** When the backup was written, from the manifest's {@code createdAt}. */
        public final long createdAt;

        public Candidate(File file, String instanceKey, long createdAt) {
            this.file = file;
            this.instanceKey = instanceKey == null ? "" : instanceKey;
            this.createdAt = createdAt;
        }
    }

    private BackupRetention() {
    }

    /** Clamps a stored keep count into {@code [1, MAX_KEEP]}, defaulting an unset value. */
    public static int clampKeep(int keep) {
        if (keep <= 0) return DEFAULT_KEEP;
        return Math.min(keep, MAX_KEEP);
    }

    /** Parses the retain count from a settings string, tolerating junk. */
    public static int parseKeep(String value) {
        if (value == null) return DEFAULT_KEEP;
        try {
            return clampKeep(Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            return DEFAULT_KEEP;
        }
    }

    /**
     * The files to delete so at most {@code keep} backups of each instance remain.
     *
     * <p>Grouped by instance, sorted newest first, and every file past the {@code keep}th of its
     * group is returned. The newest of a group is always kept even when {@code keep} is 0 or
     * negative. Ties on {@code createdAt} fall back to the file name so the result is deterministic.
     */
    public static List<File> selectForDeletion(List<Candidate> candidates, int keep) {
        List<File> toDelete = new ArrayList<>();
        if (candidates == null || candidates.isEmpty()) return toDelete;
        int limit = clampKeep(keep);

        Map<String, List<Candidate>> byInstance = new LinkedHashMap<>();
        for (Candidate c : candidates) {
            if (c == null || c.file == null) continue;
            byInstance.computeIfAbsent(c.instanceKey, k -> new ArrayList<>()).add(c);
        }
        for (List<Candidate> group : byInstance.values()) {
            group.sort(NEWEST_FIRST);
            for (int i = limit; i < group.size(); i++) {
                toDelete.add(group.get(i).file);
            }
        }
        return toDelete;
    }

    /**
     * Whether a file name looks like an instance backup this class owns, so a stray file in the
     * backups folder is never swept up.
     */
    public static boolean isManagedBackupName(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.startsWith("chimeralauncher_instance_")
                && lower.endsWith(InstanceBackupManager.BACKUP_EXTENSION);
    }

    private static final Comparator<Candidate> NEWEST_FIRST = (a, b) -> {
        int byTime = Long.compare(b.createdAt, a.createdAt);
        if (byTime != 0) return byTime;
        String an = a.file != null ? a.file.getName() : "";
        String bn = b.file != null ? b.file.getName() : "";
        return bn.compareTo(an);
    };
}

package org.chimeramc.client.core.minecraft;

import org.chimeramc.client.util.AppLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A record of each extracted library's size, modification time and SHA-256, and the rule that
 * decides whether the files on disk still match it.
 *
 * <p>A presence-and-length check is not enough to trust an extracted cache. A library can be the
 * right size and still be wrong: a partially overwritten file, a corrupted sector, a file another
 * process truncated and rewrote with different bytes, or - the case that actually happens on
 * Android - a copy interrupted between two writes that happened to land on the same length. Every
 * one of those loads to a {@code dlopen} failure or, worse, to a native crash inside a library
 * whose bytes are not the ones the launcher shipped. A hash is the only check that sees them.
 *
 * <p>Hashing ten libraries of a hundred megabytes each on every launch would be a real cost, so the
 * record carries the two cheap signals as well. {@link #verify} trusts a file whose size <em>and</em>
 * modification time still match the record - the file is demonstrably untouched - and re-hashes
 * only a file whose metadata changed, which is exactly when it might have been damaged. A cache hit
 * therefore costs one {@code stat} per library, and the hash does the work when something is
 * actually suspect.
 *
 * <p>Android-free and pure, so the format and the verify rule are unit-tested with no device.
 */
public final class LibraryHashManifest {

    private LibraryHashManifest() {
    }

    /** One library's recorded state. */
    public static final class Entry {
        public final String name;
        public final long size;
        public final long lastModified;
        public final String sha256;

        public Entry(String name, long size, long lastModified, String sha256) {
            this.name = name == null ? "" : name;
            this.size = size;
            this.lastModified = lastModified;
            this.sha256 = sha256 == null ? "" : sha256;
        }
    }

    /** What verification found. */
    public enum Verdict {
        /** Every file matches its record. */
        INTACT,
        /** No record was ever written, so nothing can be checked. */
        NO_RECORD,
        /** At least one file is missing, resized, or hashes differently. */
        DAMAGED,
    }

    /**
     * The manifest text for a set of libraries, sorted by name so two callers cannot produce
     * different text for the same set.
     */
    public static String build(Map<String, Entry> entries) {
        if (entries == null || entries.isEmpty()) return "";
        List<String> names = new ArrayList<>(entries.keySet());
        Collections.sort(names);
        StringBuilder builder = new StringBuilder(names.size() * 96);
        for (String name : names) {
            Entry entry = entries.get(name);
            if (entry == null) continue;
            builder.append(entry.name).append('|')
                    .append(entry.size).append('|')
                    .append(entry.lastModified).append('|')
                    .append(entry.sha256).append('\n');
        }
        return builder.toString();
    }

    /** Reads back what {@link #build} wrote. Malformed lines are skipped, never guessed at. */
    public static Map<String, Entry> parse(String text) {
        Map<String, Entry> out = new LinkedHashMap<>();
        if (text == null || text.isEmpty()) return out;
        for (String line : text.split("\n")) {
            if (line.isEmpty()) continue;
            String[] parts = line.split("\\|", -1);
            if (parts.length != 4) continue;
            if (parts[0].isEmpty() || parts[3].isEmpty()) continue;
            try {
                out.put(parts[0], new Entry(parts[0], Long.parseLong(parts[1]), Long.parseLong(parts[2]), parts[3]));
            } catch (NumberFormatException ignored) {
                AppLog.warnOnce("CaramelClient", "LibraryHashManifest:99", "Ignored a failure at LibraryHashManifest:99", ignored);
            }
        }
        return out;
    }

    /**
     * Whether the files in {@code dir} still match {@code record}.
     *
     * @param hasher the digest for one file, injected so the rule is testable without hashing
     *               anything real. Only called for a file whose size or mtime already changed.
     */
    public static Verdict verify(Map<String, Entry> record,
                                File dir,
                                Sha256 hasher) {
        if (record == null || record.isEmpty()) return Verdict.NO_RECORD;
        if (dir == null || !dir.isDirectory()) return Verdict.DAMAGED;
        for (Entry entry : record.values()) {
            File file = new File(dir, entry.name);
            if (!file.isFile() || file.length() <= 0L) return Verdict.DAMAGED;
            if (file.length() == entry.size && file.lastModified() == entry.lastModified) {
                // Untouched since the record was written, so the recorded hash still applies and
                // there is nothing to re-read.
                continue;
            }
            if (!entry.sha256.equalsIgnoreCase(hasher.hash(file))) {
                return Verdict.DAMAGED;
            }
        }
        return Verdict.INTACT;
    }

    /** A SHA-256 over a file, or over bytes. */
    public interface Sha256 {
        String hash(File file);
    }

    /** The real digest. Kept here so the pure rule above needs no Android and no clock. */
    public static final Sha256 REAL = new Sha256() {
        @Override
        public String hash(File file) {
            try (FileInputStream in = new FileInputStream(file)) {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
                return toHex(digest.digest());
            } catch (IOException | NoSuchAlgorithmException e) {
                // A file that cannot be read cannot be verified; a sentinel that never equals a
                // real digest makes the caller treat it as damaged and re-extract.
                return "";
            }
        }
    };

    /** Records one file. Returns null when it cannot be read, so the caller records nothing. */
    public static Entry entryFor(File file, Sha256 hasher) {
        if (file == null || !file.isFile()) return null;
        String hash = hasher.hash(file);
        if (hash.isEmpty()) return null;
        return new Entry(file.getName(), file.length(), file.lastModified(), hash);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format(Locale.US, "%02x", b));
        }
        return builder.toString();
    }
}

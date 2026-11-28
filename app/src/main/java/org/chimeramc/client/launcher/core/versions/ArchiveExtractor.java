package org.chimeramc.client.core.versions;

import org.chimeramc.client.util.AppLog;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.function.IntConsumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Unpacks a game package (XAPK, APK-as-zip or instance zip) into a scratch directory.
 *
 * <p>Split out of {@code VersionManager} so the one part of an import that parses hostile input —
 * an archive from anywhere on the device — is small enough to read in one sitting. Entries that
 * resolve outside the output directory are skipped rather than written, so a crafted name such as
 * {@code ../../files/x.apk} cannot place a file outside the app's own scratch space.
 */
final class ArchiveExtractor {

    private static final String TAG = "ArchiveExtractor";
    private static final int BUFFER_SIZE = 8192;

    private ArchiveExtractor() {
    }

    /** Reports 10 to 50 percent, the share of the import this step owns. */
    static void extract(File archive, File outputDir, IntConsumer onProgress) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(archive)))) {
            int totalEntries = Math.max(1, countEntries(archive));
            int processed = 0;
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                if (ImportedPackageLayout.isUnsafeEntry(entry.getName())) {
                    AppLog.warnOnce(TAG, "unsafe-entry",
                            "Skipped an archive entry that points outside the import directory", null);
                    zis.closeEntry();
                    continue;
                }
                File outputFile = new File(outputDir, entry.getName());
                if (entry.isDirectory()) {
                    outputFile.mkdirs();
                } else {
                    File parent = outputFile.getParentFile();
                    if (parent != null) parent.mkdirs();
                    try (FileOutputStream out = new FileOutputStream(outputFile)) {
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            out.write(buffer, 0, len);
                        }
                    }
                }

                processed++;
                onProgress.accept(10 + (processed * 40 / totalEntries));
                zis.closeEntry();
            }
        }
    }

    private static int countEntries(File zipFile) throws IOException {
        int count = 0;
        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(zipFile)))) {
            while (zis.getNextEntry() != null) {
                count++;
                zis.closeEntry();
            }
        }
        return count;
    }
}

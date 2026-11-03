package org.chimeramc.client.core.minecraft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * An extracted library must be hash-verified, not merely present and the right length.
 *
 * <p>Drives the real rule against a real filesystem with the real SHA-256, so the test proves the
 * property the launcher depends on rather than a mock's idea of it.
 */
public class LibraryHashManifestTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private final LibraryHashManifest.Sha256 real = LibraryHashManifest.REAL;

    private File write(String name, String content) throws IOException {
        File file = folder.newFile(name);
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private Map<String, LibraryHashManifest.Entry> recordOf(File... files) {
        Map<String, LibraryHashManifest.Entry> out = new LinkedHashMap<>();
        for (File file : files) {
            LibraryHashManifest.Entry entry = LibraryHashManifest.entryFor(file, real);
            assertNotNull("entryFor must record a readable file", entry);
            out.put(entry.name, entry);
        }
        return out;
    }

    @Test
    public void anUntouchedCacheVerifiesIntact() throws IOException {
        File a = write("liba.so", "alpha");
        File b = write("libb.so", "beta");

        assertEquals(LibraryHashManifest.Verdict.INTACT,
                LibraryHashManifest.verify(recordOf(a, b), folder.getRoot(), real));
    }

    @Test
    public void aMissingLibraryIsDamaged() throws IOException {
        File a = write("liba.so", "alpha");
        Map<String, LibraryHashManifest.Entry> record = recordOf(a);
        assertTrue(a.delete());

        assertEquals(LibraryHashManifest.Verdict.DAMAGED,
                LibraryHashManifest.verify(record, folder.getRoot(), real));
    }

    @Test
    public void aTruncatedLibraryIsDamaged() throws IOException {
        File a = write("liba.so", "0123456789");
        Map<String, LibraryHashManifest.Entry> record = recordOf(a);
        Files.write(a.toPath(), "01234".getBytes(StandardCharsets.UTF_8));

        assertEquals(LibraryHashManifest.Verdict.DAMAGED,
                LibraryHashManifest.verify(record, folder.getRoot(), real));
    }

    /**
     * The case a length check cannot see: the file is rewritten with different bytes of the same
     * length, so only the hash catches it.
     */
    @Test
    public void sameLengthDifferentBytesIsDamaged() throws IOException {
        File a = write("liba.so", "AAAAAAAA");
        Map<String, LibraryHashManifest.Entry> record = recordOf(a);
        Files.write(a.toPath(), "BBBBBBBB".getBytes(StandardCharsets.UTF_8));
        // Force the metadata shortcut open, so the hash is what decides. Without this a fast
        // rewrite could land in the same mtime tick and the test would pass for the wrong reason.
        assertTrue(a.setLastModified(record.get("liba.so").lastModified + 5000));
        assertEquals("the size must still match, or the length check would be what catches it",
                record.get("liba.so").size, a.length());

        assertEquals(LibraryHashManifest.Verdict.DAMAGED,
                LibraryHashManifest.verify(record, folder.getRoot(), real));
    }

    @Test
    public void noRecordMeansNothingCanBeChecked() {
        assertEquals(LibraryHashManifest.Verdict.NO_RECORD,
                LibraryHashManifest.verify(null, folder.getRoot(), real));
    }

    /** A cache hit must not read the files: the metadata shortcut has to hold. */
    @Test
    public void anUntouchedFileIsNeverRehashed() throws IOException {
        File a = write("liba.so", "alpha");
        Map<String, LibraryHashManifest.Entry> record = recordOf(a);
        final int[] hashes = {0};
        LibraryHashManifest.Sha256 counting = file -> {
            hashes[0]++;
            return real.hash(file);
        };

        assertEquals(LibraryHashManifest.Verdict.INTACT,
                LibraryHashManifest.verify(record, folder.getRoot(), counting));
        assertEquals("a cache hit must not hash anything", 0, hashes[0]);
    }

    /** A changed file must be re-hashed, and a matching hash must still verify intact. */
    @Test
    public void aTouchedFileIsRehashedAndStillVerifies() throws IOException {
        File a = write("liba.so", "alpha");
        Map<String, LibraryHashManifest.Entry> record = recordOf(a);
        // Touch only: the bytes are the same, so the hash matches and the cache is still good.
        assertTrue(a.setLastModified(record.get("liba.so").lastModified + 5000));
        final int[] hashes = {0};
        LibraryHashManifest.Sha256 counting = file -> {
            hashes[0]++;
            return real.hash(file);
        };

        assertEquals(LibraryHashManifest.Verdict.INTACT,
                LibraryHashManifest.verify(record, folder.getRoot(), counting));
        assertEquals("a touched file must be re-hashed", 1, hashes[0]);
    }

    @Test
    public void roundTripsThroughItsTextFormat() throws IOException {
        File a = write("liba.so", "alpha");
        File b = write("libb.so", "beta");

        String text = LibraryHashManifest.build(recordOf(a, b));
        Map<String, LibraryHashManifest.Entry> parsed = LibraryHashManifest.parse(text);

        assertEquals(2, parsed.size());
        assertEquals(LibraryHashManifest.Verdict.INTACT,
                LibraryHashManifest.verify(parsed, folder.getRoot(), real));
    }

    @Test
    public void malformedLinesAreSkippedNotGuessed() {
        Map<String, LibraryHashManifest.Entry> parsed =
                LibraryHashManifest.parse("liba.so|10|20|abc\nnot a line\nlibb.so|x|y|z\n");
        assertEquals(1, parsed.size());
        assertTrue(parsed.containsKey("liba.so"));
    }

    @Test
    public void entryForRefusesAMissingFile() {
        assertNull(LibraryHashManifest.entryFor(new File(folder.getRoot(), "nope.so"), real));
        assertNull(LibraryHashManifest.entryFor(null, real));
    }

    @Test
    public void aNonDirectoryIsDamaged() throws IOException {
        File a = write("liba.so", "alpha");
        Map<String, LibraryHashManifest.Entry> record = recordOf(a);
        assertEquals(LibraryHashManifest.Verdict.DAMAGED,
                LibraryHashManifest.verify(record, new File(folder.getRoot(), "nope"), real));
    }

    @Test
    public void aZeroLengthFileIsDamaged() throws IOException {
        File a = folder.newFile("liba.so");
        Map<String, LibraryHashManifest.Entry> record =
                new LinkedHashMap<>();
        record.put("liba.so", new LibraryHashManifest.Entry("liba.so", 0L, a.lastModified(), "deadbeef"));

        assertEquals(LibraryHashManifest.Verdict.DAMAGED,
                LibraryHashManifest.verify(record, folder.getRoot(), real));
    }

    @Test
    public void theBuildOrderIsStableRegardlessOfInsertionOrder() throws IOException {
        File a = write("liba.so", "alpha");
        File b = write("libb.so", "beta");
        Map<String, LibraryHashManifest.Entry> forward = recordOf(a, b);
        Map<String, LibraryHashManifest.Entry> reverse = recordOf(b, a);

        assertEquals(LibraryHashManifest.build(forward), LibraryHashManifest.build(reverse));
        assertFalse(LibraryHashManifest.build(forward).isEmpty());
    }
}

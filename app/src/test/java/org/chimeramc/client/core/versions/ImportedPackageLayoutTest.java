package org.chimeramc.client.core.versions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The layout rules that decide whether an imported package launches.
 *
 * <p>A real XAPK keeps {@code base.apk}, its splits and {@code manifest.json} at the root and the
 * OBB under {@code Android/obb/}. The installer only reads {@code apk/} and {@code obb/}, so before
 * this step an XAPK became an empty instance: it showed in the list and refused to launch. These
 * tests build that tree on disk and check where each part ends up.
 */
public class ImportedPackageLayoutTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void anXapkRootIsCollectedIntoTheInstallerLayout() throws IOException {
        File root = folder.newFolder("extracted");
        File base = write(new File(root, "base.apk"), 4096);
        File split = write(new File(root, "split_config.arm64_v8a.apk"), 1024);
        File obb = write(new File(root, "Android/obb/com.mojang.minecraftpe/main.118.com.mojang.minecraftpe.obb"), 64);
        write(new File(root, "manifest.json"), 32);
        long baseLen = base.length();
        long splitLen = split.length();
        long obbLen = obb.length();

        ImportedPackageLayout.Result result = ImportedPackageLayout.normalise(root);

        assertTrue(result.installable());
        assertEquals(1, result.obbs);
        assertTrue(result.manifest);
        File apkDir = new File(root, "apk");
        assertEquals(baseLen, new File(apkDir, "base.apk").length());
        assertEquals(splitLen, new File(apkDir, "split_config.arm64_v8a.apk").length());
        assertEquals(obbLen, new File(root, "obb/main.118.com.mojang.minecraftpe.obb").length());
        assertTrue(new File(root, "manifest.json").isFile());
    }

    @Test
    public void anInstanceNestedOneFolderDownStillFindsItsBaseApk() throws IOException {
        File root = folder.newFolder("nested");
        write(new File(root, "MyInstance/game.apk"), 2048);
        write(new File(root, "MyInstance/obb/main.1.obb"), 16);

        ImportedPackageLayout.Result result = ImportedPackageLayout.normalise(root);

        assertTrue(result.installable());
        assertTrue(new File(root, "apk/base.apk").isFile());
        assertTrue(new File(root, "obb/main.1.obb").isFile());
    }

    @Test
    public void theLargestApkIsTheBaseAndTheOthersStayRecognisable() throws IOException {
        File root = folder.newFolder("splits");
        write(new File(root, "tiny.apk"), 100);
        write(new File(root, "the-real-one.apk"), 9000);

        ImportedPackageLayout.normalise(root);

        assertEquals(9000, new File(root, "apk/base.apk").length());
        assertEquals(100, new File(root, "apk/tiny.apk").length());
    }

    @Test
    public void normaliseIsSafeToRunTwice() throws IOException {
        File root = folder.newFolder("twice");
        write(new File(root, "base.apk"), 512);
        ImportedPackageLayout.normalise(root);
        ImportedPackageLayout.Result second = ImportedPackageLayout.normalise(root);

        assertTrue(second.installable());
        assertTrue(new File(root, "apk/base.apk").isFile());
    }

    @Test
    public void aPackageWithNoApkIsNotInstallable() throws IOException {
        File root = folder.newFolder("empty");
        write(new File(root, "readme.txt"), 10);

        ImportedPackageLayout.Result result = ImportedPackageLayout.normalise(root);

        assertFalse(result.installable());
        assertEquals(0, result.apks);
    }

    @Test
    public void theKindComesFromContentAsWellAsTheName() throws IOException {
        File apk = write(folder.newFile("game.apk"), 8);
        File zip = zip(folder.newFile("instance.zip"));
        File renamed = zip(folder.newFile("game.bin"));
        File text = write(folder.newFile("notes.txt"), 8);

        assertEquals(ImportedPackageLayout.Kind.APK, ImportedPackageLayout.detect(apk));
        assertEquals(ImportedPackageLayout.Kind.ARCHIVE, ImportedPackageLayout.detect(zip));
        assertEquals("a zip renamed still opens", ImportedPackageLayout.Kind.ARCHIVE,
                ImportedPackageLayout.detect(renamed));
        assertEquals(ImportedPackageLayout.Kind.UNKNOWN, ImportedPackageLayout.detect(text));
    }

    @Test
    public void entriesThatEscapeTheImportDirectoryAreRejected() {
        assertTrue(ImportedPackageLayout.isUnsafeEntry("../files/x.apk"));
        assertTrue(ImportedPackageLayout.isUnsafeEntry("a/../../b"));
        assertTrue(ImportedPackageLayout.isUnsafeEntry("..\\windows\\x"));
        assertTrue(ImportedPackageLayout.isUnsafeEntry("/absolute/path"));
        assertFalse(ImportedPackageLayout.isUnsafeEntry("base.apk"));
        assertFalse(ImportedPackageLayout.isUnsafeEntry("Android/obb/pkg/main.1.obb"));
        assertFalse(ImportedPackageLayout.isUnsafeEntry("two..dots.apk"));
    }

    private static File write(File file, int bytes) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("could not create " + parent);
        }
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(new byte[bytes]);
        }
        return file;
    }

    private static File zip(File file) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file))) {
            zos.putNextEntry(new ZipEntry("base.apk"));
            zos.write("not-a-real-apk".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return file;
    }
}

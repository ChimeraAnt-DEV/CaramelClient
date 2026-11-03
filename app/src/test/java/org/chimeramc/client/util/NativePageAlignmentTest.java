package org.chimeramc.client.util;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Test;

/**
 * Every bundled 64-bit native library must be linkable on a 16 KB page device.
 *
 * <p>Newer Android releases load shared objects with 16 KB page granularity; an ELF whose
 * {@code PT_LOAD} segments are aligned to only 4 KB fails to load there ("dlopen failed: empty/
 * misaligned segment") while every older device silently accepts it. The failure therefore only
 * appears on the newest hardware, which is exactly the device a developer is least likely to test on.
 *
 * <p>The preloader is built with {@code -Wl,-z,max-page-size=16384} (see its CMakeLists.txt), but the
 * prebuilt libraries dropped into {@code jniLibs/} are produced elsewhere, so this reads each one's
 * program headers and asserts the alignment. Reading the real bytes is the only way to catch a
 * prebuilt that was rebuilt without the flag.
 */
public class NativePageAlignmentTest {

    /** The ABI the APK ships (arm64-v8a only), so this is the only directory to check. */
    private static final Path JNI_LIBS = Paths.get("src", "main", "jniLibs", "arm64-v8a");

    private static final long SIXTEEN_KB = 16 * 1024L;
    private static final int PT_LOAD = 1;

    @Test
    public void everyBundledLibraryIsSixteenKilobytePageAligned() throws IOException {
        Path dir = findDir(JNI_LIBS);
        List<Path> libs = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> p.toString().endsWith(".so")).forEach(libs::add);
        }
        assertTrue("no bundled libraries found under " + dir, !libs.isEmpty());

        List<String> failures = new ArrayList<>();
        for (Path lib : libs) {
            long align = maxLoadAlignment(Files.readAllBytes(lib));
            if (align < SIXTEEN_KB) {
                failures.add(lib.getFileName() + " (PT_LOAD align " + align + ")");
            }
        }
        if (!failures.isEmpty()) {
            fail("these bundled libraries are not 16 KB page aligned, so they will not load on newer"
                    + " Android devices. Rebuild them with -Wl,-z,max-page-size=16384:\n  "
                    + String.join("\n  ", failures));
        }
    }

    /** The largest {@code p_align} across the ELF's {@code PT_LOAD} segments, or 0 if unreadable. */
    private static long maxLoadAlignment(byte[] elf) {
        if (elf.length < 64 || elf[0] != 0x7f || elf[1] != 'E' || elf[2] != 'L' || elf[3] != 'F') {
            return 0;
        }
        if (elf[4] != 2) {
            // 32-bit ELF: not what this APK ships, but do not crash on it.
            return 0;
        }
        ByteBuffer buf = ByteBuffer.wrap(elf).order(ByteOrder.LITTLE_ENDIAN);
        long phoff = buf.getLong(0x20);
        int phentsize = buf.getShort(0x36) & 0xffff;
        int phnum = buf.getShort(0x38) & 0xffff;
        long maxAlign = 0;
        for (int i = 0; i < phnum; i++) {
            int off = (int) (phoff + (long) i * phentsize);
            if (off < 0 || off + 0x38 > elf.length) break;
            int type = buf.getInt(off);
            if (type != PT_LOAD) continue;
            long align = buf.getLong(off + 0x30);
            if (align > maxAlign) maxAlign = align;
        }
        return maxAlign;
    }

    private static Path findDir(Path relative) {
        if (Files.isDirectory(relative)) return relative;
        Path candidate = Paths.get("app").resolve(relative);
        if (Files.isDirectory(candidate)) return candidate;
        candidate = Paths.get("..").resolve(relative);
        if (Files.isDirectory(candidate)) return candidate;
        throw new IllegalStateException("cannot locate " + relative + " from "
                + Paths.get("").toAbsolutePath());
    }
}

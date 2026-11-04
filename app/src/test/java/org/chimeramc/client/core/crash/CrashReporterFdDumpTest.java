package org.chimeramc.client.core.crash;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * xCrash must not abort while dumping the file-descriptor table.
 *
 * <p>Two crash reports from the field (Share(7) and Share(8), five minutes apart) are the same
 * failure, and it is <em>xCrash itself</em> that dies, not the app: the tombstone's own backtrace is
 * {@code xcrash.Util.getFds -> Os.readlink -> Linux_readlink -> CheckJNI::NewStringUTF}, and the
 * abort message is {@code JNI DETECTED ERROR IN APPLICATION: input is not valid Modified UTF-8:
 * illegal start byte 0x87}. Enumerating {@code /proc/self/fd} reads each link's target, and one
 * target is not valid Modified UTF-8, so JNI aborts the process while writing the crash report. That
 * turns a reportable crash into a crash loop: the user sees the app die and gets no useful report.
 *
 * <p>The FD table is diagnostic garnish — the tombstone is useful without it. Disabling the FD dump
 * for the Java, native and ANR handlers removes the abort at its source, so the report is written and
 * the real failure is reported.
 *
 * <p>This reads the real configuration, because the handler wiring only runs on a device.
 */
public class CrashReporterFdDumpTest {

    @Test
    public void theFdDumpIsDisabledOnEveryHandler() throws IOException {
        String src = read("app/src/main/java/org/chimeramc/client/launcher/core/crash/"
                + "CrashReporter.kt");
        assertTrue("the Java handler must not dump fds, or a non-UTF-8 fd target aborts the "
                + "process inside JNI while the report is written",
                src.contains("setJavaDumpFds(false)"));
        assertTrue("the native handler must not dump fds",
                src.contains("setNativeDumpFds(false)"));
        assertTrue("the ANR handler must not dump fds",
                src.contains("setAnrDumpFds(false)"));
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(root().resolve(relativePath)), StandardCharsets.UTF_8);
    }

    private static Path root() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (Files.exists(dir.resolve("app/src/main/res/layout"))) return dir;
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find the project root");
    }
}

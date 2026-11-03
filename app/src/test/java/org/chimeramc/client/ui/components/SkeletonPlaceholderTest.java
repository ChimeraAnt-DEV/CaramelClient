package org.chimeramc.client.ui.components;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * The list screens show a skeleton, not a spinner.
 *
 * <p>A spinner communicates "wait"; a skeleton communicates "a grid of cards is coming", which is
 * the difference between a delay that reads as progress and one that reads as a stall. The swap is
 * a resource change, so a headless build machine cannot see a spinner creep back in — this reads
 * the real layouts and fails if one does.
 */
public class SkeletonPlaceholderTest {

    private static final String[] LIST_LAYOUTS = {
            "app/src/main/res/layout/activity_content_list.xml",
            "app/src/main/res/layout/activity_curseforge.xml",
            "app/src/main/res/layout/activity_mod_hub.xml",
            "app/src/main/res/layout/activity_modrinth_detail.xml",
    };

    @Test
    public void everyListLoadingPlaceholderIsASkeleton() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (String layout : LIST_LAYOUTS) {
            String xml = read(layout);
            if (xml.contains("<ProgressBar")) {
                offenders.add(layout);
            }
        }
        assertTrue("a list screen still declares a spinner; it should use SkeletonView so the "
                        + "placeholder says what is coming: " + offenders,
                offenders.isEmpty());
    }

    @Test
    public void theSkeletonIsTheSharedComponentNotAPerScreenCopy() throws IOException {
        for (String layout : LIST_LAYOUTS) {
            String xml = read(layout);
            assertTrue(layout + " should inflate the shared SkeletonView",
                    xml.contains("org.chimeramc.client.ui.components.SkeletonView"));
        }
    }

    @Test
    public void theReplaySkeletonIsGoneAndReusesTheSharedView() throws IOException {
        assertFalse("ReplaySkeletonView was folded into the shared SkeletonView",
                exists("app/src/main/java/org/chimeramc/client/core/replay/ReplaySkeletonView.java"));
        String panel = read("app/src/main/java/org/chimeramc/client/core/replay/ReplayPanel.java");
        assertTrue("the Replay panel should build the shared SkeletonView",
                panel.contains("org.chimeramc.client.ui.components.SkeletonView"));
    }

    /**
     * Resolves a repo-relative file, searching upward from the test working directory.
     *
     * <p>Gradle runs a unit test with a working directory that is not always the repo root, so a
     * plain relative path is not reliable; the same upward walk the nav resource test uses keeps
     * this test correct wherever it is launched from.
     */
    private static String read(String relative) throws IOException {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            Path candidate = dir.resolve(relative);
            if (Files.exists(candidate)) {
                return new String(Files.readAllBytes(candidate), StandardCharsets.UTF_8);
            }
            dir = dir.getParent();
        }
        throw new IOException("could not locate " + relative + " from " + Paths.get("").toAbsolutePath());
    }

    /** True when the repo-relative file exists, searching upward from the working directory. */
    private static boolean exists(String relative) {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (Files.exists(dir.resolve(relative))) return true;
            dir = dir.getParent();
        }
        return false;
    }
}

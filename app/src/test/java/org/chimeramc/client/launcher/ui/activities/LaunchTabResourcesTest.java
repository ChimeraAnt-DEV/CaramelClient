package org.chimeramc.client.launcher.ui.activities;

import static org.junit.Assert.assertFalse;

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
 * The launch tab carries no floating head.
 *
 * <p>The launch screen used to open with a glass disc holding the player's skin head, turning on
 * its own axis above the hero panel. It was decoration the launch flow does not need: it pushed the
 * instance identity down the screen, spent a software-rasterised turntable every frame, and its
 * only unique action (open the profile sheet) is already reachable from the Cosmetics control.
 *
 * <p>A build machine cannot see a head sitting in a layout, so the removal is asserted here against
 * the real resources and source: the include is gone, the layout file is gone, and nothing
 * references the view class. A future edit that reintroduces any of the three fails the build.
 */
public class LaunchTabResourcesTest {

    private static final String[] HEAD_POD_MARKERS = {
            "HeadPodView", "HeadPodMotion", "HeadPodSync", "launch_head_pod"
    };

    @Test
    public void theLaunchTabDoesNotIncludeAHeadPod() throws IOException {
        String main = read("app/src/main/res/layout/activity_main.xml");
        assertFalse("the launch tab must not include the floating head pod",
                main.contains("@layout/include_launch_head_pod"));
    }

    @Test
    public void theHeadPodLayoutIsGone() {
        assertFalse("include_launch_head_pod.xml is the floating head's layout and must not exist",
                Files.exists(projectRoot().resolve(
                        "app/src/main/res/layout/include_launch_head_pod.xml")));
    }

    @Test
    public void nothingReferencesTheHeadPod() throws IOException {
        List<String> offenders = new ArrayList<>();
        Path root = resolve("app/src/main");
        try (Stream<Path> s = Files.walk(root)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                String file = p.getFileName().toString();
                if (!(file.endsWith(".java") || file.endsWith(".kt") || file.endsWith(".xml"))) {
                    continue;
                }
                String text = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
                for (String marker : HEAD_POD_MARKERS) {
                    if (text.contains(marker)) {
                        offenders.add(root.relativize(p) + " -> " + marker);
                    }
                }
            }
        }
        assertFalse("the floating head was removed, so no source may reference it: " + offenders,
                !offenders.isEmpty());
    }

    private static Path projectRoot() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            if (Files.exists(dir.resolve("app/src/main/res/layout"))) return dir;
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find the project root");
    }

    private static Path resolve(String relativePath) {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            Path candidate = dir.resolve(relativePath);
            if (Files.exists(candidate)) return candidate;
            dir = dir.getParent();
        }
        throw new IllegalStateException("could not find " + relativePath);
    }

    private static String read(String relativePath) throws IOException {
        return new String(Files.readAllBytes(resolve(relativePath)), StandardCharsets.UTF_8);
    }
}

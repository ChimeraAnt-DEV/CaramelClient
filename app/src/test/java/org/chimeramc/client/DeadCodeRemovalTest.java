package org.chimeramc.client;

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
 * Classes that nothing referenced are gone, not left to rot.
 *
 * <p>{@code GyroMod} was a wrapper with no caller: the gyro overlay sends its look delta through
 * {@code PojavControlsMod}, so the wrapper was a second, unused route to the same call.
 * {@code SignallingCancellation} was a cancellation implementation no production path constructed —
 * the one live wait builds its own anonymous {@code Cancellation}. Both were reachable only from
 * their own tests, which is not a reason to keep code.
 *
 * <p>This asserts they stay gone, so a copy from an old branch cannot quietly reintroduce a dead
 * route that looks like a feature.
 */
public class DeadCodeRemovalTest {

    private static final String[] REMOVED = {
            "GyroMod",
            "SignallingCancellation",
    };

    @Test
    public void theUnreferencedClassesAreGone() {
        for (String name : REMOVED) {
            for (String root : new String[]{"app/src/main", "app/src/test"}) {
                Path dir = projectRoot().resolve(root);
                try (Stream<Path> s = Files.walk(dir)) {
                    for (Path p : (Iterable<Path>) s::iterator) {
                        if (p.getFileName().toString().equals(name + ".java")) {
                            throw new AssertionError(name + " was removed and must not exist: " + p);
                        }
                    }
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    @Test
    public void nothingStillNamesTheRemovedClasses() throws IOException {
        List<String> offenders = new ArrayList<>();
        // Production source only: this test names the classes on purpose, and a test that asserts a
        // class is gone is not a reference to it.
        Path root = projectRoot().resolve("app/src/main");
        try (Stream<Path> s = Files.walk(root)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                String file = p.getFileName().toString();
                if (!(file.endsWith(".java") || file.endsWith(".kt"))) continue;
                String text = new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
                for (String name : REMOVED) {
                    if (text.contains(name)) {
                        offenders.add(projectRoot().relativize(p) + " -> " + name);
                    }
                }
            }
        }
        assertFalse("no production source may reference a removed dead class: " + offenders,
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
}

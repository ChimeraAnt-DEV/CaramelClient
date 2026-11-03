package org.chimeramc.client.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * The preloader submodule is pinned to a commit, not a branch.
 *
 * <p>A gitlink is always a commit SHA, so the only way a submodule silently floats is a
 * {@code branch =} entry in {@code .gitmodules}: it lets {@code git submodule update --remote} move
 * the checkout to whatever that branch now points at, which turns a reproducible build into a
 * moving target. The rule is that the pin is bumped in a dedicated commit, so the branch entry must
 * not exist and the preloader gitlink must be recorded in the index.
 *
 * <p>{@code scripts/check_submodules.sh} enforces the same rule in CI against the checked-out tree;
 * this test is the in-suite guard so a local {@code ./gradlew test} catches it too.
 */
public class SubmodulePinTest {

    private static final Path GITMODULES = Paths.get("..", ".gitmodules");

    @Test
    public void noSubmoduleTracksABranch() throws IOException {
        if (!Files.isRegularFile(GITMODULES)) return; // a source tarball has no submodules
        String text = new String(Files.readAllBytes(GITMODULES), StandardCharsets.UTF_8);
        for (String line : text.split("\n")) {
            assertFalse("a submodule must be pinned to a commit, not a branch: " + line.trim(),
                    line.trim().startsWith("branch =") || line.trim().startsWith("branch="));
        }
    }

    @Test
    public void thePreloaderSubmoduleIsDeclared() throws IOException {
        if (!Files.isRegularFile(GITMODULES)) return;
        List<String> lines = Files.readAllLines(GITMODULES, StandardCharsets.UTF_8);
        boolean declared = false;
        for (String line : lines) {
            if (line.trim().equals("path = app/src/main/cpp/preloader")) {
                declared = true;
                break;
            }
        }
        assertTrue("the preloader submodule must stay declared in .gitmodules", declared);
    }
}

package org.chimeramc.client.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.After;
import org.junit.Test;

/**
 * Every API gate lives in one class, and that class is right on both sides of each boundary
 *.
 *
 * <p>The tree had 70-odd inline {@code Build.VERSION.SDK_INT} comparisons. An inline gate cannot be
 * checked on the side the test machine is not running, so the wrong direction on a boundary ships
 * unnoticed; and when {@code minSdk} moves there is no list of the places that must move with it.
 *
 * <p>Two halves. The first pins the predicates against a fixed level, which is the only way to
 * exercise an API gate off-device. The second scans the real source tree and fails if a raw
 * {@code SDK_INT} comparison is written again — the ratchet that keeps the single owner honest.
 */
public class ApiLevelsTest {

    private static final Path SOURCE_ROOT = Paths.get("src", "main", "java");

    @After
    public void clearOverride() {
        ApiLevels.setLevelForTest(null);
    }

    @Test
    public void everyNamedGateIsExactOnBothSidesOfItsBoundary() {
        assertBoundary(23, ApiLevels::atLeastM, ApiLevels::belowM);
        assertBoundary(24, ApiLevels::atLeastN, null);
        assertBoundary(26, ApiLevels::atLeastO, ApiLevels::belowO);
        assertBoundary(28, ApiLevels::atLeastP, null);
        assertBoundary(29, ApiLevels::atLeastQ, ApiLevels::belowQ);
        assertBoundary(30, ApiLevels::atLeastR, ApiLevels::belowR);
        assertBoundary(31, ApiLevels::atLeastS, ApiLevels::belowS);
        assertBoundary(33, ApiLevels::atLeastTiramisu, ApiLevels::belowTiramisu);
        assertBoundary(34, ApiLevels::atLeastUpsideDownCake, null);
    }

    private static void assertBoundary(int level, java.util.function.BooleanSupplier atLeast,
                                       java.util.function.BooleanSupplier below) {
        ApiLevels.setLevelForTest(level - 1);
        assertFalse("atLeast must be false one below " + level, atLeast.getAsBoolean());
        if (below != null) assertTrue("below must be true one below " + level, below.getAsBoolean());

        ApiLevels.setLevelForTest(level);
        assertTrue("atLeast must be true at " + level, atLeast.getAsBoolean());
        if (below != null) assertFalse("below must be false at " + level, below.getAsBoolean());
    }

    @Test
    public void theRawComparisonAndTheHelpersAgree() {
        ApiLevels.setLevelForTest(29);
        assertTrue(ApiLevels.atLeast(29));
        assertTrue(ApiLevels.atLeastQ());
        assertFalse(ApiLevels.atLeastR());
        assertTrue(ApiLevels.belowR());
        assertFalse(ApiLevels.belowQ());
    }

    @Test
    public void noRawSdkIntComparisonRemains() throws IOException {
        Pattern raw = Pattern.compile("Build\\.VERSION\\.SDK_INT\\s*(>=|<=|==|!=|>|<)");
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(SOURCE_ROOT)) {
            for (Path file : (Iterable<Path>) paths
                    .filter(p -> p.toString().endsWith(".java") || p.toString().endsWith(".kt"))
                    ::iterator) {
                String source = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                Matcher m = raw.matcher(source);
                while (m.find()) {
                    int line = source.substring(0, m.start()).split("\n", -1).length;
                    offenders.add(SOURCE_ROOT.relativize(file) + ":" + line);
                }
            }
        }
        if (!offenders.isEmpty()) {
            fail("API gates belong in ApiLevels, where both sides of the boundary are unit-tested; a"
                    + " raw SDK_INT comparison cannot be exercised off-device and is missed when the"
                    + " level moves. Offenders:\n  " + String.join("\n  ", offenders));
        }
    }
}

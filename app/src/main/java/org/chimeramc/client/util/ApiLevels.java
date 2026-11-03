package org.chimeramc.client.util;

import android.os.Build;

/**
 * One owner for every API-level gate in the launcher.
 *
 * <p>{@code minSdk} is 28, yet the tree carried 70-odd {@code ApiLevels.sdkInt()} comparisons
 * scattered across the code, each one a small chance to write the wrong level or the wrong
 * direction. A gate that is written inline cannot be tested on both sides of the boundary without a
 * device, and when a level is bumped there is no single place to look for the ones that must move.
 *
 * <p>Each named predicate here is a pure boolean over {@link #sdkInt()}, so a JVM test can assert
 * both sides of every boundary with a fixed reading. A call site asks a question
 * ({@link #atLeastR()}, {@link #belowQ()}) rather than restating the arithmetic, and the check that
 * matters — {@code ApiLevelsTest} — fails the build if a raw {@code SDK_INT} comparison is written
 * again.
 *
 * <p>The level itself is read through {@link #sdkInt()} rather than {@link Build#VERSION} directly
 * so a test can supply a value; production always uses the platform reading.
 */
public final class ApiLevels {

    /** The level the app itself is built against; the reference point for "modern". */
    public static final int TIRAMISU = 33;

    private static volatile Integer overrideForTest;

    private ApiLevels() {
    }

    /** The running device's API level, or a test-supplied value when one is set. */
    public static int sdkInt() {
        Integer override = overrideForTest;
        return override != null ? override : Build.VERSION.SDK_INT;
    }

    /** True when the device is at or above [level]. The one place the comparison is written. */
    public static boolean atLeast(int level) {
        return sdkInt() >= level;
    }

    /** True when the device is below [level]. */
    public static boolean below(int level) {
        return sdkInt() < level;
    }

    // Named gates. A call site names the platform feature it needs, not a number.

    public static boolean atLeastM() {
        return atLeast(23);
    }

    public static boolean belowM() {
        return below(23);
    }

    public static boolean atLeastN() {
        return atLeast(24);
    }

    public static boolean atLeastO() {
        return atLeast(26);
    }

    public static boolean belowO() {
        return below(26);
    }

    public static boolean atLeastP() {
        return atLeast(28);
    }

    public static boolean atLeastQ() {
        return atLeast(29);
    }

    public static boolean belowQ() {
        return below(29);
    }

    public static boolean atLeastR() {
        return atLeast(30);
    }

    public static boolean belowR() {
        return below(30);
    }

    public static boolean atLeastS() {
        return atLeast(31);
    }

    public static boolean belowS() {
        return below(31);
    }

    public static boolean atLeastTiramisu() {
        return atLeast(TIRAMISU);
    }

    public static boolean belowTiramisu() {
        return below(TIRAMISU);
    }

    public static boolean atLeastUpsideDownCake() {
        return atLeast(34);
    }

    /**
     * Sets a fixed level for a test. Package-private so production code cannot change it; the test
     * lives in the same package.
     */
    static void setLevelForTest(Integer level) {
        overrideForTest = level;
    }
}

package org.chimeramc.client.core.minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The decision half of native-library loading (item 301).
 *
 * <p>{@code GamePackageManager.loadAllLibraries} used to interleave three separate rules — which
 * libraries a 32-bit device may skip, which the caller asked to exclude, and the progress
 * arithmetic — inside one loop that also performed the loads. That made the rules impossible to test
 * without a device and impossible to read: a library excluded for one reason and skipped for another
 * produced the same observable result, so a mistake was invisible.
 *
 * <p>This class owns the rules and returns an ordered plan. The loader walks the plan and performs
 * the {@code dlopen} calls, nothing more. Pure and Android-free, so {@code LibraryLoadPlanTest} pins
 * every branch.
 */
public final class LibraryLoadPlan {

    private LibraryLoadPlan() {
    }

    /** One library the loader should open, with the reason it is in the plan. */
    public static final class Step {
        /** The library name as it appears in the shared required/optional lists. */
        public final String name;
        /** 1-based position among the loaded libraries, for "3/12" progress text. */
        public final int index;
        /** The total number of loaded libraries, for "3/12" progress text. */
        public final int total;

        Step(String name, int index, int total) {
            this.name = name;
            this.index = index;
            this.total = total;
        }
    }

    /**
     * Builds the ordered plan.
     *
     * @param allLibs             every library the launch considers, in load order
     * @param systemLoadedLibs    the subset the platform loads for us ({@code System.loadLibrary});
     *                            skipped on a 32-bit device because they are arm64-only prebuilts
     * @param excludeLibs         names the caller wants skipped for this launch path
     * @param deviceSupports64Bit whether the process can load the arm64-only prebuilts
     */
    public static List<Step> plan(List<String> allLibs,
                                  List<String> systemLoadedLibs,
                                  Set<String> excludeLibs,
                                  boolean deviceSupports64Bit) {
        List<String> eligible = new ArrayList<>();
        for (String lib : allLibs) {
            // PlayFab/maesdk are arm64-only closed-source prebuilts; a 32-bit device has no copy,
            // so excluding them here keeps the failure out of System.loadLibrary's generic error.
            if (systemLoadedLibs.contains(lib) && !deviceSupports64Bit) continue;
            if (isExcluded(lib, excludeLibs)) continue;
            eligible.add(lib);
        }
        List<Step> steps = new ArrayList<>(eligible.size());
        for (int i = 0; i < eligible.size(); i++) {
            steps.add(new Step(eligible.get(i), i + 1, eligible.size()));
        }
        return steps;
    }

    /**
     * The reason a library is skipped, keyed by the name the caller passed.
     *
     * <p>Ordered so the report names the same library the plan did. A caller-supplied reason wins;
     * otherwise the plan states the default. The map is empty when nothing is skipped, so a caller
     * can test {@code isEmpty} rather than scanning.
     */
    public static Map<String, String> skipReasons(List<String> allLibs,
                                                  List<String> systemLoadedLibs,
                                                  Set<String> excludeLibs,
                                                  boolean deviceSupports64Bit,
                                                  Map<String, String> excludeReasons) {
        Map<String, String> out = new LinkedHashMap<>();
        for (String lib : allLibs) {
            if (systemLoadedLibs.contains(lib) && !deviceSupports64Bit) {
                out.put(lib, "arm64-only prebuilt, not bundled on this device");
                continue;
            }
            if (isExcluded(lib, excludeLibs)) {
                String custom = excludeReasons == null ? null : excludeReasons.get(LibraryNames.normalize(lib));
                if (custom == null && excludeReasons != null) custom = excludeReasons.get(lib);
                out.put(lib, custom != null ? custom : "not required in this launch path");
            }
        }
        return Collections.unmodifiableMap(out);
    }

    /**
     * A name matches an exclude entry in either its short or its complete form.
     *
     * <p>Callers pass either form — {@code "HttpClient.Android"} from the preparer,
     * {@code "libHttpClient.Android.so"} from a rule — and both must match, or a library the caller
     * asked to skip would still load.
     */
    public static boolean isExcluded(String lib, Set<String> excludeLibs) {
        if (excludeLibs == null || excludeLibs.isEmpty()) return false;
        return excludeLibs.contains(lib) || excludeLibs.contains(LibraryNames.normalize(lib))
                || excludeLibs.contains(LibraryNames.fileName(lib));
    }

    /**
     * The progress value for a step, spread across {@code [start, end)}.
     *
     * <p>Pure so the arithmetic is testable: the first library reports {@code start}, the last
     * reports short of {@code end}, and a single-library plan does not divide by zero.
     */
    public static int progressFor(int index, int total, int start, int end) {
        int safeTotal = Math.max(1, total);
        int clamped = Math.max(1, Math.min(index, safeTotal));
        return start + ((end - start) * (clamped - 1) / safeTotal);
    }
}

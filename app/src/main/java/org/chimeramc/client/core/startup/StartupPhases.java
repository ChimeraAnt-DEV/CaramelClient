package org.chimeramc.client.core.startup;

import java.util.ArrayList;
import java.util.List;

/**
 * Which startup work must finish before the first frame, and which can wait.
 *
 * <p>{@code Application.onCreate} ran everything on the main thread, including native geometry
 * publishing, the crash handler install and several feature singletons. That work blocks the first
 * frame, which is the "startup time is horrible" report. Most of it is not needed until the feature
 * that owns it is actually used, so it can run on a background executor after the first frame.
 *
 * <p>The classification is here, as data, so it is unit-tested and so a new init step has to say
 * which phase it belongs to. A step is critical only when a screen drawn on the first frame reads it.
 */
public final class StartupPhases {

    /** A startup step. */
    public enum Step {
        /** The failure reporter, so the first failure anywhere is durable. */
        APP_LOG,
        /** Feature settings: almost every screen reads a setting as it inflates. */
        SETTINGS,
        /** StrictMode, debug only; it must be on before the main thread does any work. */
        STRICT_MODE,
        /** The crash handler, so a crash during startup is still recorded. */
        CRASH_REPORTER,
        /** The low-latency network policy: read when a socket is opened, not on the first frame. */
        LOW_LATENCY_NETWORK,
        /** Thermal advice: read when the settings screen opens. */
        THERMAL_GOVERNOR,
        /** The FPS optimizer service: read when a session runs. */
        FPS_OPTIMIZER,
        /** The FPS module service: read when the mod menu opens. */
        FPS_MODULES,
        /** Optifine startup reconciliation: a disk pass over the last session's state. */
        OPTIFINE_RECONCILE,
        /** Hook availability probes: read when a module draws, after the first frame. */
        HOOK_AVAILABILITY,
        /** Storage migration reconciliation: a disk pass. */
        STORAGE_MIGRATION,
        /** The logcat overlay manager: read when the overlay is shown. */
        LOGCAT_OVERLAY,
        /** Playtime: read when an instance card is drawn. */
        PLAYTIME,
        /** The session restarter hook: installed once, needed only when a session starts. */
        SESSION_RESTARTER,
        /** The replay manager singleton: read when a replay screen opens. */
        REPLAY_MANAGER,
        /** Native cosmetics: publishes geometry to the native registry, needed when drawing. */
        NATIVE_COSMETICS,
        /** The account skin re-sync: a cache read plus a background fetch, not needed to draw. */
        ACCOUNT_SKIN_SYNC,
    }

    private StartupPhases() {
    }

    /**
     * The steps that must complete before the first frame.
     *
     * <p>Only the ones a screen drawn on the first frame reads: the logger (so a failure is durable),
     * the settings (every screen reads one), StrictMode (it must be on before main-thread work) and
     * the crash handler (so a startup crash is recorded). Everything else waits.
     */
    public static List<Step> criticalPath() {
        List<Step> out = new ArrayList<>();
        out.add(Step.APP_LOG);
        out.add(Step.STRICT_MODE);
        out.add(Step.SETTINGS);
        out.add(Step.CRASH_REPORTER);
        return out;
    }

    /**
     * The steps that can run after the first frame.
     *
     * <p>Each is read only when its own feature is used, so running it late is invisible; running it
     * on the main thread before the first frame is not.
     */
    public static List<Step> deferred() {
        List<Step> out = new ArrayList<>();
        for (Step s : Step.values()) {
            if (!criticalPath().contains(s)) out.add(s);
        }
        return out;
    }

    /** True when a step must complete before the first frame. */
    public static boolean isCritical(Step step) {
        return step != null && criticalPath().contains(step);
    }

    /**
     * True when every step is classified into exactly one phase.
     *
     * <p>A step that is in neither list would silently never run, which is worse than a slow start.
     */
    public static boolean everyStepIsClassified() {
        for (Step s : Step.values()) {
            boolean critical = criticalPath().contains(s);
            boolean deferred = deferred().contains(s);
            if (critical == deferred) return false;
        }
        return true;
    }
}

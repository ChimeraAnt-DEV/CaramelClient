package org.chimeramc.client.core.backend;

import org.chimeramc.client.util.AppLog;

import java.util.EnumSet;
import java.util.Set;

/**
 * The single registration point for the active {@link GameBackend}.
 *
 * <p>The reference is {@code volatile}, so an input path reads it once per call with no lock and a
 * script reload can swap the backend live. Every call goes through here rather than touching the
 * backend directly, which is what gives the whole client one place to (a) check the capability,
 * (b) swallow a backend that throws, and (c) report each kind of failure exactly once instead of
 * flooding logcat from the frame path.
 *
 * <p>With no backend registered every call is a quiet no-op returning {@code false}, so the launcher
 * still starts the game and the overlays degrade rather than crash.
 */
public final class GameBridge {

    /** Where a failure report goes. Replaced in tests, and in production by the in-app logger. */
    public interface Logger {
        void warn(String message, Throwable cause);
    }

    private static final Logger DEFAULT_LOGGER = (message, cause) -> {
        try {
            android.util.Log.w("GameBridge", message, cause);
        } catch (Throwable ignored) {
            AppLog.warnOnce("CaramelClient", "GameBridge:31", "Ignored a failure at GameBridge:31", ignored);
        }
    };

    private static volatile GameBackend backend;
    private static volatile Logger logger = DEFAULT_LOGGER;
    /**
     * Per-feature call rate, for the developer overlay. Advanced from the call paths
     * below, which run on whichever thread makes the call; the meter is deliberately lock-free.
     */
    private static final BackendCallMeter callMeter = new BackendCallMeter();
    /**
     * Watches for a backend that has stopped answering and, when it trips, releases every injected
     * input and tells the user. A key held down by a hung backend is the failure this
     * exists to prevent.
     */
    private static final BackendDeadManSwitch deadManSwitch = new BackendDeadManSwitch();
    /** Failure kinds already reported, so a per-frame failure logs once, not sixty times a second. */
    private static final Set<GameBackend.Feature> reportedFailures =
            EnumSet.noneOf(GameBackend.Feature.class);

    private GameBridge() {
    }

    /**
     * Installs a backend, replacing any current one.
     *
     * <p>The previous backend is asked to release everything it injected first, because a swap
     * during play (a script reload) would otherwise leave a key held down in the game.
     */
    public static synchronized void register(GameBackend next) {
        if (next == null) {
            unregister();
            return;
        }
        GameBackend previous = backend;
        if (previous != null && previous != next) {
            safeReleaseAll(previous);
        }
        reportedFailures.clear();
        callMeter.reset();
        deadManSwitch.reset();
        backend = next;
    }

    /** Removes the backend, releasing everything it injected. */
    public static synchronized void unregister() {
        GameBackend current = backend;
        backend = null;
        reportedFailures.clear();
        callMeter.reset();
        deadManSwitch.reset();
        if (current != null) {
            safeReleaseAll(current);
        }
    }

    /** The active backend, or null. */
    public static GameBackend backend() {
        return backend;
    }

    /**
     * The per-feature call meter.
     *
     * <p>Exposed rather than hidden behind a getter per feature: the developer overlay reads it on a
     * timer and rotates the bucket itself, so a second in which nothing was called still advances.
     */
    public static BackendCallMeter callMeter() {
        return callMeter;
    }

    /** The dead-man switch. Exposed so a host can reset it on a session boundary. */
    public static BackendDeadManSwitch deadManSwitch() {
        return deadManSwitch;
    }

    public static boolean hasBackend() {
        return backend != null;
    }

    /** The backend's name, or an empty string when none is registered. */
    public static String backendName() {
        GameBackend b = backend;
        return b == null ? "" : b.name();
    }

    /** The backend's version, or an empty string when none is registered. */
    public static String backendVersion() {
        GameBackend b = backend;
        return b == null ? "" : b.version();
    }

    /**
     * The backend's script-API version, or 0 when none is registered.
     *
     * <p>Zero is not a valid {@link GameBackend#API_VERSION}, so a caller can tell "no backend" from
     * "a backend that reports version 0".
     */
    public static int backendApiVersion() {
        GameBackend b = backend;
        return b == null ? 0 : b.apiVersion();
    }

    /** Whether the active backend provides {@code feature}. False when none is registered. */
    public static boolean supports(GameBackend.Feature feature) {
        GameBackend b = backend;
        if (b == null) {
            return false;
        }
        try {
            return b.supports(feature);
        } catch (Throwable t) {
            reportFailure(feature, "supports(" + feature + ")", t);
            return false;
        }
    }

    public static boolean sendKey(int bedrockKeyCode, boolean down) {
        return call(GameBackend.Feature.KEY, b -> b.sendKey(bedrockKeyCode, down));
    }

    public static boolean sendMouseButton(int androidButton, boolean down) {
        return call(GameBackend.Feature.MOUSE_BUTTON, b -> b.sendMouseButton(androidButton, down));
    }

    public static boolean sendScroll(float vertical) {
        return call(GameBackend.Feature.SCROLL, b -> b.sendScroll(vertical));
    }

    public static boolean sendLookDelta(float deltaX, float deltaY) {
        return call(GameBackend.Feature.LOOK_DELTA, b -> b.sendLookDelta(deltaX, deltaY));
    }

    public static boolean sendPointer(float x, float y) {
        return call(GameBackend.Feature.POINTER, b -> b.sendPointer(x, y));
    }

    public static boolean setZoom(boolean active) {
        return call(GameBackend.Feature.ZOOM, b -> b.setZoom(active));
    }

    public static boolean setAutoSprint(boolean enabled) {
        return call(GameBackend.Feature.AUTO_SPRINT, b -> b.setAutoSprint(enabled));
    }

    /** Whether auto-sprint is active. False when no backend provides it. */
    public static boolean isAutoSprintActive() {
        return call(GameBackend.Feature.AUTO_SPRINT, GameBackend::isAutoSprintActive);
    }

    public static boolean selectHotbarSlot(int slot) {
        return call(GameBackend.Feature.HOTBAR_SLOT, b -> b.selectHotbarSlot(slot));
    }

    /**
     * The game's frame rate, or a negative value when it cannot be read.
     *
     * <p>Deliberately not routed through {@link #call}: an unknown reading is a negative number, not
     * a {@code false}, so the fallback in the overlay can tell the two apart.
     */
    public static int readFps() {
        GameBackend b = backend;
        if (b == null || !supports(GameBackend.Feature.FPS)) {
            return -1;
        }
        long now = System.currentTimeMillis();
        callMeter.record(GameBackend.Feature.FPS, now);
        try {
            int fps = b.readFps();
            // A negative reading is "unknown", not a failure: the overlay falls back rather than
            // treating an unsupported readout as a dead backend.
            noteLiveness(fps >= 0, now);
            return fps;
        } catch (Throwable t) {
            reportFailure(GameBackend.Feature.FPS, "readFps", t);
            noteLiveness(false, now);
            return -1;
        }
    }

    /**
     * Whether the last {@link #readFps()} reading is the game's exact frame rate.
     *
     * <p>Exact only when the engine's present hook is live and counting. An inferred tick (the hook
     * absent) or no backend at all reads as approximate, so the FPS overlay marks the number instead
     * of passing a close-but-not-exact rate off as the game's real frame rate. Fail-closed.
     */
    public static boolean isFrameRateExact() {
        GameBackend b = backend;
        if (b == null || !supports(GameBackend.Feature.FPS)) {
            return false;
        }
        try {
            return b.isFrameRateExact();
        } catch (Throwable t) {
            return false;
        }
    }

    /** Releases every key and button the backend injected. Safe to call with no backend. */
    public static void releaseAll() {
        GameBackend b = backend;
        if (b != null) {
            safeReleaseAll(b);
        }
    }

    /**
     * A one-line description of the active backend for About and Diagnostics.
     *
     * <p>Pure string work from the backend's own name/version, so the wording is unit-tested and the
     * two screens cannot describe the same state differently. A build with no backend says so
     * plainly, because "the launcher works but injected input does nothing" is exactly the state a
     * user needs named before they report a bug.
     */
    public static String statusLine() {
        GameBackend b = backend;
        if (b == null) {
            return "none registered (launcher input only)";
        }
        String name = safeText(b.name());
        String version = safeText(b.version());
        int api = b.apiVersion();
        StringBuilder out = new StringBuilder();
        out.append(name.isEmpty() ? "unnamed backend" : name);
        if (!version.isEmpty()) {
            out.append(' ').append(version);
        }
        out.append(" (API v").append(api).append(')');
        return out.toString();
    }

    private static String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    /** Replaces the failure logger. Passing null restores the default. */
    public static void setLogger(Logger next) {
        logger = next == null ? DEFAULT_LOGGER : next;
    }

    private static boolean call(GameBackend.Feature feature, Invocation invocation) {
        GameBackend b = backend;
        if (b == null || !supports(feature)) {
            return false;
        }
        // One clock read for both counters: this runs on the per-frame look path, so a second
        // System.currentTimeMillis call per event is exactly the kind of cost this path must not pay.
        long now = System.currentTimeMillis();
        callMeter.record(feature, now);
        boolean ok;
        try {
            ok = invocation.invoke(b);
        } catch (Throwable t) {
            reportFailure(feature, feature.name(), t);
            ok = false;
        }
        noteLiveness(ok, now);
        return ok;
    }

    /**
     * Feeds one call outcome to the dead-man switch and, when it trips, releases every injected key
     * and button so a hung backend cannot leave input stuck.
     */
    private static void noteLiveness(boolean ok, long now) {
        if (deadManSwitch.record(ok, now)) {
            GameBackend b = backend;
            if (b != null) {
                safeReleaseAll(b);
            }
            logger.warn("GameBackend stopped responding; released all injected input so nothing stays "
                    + "held down. Check the script runtime and reload it.", null);
        }
    }

    private static void safeReleaseAll(GameBackend b) {
        try {
            b.releaseAll();
        } catch (Throwable t) {
            reportFailure(GameBackend.Feature.KEY, "releaseAll", t);
        }
    }

    private static void reportFailure(GameBackend.Feature feature, String call, Throwable cause) {
        boolean first;
        synchronized (reportedFailures) {
            first = reportedFailures.add(feature);
        }
        if (!first) {
            return;
        }
        String name = backendName();
        logger.warn("GameBackend " + (name.isEmpty() ? "<none>" : name) + " failed in " + call
                + "; further " + feature + " failures this session are not logged", cause);
    }

    /** A backend call, so the failure handling is written once. */
    private interface Invocation {
        boolean invoke(GameBackend backend);
    }

    /** Test hook: clears the once-only failure log and the registration. */
    public static synchronized void resetForTests() {
        backend = null;
        reportedFailures.clear();
        callMeter.reset();
        deadManSwitch.reset();
        logger = DEFAULT_LOGGER;
    }
}

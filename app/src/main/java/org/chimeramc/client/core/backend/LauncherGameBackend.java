package org.chimeramc.client.core.backend;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The launcher's own {@link GameBackend}, driving input through the game activity's handler.
 *
 * <p>This replaces the prebuilt {@code libinbuiltmods.so} wrappers that used to provide the same
 * capabilities: the launcher could not rebuild those binaries, so the ability to inject input is
 * implemented here against the game's own entry points instead.
 *
 * <p>Capabilities that genuinely need native code the launcher does not have — a field-of-view
 * change for zoom and reading an item out of a hotbar slot — are reported as <b>not supported</b>
 * rather than faked, so the UI greys them out rather than appearing to work (see
 * {@link GameBackend#supports}). The rest are provided by injecting the same key/mouse events a
 * physical device would produce.
 *
 * <p>The frame rate is provided when a {@link FrameTickSource} is supplied: the preloader's render
 * hook counts one tick per frame the game actually draws, and {@link GameFrameRate} turns that into
 * a rate. With no tick source (a build without the hook) the capability is not advertised and the
 * overlay keeps its host-side meter, so the number is never invented.
 */
public final class LauncherGameBackend implements GameBackend {

    public static final String NAME = "Launcher Input";
    public static final String VERSION = "1.1";

    /** The forward key's Bedrock code, used for the sprint double-tap. */
    private static final int BEDROCK_KEY_W = 87;

    private final GameInputHost host;
    /** Supplies the game's frame tick when the render hook is live; null when there is none. */
    private final FrameTickSource frameTickSource;
    /** Derives the frame rate from the tick source; only sampled when the source exists. */
    private final GameFrameRate frameRate = new GameFrameRate();
    /**
     * The monotonic clock the frame-rate window is measured against. Injectable so the rate path is
     * unit-tested on the JVM, where {@code android.os.SystemClock} is a stub that throws.
     */
    private java.util.function.LongSupplier clock = android.os.SystemClock::uptimeMillis;
    /** Capabilities this backend provides; everything else is reported unsupported. */
    private final Set<Feature> supported = EnumSet.of(
            Feature.KEY, Feature.MOUSE_BUTTON, Feature.SCROLL, Feature.LOOK_DELTA, Feature.POINTER,
            Feature.AUTO_SPRINT, Feature.HOTBAR_SLOT);

    private boolean autoSprintActive;

    /**
     * The keys and buttons this backend has pressed and not yet released (item 217).
     *
     * <p>Needed because the backend is the only thing that knows what it injected. When the window
     * loses focus or the activity pauses, nothing will ever send the matching release, so a key held
     * at that moment stays held in the game — the classic "the player keeps walking after
     * alt-tabbing" bug. {@link #releaseAll()} walks these and sends the release for each.
     *
     * <p>Concurrent sets because injection and the focus/pause callback are not guaranteed to be the
     * same thread. These are per-event, not per-frame, so the cost is off the frame path.
     */
    private final Set<Integer> heldKeys = ConcurrentHashMap.newKeySet();
    private final Set<Integer> heldButtons = ConcurrentHashMap.newKeySet();

    public LauncherGameBackend(GameInputHost host) {
        this(host, null);
    }

    /**
     * @param host the input dispatcher; required
     * @param frameTickSource the game's frame-tick supplier, or null when the render hook is not
     *     available (in which case the {@link Feature#FPS} capability is not advertised)
     */
    public LauncherGameBackend(GameInputHost host, FrameTickSource frameTickSource) {
        if (host == null) {
            throw new IllegalArgumentException("host");
        }
        this.host = host;
        this.frameTickSource = frameTickSource;
        if (frameTickSource != null) {
            supported.add(Feature.FPS);
        }
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String version() {
        return VERSION;
    }

    @Override
    public int apiVersion() {
        return API_VERSION;
    }

    @Override
    public boolean supports(Feature feature) {
        return feature != null && supported.contains(feature);
    }

    @Override
    public boolean sendKey(int bedrockKeyCode, boolean down) {
        boolean injected = host.injectKey(bedrockKeyCode, down);
        if (down) {
            heldKeys.add(bedrockKeyCode);
        } else {
            heldKeys.remove(bedrockKeyCode);
        }
        return injected;
    }

    @Override
    public boolean sendMouseButton(int androidButton, boolean down) {
        boolean injected = host.injectMouseButton(androidButton, down);
        if (down) {
            heldButtons.add(androidButton);
        } else {
            heldButtons.remove(androidButton);
        }
        return injected;
    }

    @Override
    public boolean sendScroll(float vertical) {
        return host.injectScroll(vertical);
    }

    @Override
    public boolean sendLookDelta(float deltaX, float deltaY) {
        return host.injectLookDelta(deltaX, deltaY);
    }

    @Override
    public boolean sendPointer(float x, float y) {
        return host.injectPointer(x, y);
    }

    /**
     * The game's frame rate, derived from the preloader's render-hook frame counter.
     *
     * <p>The render hook increments one tick per frame the game actually draws, so counting ticks
     * over a wall-clock window is the game's own frame rate — not the host's vsync rate and not a
     * guess. {@link GameFrameRate} owns the windowing and returns "unknown" while the hook is not
     * drawing, so a loading screen does not freeze the last reading.
     *
     * <p>Returns {@link GameFrameRate#UNKNOWN} when no tick source was supplied (a build without
     * the hook), which the overlay treats as "fall back to the host-side meter".
     */
    @Override
    public int readFps() {
        if (frameTickSource == null) {
            return GameFrameRate.UNKNOWN;
        }
        long tick;
        try {
            tick = frameTickSource.renderTick();
        } catch (Throwable t) {
            return GameFrameRate.UNKNOWN;
        }
        return frameRate.sample(tick, clock.getAsLong());
    }

    /**
     * Replaces the clock the frame-rate window is measured against.
     *
     * <p>Tests only: on a device the default {@code SystemClock.uptimeMillis} is the correct
     * monotonic source. Kept package-visible rather than public so production code cannot swap it.
     */
    void setClock(java.util.function.LongSupplier clock) {
        if (clock != null) {
            this.clock = clock;
        }
    }

    /**
     * Turns auto-sprint on or off by double-tapping forward.
     *
     * <p>Bedrock has no dedicated sprint key: a player sprints by double-tapping forward. This
     * reproduces that, holding the second press so the sprint is sustained. It is an approximation
     * — a player who releases forward stops sprinting, exactly as they would without the module.
     */
    @Override
    public boolean setAutoSprint(boolean enabled) {
        if (enabled == autoSprintActive) {
            return true;
        }
        autoSprintActive = enabled;
        if (enabled) {
            // The double-tap goes through sendKey, not the host directly, so the held key is
            // tracked and released if the window loses focus mid-sprint (item 217).
            sendKey(BEDROCK_KEY_W, true);
            sendKey(BEDROCK_KEY_W, false);
            sendKey(BEDROCK_KEY_W, true);
        } else {
            sendKey(BEDROCK_KEY_W, false);
        }
        return true;
    }

    @Override
    public boolean isAutoSprintActive() {
        return autoSprintActive;
    }

    @Override
    public boolean selectHotbarSlot(int slot) {
        if (slot < 1 || slot > 9) {
            return false;
        }
        int bedrockKey = '0' + slot;
        boolean down = sendKey(bedrockKey, true);
        sendKey(bedrockKey, false);
        return down;
    }

    /**
     * Releases everything this backend injected and is still holding.
     *
     * <p>Called on window-focus loss and on pause. It must be idempotent and must never throw: it
     * runs from a lifecycle callback where an exception would take the game down. A release that
     * fails is dropped rather than retried, because the alternative — leaving it in the held set —
     * would keep re-releasing a key the host has already forgotten.
     */
    @Override
    public void releaseAll() {
        autoSprintActive = false;

        List<Integer> keys = new ArrayList<>(heldKeys);
        heldKeys.clear();
        for (int key : keys) {
            try {
                host.injectKey(key, false);
            } catch (Throwable ignored) {
                // Best effort: the session is losing focus, so nothing useful can be reported here.
            }
        }

        List<Integer> buttons = new ArrayList<>(heldButtons);
        heldButtons.clear();
        for (int button : buttons) {
            try {
                host.injectMouseButton(button, false);
            } catch (Throwable ignored) {
            }
        }
    }

    /** How many keys this backend is currently holding; a diagnostic for tests and the log. */
    public int heldKeyCount() {
        return heldKeys.size();
    }

    /** How many mouse buttons this backend is currently holding. */
    public int heldButtonCount() {
        return heldButtons.size();
    }
}

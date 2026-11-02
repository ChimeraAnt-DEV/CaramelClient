package org.chimeramc.client.core.backend;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Which preloader hooks the running build actually installed, as one queryable table (items 453,
 * 493).
 *
 * <p>A hook that did not resolve is not an error — the game classes export no symbols, so a hook
 * exists only when its per-build signature was found. The problem is that a feature built on a
 * missing hook looks like a feature that does nothing: the overlay draws nothing, the button does
 * nothing, and there is no way to tell "broken" from "not supported on this build". This class is
 * the one place that answers, so a script or a module can skip a feature whose hook is absent
 * instead of calling into it and getting silence.
 *
 * <p><b>Fail-closed.</b> With no {@link Source} installed, every hook reads as not live, so a
 * caller that consults this before drawing or acting does nothing rather than guessing. The
 * launcher installs the real source once the preloader library is loaded.
 *
 * <p>Android-free on purpose: the mapping and the summary are pinned by
 * {@link org.chimeramc.client.core.backend.HookAvailabilityTest} with no device.
 */
public final class HookAvailability {

    /** A preloader hook the launcher knows how to ask about. The set is closed so a feature cannot
     * spell the same hook two ways. */
    public enum Hook {
        /** The game's player-render entry, which the cosmetics renderer and the FPS tick need. */
        PLAYER_RENDER("player_render"),
        /** The engine image loader, which the cape/hat/pet pixel substitution needs. */
        MCE_IMAGE("mce_image"),
        /** The cosmetics render slot (a distinct install from the render feed). */
        COSMETICS_RENDER("cosmetics_render"),
        /** The local-player feed, which the nametag icon and distance falloff need. */
        LOCAL_PLAYER("local_player"),
        /** The texture-cache flush callback, which a live cosmetic swap needs. */
        TEXTURE_FLUSH("texture_flush");
        // Only hooks with a real "resolved" probe are listed. The pause-menu and HUD-screen queries
        // report whether a screen is *open*, not whether the hook installed, so there is nothing
        // honest to put here for them and they are deliberately absent rather than always-false.

        private final String id;

        Hook(String id) {
            this.id = id;
        }

        /** The stable id, safe to send to a script or print in a report. */
        public String id() {
            return id;
        }
    }

    /** Where the live/absent reading comes from. Replaced in tests, and in production by the
     * preloader probe. */
    public interface Source {
        boolean isLive(Hook hook);
    }

    private static final Source NO_HOOKS = hook -> false;

    private static volatile Source source = NO_HOOKS;

    private HookAvailability() {
    }

    /** Installs the probe that answers {@link #isLive}. */
    public static void install(Source next) {
        source = next == null ? NO_HOOKS : next;
    }

    /** Restores the fail-closed default. Tests only. */
    public static void resetForTests() {
        source = NO_HOOKS;
    }

    /**
     * Whether {@code hook} is live in this build.
     *
     * <p>A source that throws reads as not live: a diagnostic must never be the thing that takes a
     * draw call down.
     */
    public static boolean isLive(Hook hook) {
        if (hook == null) return false;
        try {
            return source.isLive(hook);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Whether the hook a {@link GameBackend.Feature} depends on is live.
     *
     * <p>A feature with no hook of its own (the injected-input features, which are host-side) is
     * reported as available, because this table answers only about hooks — the backend's own
     * {@link GameBackend#supports} is still the authority for those.
     */
    public static boolean featureHookIsLive(GameBackend.Feature feature) {
        Hook hook = hookFor(feature);
        return hook == null || isLive(hook);
    }

    /** The hook a feature depends on, or null when the feature does not depend on one. */
    public static Hook hookFor(GameBackend.Feature feature) {
        if (feature == null) return null;
        switch (feature) {
            case FPS:
                return Hook.PLAYER_RENDER;
            default:
                return null;
        }
    }

    /** Every hook and its state, in declaration order, for a report. */
    public static Map<Hook, Boolean> snapshot() {
        Map<Hook, Boolean> out = new LinkedHashMap<>();
        for (Hook hook : Hook.values()) {
            out.put(hook, isLive(hook));
        }
        return out;
    }

    /**
     * A one-line-per-hook report, or a single line when no hook is live.
     *
     * <p>Never empty, so a Diagnostics row always says something a bug report can quote.
     */
    public static String summary() {
        StringBuilder builder = new StringBuilder();
        int live = 0;
        for (Map.Entry<Hook, Boolean> entry : snapshot().entrySet()) {
            if (entry.getValue()) live++;
            if (builder.length() > 0) builder.append(", ");
            builder.append(entry.getKey().id()).append('=').append(entry.getValue() ? "on" : "off");
        }
        if (live == 0) {
            builder.append(" (no preloader hook is live on this build)");
        }
        return builder.toString();
    }
}

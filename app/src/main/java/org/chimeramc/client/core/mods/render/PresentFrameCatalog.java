package org.chimeramc.client.core.mods.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The catalogue of the two present-frame effect modules: their ids and every configurable option,
 * with the default value and the bounds.
 *
 * <p><b>Honesty contract (read this before adding an option).</b> Both modules act on the frame the
 * engine is about to present, on the sweetloader's present / frame-boundary hook (the
 * {@code eglSwapBuffers} detour). That is the only place a second presented frame can be emitted or
 * the previous frame blended in, and it is a hook that is not live on every build (a Vulkan-only
 * build exposes no libEGL present call). So:
 *
 * <ul>
 *   <li>Neither module claims to raise the game's frame rate. Frame generation blends the previous
 *       frame into the current one so the presented image sits between the two frames the game
 *       already drew — motion reads as smoother at the same render rate, but no extra frame is
 *       presented; motion blur blends the previous frame in, which softens the image by design.</li>
 *   <li>Both are gated on the live present hook, not on the static "needs game data" list: they are
 *       badged while the hook is absent and become usable the moment it installs.</li>
 * </ul>
 *
 * <p>Pure data and pure helpers, so the counts, the key uniqueness and the bounds are unit-tested
 * without a device — see {@code PresentFrameModulesTest}.
 */
public final class PresentFrameCatalog {

    /** One configurable option of a present-frame module. */
    public static final class Option {
        public final String key;
        public final String label;
        /** "toggle" or "slider" (an int slider with min/max). */
        public final String type;
        public final String defaultValue;
        public final int min;
        public final int max;
        public final String description;

        Option(String key, String label, String type, String defaultValue, int min, int max,
               String description) {
            this.key = key;
            this.label = label;
            this.type = type;
            this.defaultValue = defaultValue;
            this.min = min;
            this.max = max;
            this.description = description;
        }

        public boolean isToggle() {
            return "toggle".equals(type);
        }
    }

    /** One present-frame module: its identity plus its options. */
    public static final class Module {
        public final String id;
        public final String title;
        public final String description;
        public final List<Option> options;

        Module(String id, String title, String description, List<Option> options) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.options = Collections.unmodifiableList(options);
        }
    }

    public static final String FRAME_GENERATION = "frame_generation";
    public static final String MOTION_BLUR = "motion_blur";

    private static final List<Module> MODULES;

    static {
        List<Module> modules = new ArrayList<>();

        // --- Frame generation ---------------------------------------------------------------------
        List<Option> frameGen = new ArrayList<>();
        frameGen.add(toggle("fg_enabled", "Frame generation",
                "Blends the previous frame into the current one on the present boundary, so the "
                        + "presented image sits between the two frames the game drew. Motion reads as "
                        + "smoother; the game's render rate is unchanged and no extra frame is "
                        + "presented."));
        frameGen.add(slider("fg_strength", "Interpolation strength (%)", 50, 0, 100,
                "How far the blend is pushed toward the previous frame. Higher reads as smoother but "
                        + "mixes more of the old frame in, so fast motion and edges show more artefacts."));
        frameGen.add(slider("fg_max_multiplier", "Max presented frames per rendered frame", 2, 2, 3,
                "Reserved. The seam presents one frame per rendered frame — it blends, it does not "
                        + "insert an extra present — so this ceiling is not acted on yet; it is kept "
                        + "for a future true frame-insertion seam."));
        frameGen.add(toggle("fg_hold_artifacts", "Reduce edge artefacts",
                "Bias the blend to reuse less of the previous frame near edges, which trades a "
                        + "little smoothness for fewer warping artefacts."));
        modules.add(new Module(FRAME_GENERATION,
                "Frame Generation",
                "Blends the previous presented frame into the current one on the engine's present "
                        + "boundary, so motion reads as smoother at the same render rate. It does not "
                        + "make the game render faster and it does not present an extra frame.", frameGen));

        // --- Motion blur --------------------------------------------------------------------------
        List<Option> motionBlur = new ArrayList<>();
        motionBlur.add(toggle("mb_enabled", "Motion blur",
                "Blend the previous presented frame into the new one, weighted by how fast the view "
                        + "is moving. Fast movement smears along the direction of travel."));
        motionBlur.add(slider("mb_strength", "Blur strength (%)", 35, 0, 100,
                "How much of the previous frame is blended in. Higher is a heavier smear and a "
                        + "softer image."));
        motionBlur.add(slider("mb_budget", "Blend budget (%)", 60, 10, 100,
                "The most of the frame time the blend may take. A bounded budget keeps the effect "
                        + "from costing more than it returns on a weak device."));
        motionBlur.add(toggle("mb_motion_weight", "Weight by view speed",
                "Scale the blur by how fast the camera is turning, so a still view stays sharp and "
                        + "only motion smears."));
        modules.add(new Module(MOTION_BLUR,
                "Motion Blur",
                "Blends the previous presented frame into the new one, weighted by how fast the view "
                        + "is moving. It softens the image by design and does not add frames.",
                motionBlur));

        MODULES = Collections.unmodifiableList(modules);
    }

    private PresentFrameCatalog() {
    }

    /** Every present-frame module, in display order. */
    public static List<Module> modules() {
        return MODULES;
    }

    /** The module with this id, or null. */
    public static Module find(String id) {
        for (Module m : MODULES) {
            if (m.id.equals(id)) {
                return m;
            }
        }
        return null;
    }

    /** True when this id is one of the present-frame modules. */
    public static boolean isPresentFrameModule(String id) {
        return find(id) != null;
    }

    /** Every option across every module, for a key-uniqueness / count assertion. */
    public static List<Option> allOptions() {
        List<Option> out = new ArrayList<>();
        for (Module m : MODULES) {
            out.addAll(m.options);
        }
        return out;
    }

    private static Option toggle(String key, String label, String description) {
        return new Option(key, label, "toggle", "false", 0, 0, description);
    }

    private static Option slider(String key, String label, int def, int min, int max,
                                 String description) {
        return new Option(key, label, "slider", String.valueOf(def), min, max, description);
    }
}

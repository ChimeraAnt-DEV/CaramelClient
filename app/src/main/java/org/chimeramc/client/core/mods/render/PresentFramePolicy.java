package org.chimeramc.client.core.mods.render;

/**
 * The decisions the two present-frame effect modules actually make, as a pure function of their
 * options and whether the present hook is live.
 *
 * <p>Separated from the options so the effect of a switch is a testable function, not a hidden side
 * effect at a call site. Nothing here touches Minecraft, GL or the sweetloader; the native present
 * seam reads a {@link Plan} and does the draw. That keeps the honesty contract checkable off-device:
 *
 * <ul>
 *   <li>the plan carries <em>no</em> frame-rate figure — neither module measures or promises one;</li>
 *   <li>when the present hook is not live the plan is {@link Plan#INACTIVE}, so a stored switch can
 *       never drive a seam that is not there;</li>
 *   <li>the blur's budget is clamped, because the effect cannot be free and a bounded budget is what
 *       stops it costing more than it returns.</li>
 * </ul>
 */
public final class PresentFramePolicy {

    /** What the present seam should do with the frame this call, for both modules at once. */
    public static final class Plan {
        /** Whether frame generation is enabled and its hook is live. */
        public final boolean frameGenerationEnabled;
        /** 0..1 how far the blend is pushed toward the previous of the two real frames. */
        public final float interpolationStrength;
        /** 2 or 3: reserved ceiling on presented frames per rendered frame (not acted on yet). */
        public final int maxFramesPerRendered;
        /** Bias the blend to reuse less of the previous frame near edges. */
        public final boolean reduceEdgeArtifacts;

        /** Whether motion blur is enabled and its hook is live. */
        public final boolean motionBlurEnabled;
        /** 0..1: how much of the previous frame is blended in. */
        public final float blurStrength;
        /** 0..1: the most of the frame time the blend may take. */
        public final float blurBudget;
        /** Scale the blur by camera speed, so a still view stays sharp. */
        public final boolean weightByViewSpeed;

        /** The plan when neither effect can run (module off, or the present hook is absent). */
        public static final Plan INACTIVE = new Plan(false, 0f, 2, false, false, 0f, 0f, false);

        Plan(boolean frameGenerationEnabled, float interpolationStrength, int maxFramesPerRendered,
             boolean reduceEdgeArtifacts, boolean motionBlurEnabled, float blurStrength,
             float blurBudget, boolean weightByViewSpeed) {
            this.frameGenerationEnabled = frameGenerationEnabled;
            this.interpolationStrength = interpolationStrength;
            this.maxFramesPerRendered = maxFramesPerRendered;
            this.reduceEdgeArtifacts = reduceEdgeArtifacts;
            this.motionBlurEnabled = motionBlurEnabled;
            this.blurStrength = blurStrength;
            this.blurBudget = blurBudget;
            this.weightByViewSpeed = weightByViewSpeed;
        }

        /** Whether any effect in this plan will do work this frame. */
        public boolean isActive() {
            return frameGenerationEnabled || motionBlurEnabled;
        }
    }

    /** The multiplier ceiling: more presented frames is more artefacts than benefit. */
    public static final int MAX_MULTIPLIER = 3;

    private PresentFramePolicy() {
    }

    /**
     * Builds the plan.
     *
     * @param presentHookLive whether the engine's present/frame-boundary hook is installed; when it
     *     is not, both effects are off regardless of their switches
     * @param frameGenEnabled the frame-generation module switch
     * @param motionBlurEnabled the motion-blur module switch
     * @param values the option values, keyed by option key; a missing key reads its default
     */
    public static Plan plan(boolean presentHookLive, boolean frameGenEnabled,
                            boolean motionBlurEnabled, ValueSource values) {
        if (!presentHookLive) {
            return Plan.INACTIVE;
        }

        boolean frameGen = frameGenEnabled && values.bool("fg_enabled", false);
        float strength = frameGen ? clamp01(values.integer("fg_strength", 50) / 100f) : 0f;
        int multiplier = clampMultiplier(values.integer("fg_max_multiplier", 2));
        boolean reduceArtifacts = frameGen && values.bool("fg_hold_artifacts", false);

        boolean blur = motionBlurEnabled && values.bool("mb_enabled", false);
        float blurStrength = blur ? clamp01(values.integer("mb_strength", 35) / 100f) : 0f;
        float blurBudget = blur ? clamp(values.integer("mb_budget", 60) / 100f, 0.10f, 1.0f) : 0f;
        boolean weightBySpeed = blur && values.bool("mb_motion_weight", false);

        return new Plan(frameGen, strength, multiplier, reduceArtifacts,
                blur, blurStrength, blurBudget, weightBySpeed);
    }

    private static int clampMultiplier(int value) {
        if (value < 2) return 2;
        return Math.min(value, MAX_MULTIPLIER);
    }

    private static float clamp01(float value) {
        return clamp(value, 0f, 1f);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    /** Reads option values by key, so the policy is testable without a SharedPreferences. */
    public interface ValueSource {
        int integer(String key, int fallback);

        boolean bool(String key, boolean fallback);
    }
}

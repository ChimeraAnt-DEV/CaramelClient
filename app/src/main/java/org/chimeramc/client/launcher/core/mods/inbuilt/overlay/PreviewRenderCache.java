package org.chimeramc.client.core.mods.inbuilt.overlay;

/**
 * The preview's two render decisions, as pure rules.
 *
 * <p>Both decide whether work happens, so both are worth a unit test: a wrong answer is either a
 * frame that never refreshes (the scene changed but the cache says it did not) or a preview that
 * keeps rasterising an unchanged scene or keeps animating off screen.
 */
public final class PreviewRenderCache {

    private PreviewRenderCache() {
    }

    /**
     * Whether the scene must be rasterised for this draw.
     *
     * <p>True when the scene revision differs from the one the cached bitmap was rendered at. The
     * rendered revision starts at {@code -1}, which no real revision reaches, so the first draw
     * always rasterises.
     */
    public static boolean shouldRasterise(long renderedRevision, long sceneRevision) {
        return renderedRevision != sceneRevision;
    }

    /**
     * Whether the idle animation should run.
     *
     * <p>Every condition is required: motion enabled, the view visible, attached to a window, the
     * window itself visible, and something to animate (a cape or pet, or the always-animate opt-in
     * the Launch hero uses). A preview that keeps Choreographer frames while its window is hidden
     * spends a phone's battery on a picture nobody can see.
     */
    public static boolean wantsAnimation(boolean animationsEnabled, boolean viewVisible,
                                         boolean attached, boolean windowVisible,
                                         boolean alwaysAnimate, boolean hasCapeOrPet) {
        return animationsEnabled && viewVisible && attached && windowVisible
                && (alwaysAnimate || hasCapeOrPet);
    }
}

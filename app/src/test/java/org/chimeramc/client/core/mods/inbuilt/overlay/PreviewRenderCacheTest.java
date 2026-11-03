package org.chimeramc.client.core.mods.inbuilt.overlay;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The preview's render-cache and animation-gate rules.
 *
 * <p>Both are silent failures if wrong: a stale revision freezes the preview, a missing gate keeps
 * a hidden preview burning frames.
 */
public class PreviewRenderCacheTest {

    @Test
    public void theFirstDrawAlwaysRasterises() {
        // renderedRevision starts at -1, which no scene revision reaches.
        assertTrue(PreviewRenderCache.shouldRasterise(-1L, 0L));
    }

    @Test
    public void anUnchangedSceneDoesNotRasteriseAgain() {
        assertFalse(PreviewRenderCache.shouldRasterise(7L, 7L));
    }

    @Test
    public void aBumpedRevisionRasterisesAgain() {
        assertTrue(PreviewRenderCache.shouldRasterise(7L, 8L));
    }

    @Test
    public void animationRunsOnlyWithEverythingSatisfied() {
        assertTrue(PreviewRenderCache.wantsAnimation(true, true, true, true, false, true));
        assertTrue(PreviewRenderCache.wantsAnimation(true, true, true, true, true, false));
    }

    @Test
    public void animationStopsWhenTheWindowIsHidden() {
        // The view is attached and visible, but its window is not: no frames.
        assertFalse(PreviewRenderCache.wantsAnimation(true, true, true, false, true, true));
    }

    @Test
    public void animationStopsWhenTheViewIsNotAttachedOrVisible() {
        assertFalse(PreviewRenderCache.wantsAnimation(true, true, false, true, true, true));
        assertFalse(PreviewRenderCache.wantsAnimation(true, false, true, true, true, true));
    }

    @Test
    public void animationStopsWhenMotionIsDisabled() {
        assertFalse(PreviewRenderCache.wantsAnimation(false, true, true, true, true, true));
    }

    @Test
    public void animationStopsWhenThereIsNothingToAnimate() {
        // No cape, no pet and no always-animate opt-in: the editor's idle case.
        assertFalse(PreviewRenderCache.wantsAnimation(true, true, true, true, false, false));
    }
}

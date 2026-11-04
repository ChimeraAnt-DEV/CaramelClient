package org.chimeramc.client.core.cosmetics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * The cape leans backward when the player runs, and it has real thickness.
 *
 * <p>The model's convention is that -z is behind the player. The hem used to move toward +z as the
 * swing grew, which is into the player's back: the cape looked like it was blowing forward, which is
 * the "physics are backwards" report. A cape leans away from the direction of travel, so the hem
 * must move to a smaller z.
 *
 * <p>A Minecraft cape is a 16x10x1 pixel slab, not a plane, so the panel has a thickness of one
 * pixel (1/16 block); a zero-thickness plane is why it read as a thin sheet with no body.
 */
public class CapePhysicsTest {

    @Test
    public void theHemLeansBackwardNotIntoThePlayer() {
        // -z is behind, so a swing must reduce z. A point on the hem is below the hinge (negative y).
        float hemZAtRest = CapeSimulator.hemZForTest(0f);
        float hemZAtSpeed = CapeSimulator.hemZForTest(CapeSimulator.MAX_SWING_DEG);
        assertTrue("a leaning cape must move its hem behind the player (smaller z). rest="
                        + hemZAtRest + " leaning=" + hemZAtSpeed,
                hemZAtSpeed < hemZAtRest);
    }

    @Test
    public void aStillCapeHangsStraight() {
        assertEquals("with no speed the cape hangs straight, so the hem is at the hinge's z",
                0f, CapeSimulator.hemZForTest(0f), 1e-4f);
    }

    @Test
    public void theCapeIsASlabWithThickness() {
        // One pixel of thickness, from the 16x10x1 texture.
        assertEquals("a Minecraft cape is one pixel thick", 1f / 16f,
                CapeSimulator.THICKNESS_BLOCKS, 1e-6f);
        assertEquals("the cape is 10 pixels wide", 10f / 16f, CapeSimulator.WIDTH_BLOCKS, 1e-6f);
        assertEquals("the cape is 16 pixels tall", 1f, CapeSimulator.HEIGHT_BLOCKS, 1e-6f);
    }

    @Test
    public void theCapeHangsFromTheTopOfTheBack() {
        // The back spans to y = 24 in the model, which is the shoulders: the cape hangs from there.
        assertEquals(24f, PreviewCapeScene.ANCHOR_Y, 1e-4f);
        assertTrue("the cape must hang behind the body's back face",
                PreviewCapeScene.ANCHOR_Z < CosmeticLayering.BODY_BACK_Z);
    }
}

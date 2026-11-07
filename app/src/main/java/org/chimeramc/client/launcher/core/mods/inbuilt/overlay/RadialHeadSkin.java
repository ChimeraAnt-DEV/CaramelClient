package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import org.chimeramc.client.core.cosmetics.PlayerSkinProvider;
import org.chimeramc.client.core.cosmetics.PreviewCharacterScene;
import org.chimeramc.client.core.cosmetics.PreviewRaster;
import org.chimeramc.client.core.cosmetics.PreviewScene;
import org.chimeramc.client.core.cosmetics.SkinModel;

/**
 * The radial wheel's hub head: the player's skin atlas, the 8x8 head crop and the scene that draws
 * it.
 *
 * <p>Extracted from {@code RadialMenuView} so that class keeps headroom under the file-size budget,
 * and because the head is a self-contained unit: it owns the skin bitmap, the head pixel atlas, the
 * head-only {@link PreviewScene}, and the one crop rectangle all three depend on. The view keeps
 * only the drawing decision.
 *
 * <p>The scene holds <b>only</b> the head box, not a full body: the hub is a small disc, so a body
 * would be a tiny unreadable figure. Passing the {@code "head"} box id selects just the head and its
 * overlay from the same authored skin.
 *
 * <p>The package is {@code ...core.mods.inbuilt.overlay} to match the sibling sources in this
 * directory, not the {@code ...launcher.core...} path the file sits under.
 */
final class RadialHeadSkin {

    /** The 8x8 head region of a 64x64 skin atlas. */
    private static final int HEAD_ORIGIN = 8;
    private static final int HEAD_SIZE = 8;
    private static final int ATLAS_CROP = 16;

    private final Rect srcHead = new Rect();

    private Bitmap skinHead;
    private PreviewScene headScene;
    private int[] headAtlas;

    /**
     * Re-resolves the player's skin and rebuilds the head.
     *
     * <p>Fail-closed: a missing skin, a decode failure or a throwing provider clears the head rather
     * than leaving a stale one, so the caller can fall back deliberately.
     */
    void resolve(android.content.Context context) {
        try {
            PlayerSkinProvider.SkinBitmap skin = PlayerSkinProvider.resolve(context);
            if (skin != null && skin.bitmap != null) {
                srcHead.set(HEAD_ORIGIN, HEAD_ORIGIN, ATLAS_CROP, ATLAS_CROP);
                skinHead = skin.bitmap;
                if (skin.bitmap.getWidth() == SkinModel.ATLAS_SIZE
                        && skin.bitmap.getHeight() == SkinModel.ATLAS_SIZE) {
                    headAtlas = new int[SkinModel.ATLAS_SIZE * SkinModel.ATLAS_SIZE];
                    skin.bitmap.getPixels(headAtlas, 0, SkinModel.ATLAS_SIZE, 0, 0,
                            SkinModel.ATLAS_SIZE, SkinModel.ATLAS_SIZE);
                    headScene = new PreviewScene();
                    PreviewCharacterScene.addCharacter(headScene, headAtlas, SkinModel.ATLAS_SIZE,
                            0f, 0f, "head");
                }
            }
        } catch (Throwable ignored) {
            skinHead = null;
            headScene = null;
        }
    }

    /** True when a flat head bitmap is available to blit. */
    boolean hasFlatHead() {
        return skinHead != null && !skinHead.isRecycled();
    }

    /** True when the 3D head scene was built and can be rendered. */
    boolean hasScene() {
        return headScene != null && headAtlas != null;
    }

    /** Draws the flat head crop into {@code dstHead}. */
    void drawFlat(Canvas canvas, RectF dstHead, Paint paint) {
        canvas.drawBitmap(skinHead, srcHead, dstHead, paint);
    }

    /**
     * Renders the 3D head into {@code raster} through the supplied camera.
     *
     * @return the fitted scene, so the caller can draw the raster it just filled
     */
    PreviewScene.Fit render(SkinModel.Camera cam, PreviewRaster raster, int size) {
        PreviewScene.Fit fit = headScene.fit(cam, size, size, 0.06f);
        headScene.render(raster, cam, fit);
        return fit;
    }

    /** The scene, for callers that need to fit or inspect it. Null when none was built. */
    PreviewScene scene() {
        return headScene;
    }
}

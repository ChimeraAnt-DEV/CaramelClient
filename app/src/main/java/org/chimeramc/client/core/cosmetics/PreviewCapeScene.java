package org.chimeramc.client.core.cosmetics;

/**
 * Builds the cape cloth mesh into the software scene from a {@link CapeSimulator}.
 *
 * <p>The cloth is the same Verlet mesh the Canvas pass drew: a {@code COLS x ROWS} grid of particles
 * in blocks, hung from the shoulders. Each grid cell becomes a flat-shaded quad; the colour comes from
 * {@link CapePatterns} at the cell's centre (so the menu preview and the worn cape show the same
 * weave), the shade from the quad's own model-space normal (so a fold curling toward the light
 * brightens), and the depth buffer resolves it against the body per pixel — no manual occlusion test.
 *
 * <p>Pure and Android-free, so it is unit-testable.
 */
public final class PreviewCapeScene {

    /** Where the cloth hangs from, in model pixels/blocks, matching the Canvas pass. */
    public static final float ANCHOR_Y = 24f;
    public static final float ANCHOR_Z = CosmeticLayering.CAPE_Z;

    private PreviewCapeScene() {
    }

    /**
     * Adds the cloth quads to {@code scene}.
     *
     * @param scene  the scene to add to (not cleared)
     * @param cape   the equipped cape (pattern + colours)
     * @param cloth  the stepped Verlet mesh
     */
    public static void addCape(PreviewScene scene, CosmeticCatalog.Cape cape, CapeSimulator cloth) {
        addCape(scene, cape, cloth, null, 0, 0);
    }

    /**
     * Adds the cloth quads to {@code scene}, preferring the cape's authored artwork when it has any.
     *
     * <p>When {@code atlas} is a real authored 64x32 sheet, each cell's colour is sampled from its
     * visible panel ({@link CapeAtlas}) instead of the procedural weave, so the preview shows the
     * artwork the player picked rather than a template. A null or undersized atlas falls back to the
     * weave, so an id with no authored image still draws.
     *
     * @param scene  the scene to add to (not cleared)
     * @param cape   the equipped cape (pattern + colours)
     * @param cloth  the stepped Verlet mesh
     * @param atlas  the authored 64x32 ARGB sheet, or null to use the procedural weave
     * @param atlasW the atlas width in pixels
     * @param atlasH the atlas height in pixels
     */
    public static void addCape(PreviewScene scene, CosmeticCatalog.Cape cape, CapeSimulator cloth,
                               int[] atlas, int atlasW, int atlasH) {
        if (scene == null || cape == null || cloth == null) return;
        boolean authored = atlas != null && atlas.length >= atlasW * atlasH
                && atlasW > 0 && atlasH > 0;
        int cols = CapeSimulator.COLS;
        int rows = CapeSimulator.ROWS;
        if (cloth.size() < cols * rows) return;

        // Model-space corner for a particle: the simulator works in blocks; lift to the shoulders.
        float[][] p = new float[cols * rows][3];
        for (int i = 0; i < cols * rows; i++) {
            p[i][0] = cloth.x(i) / SkinModel.PIXELS_TO_BLOCKS;
            p[i][1] = ANCHOR_Y + cloth.y(i) / SkinModel.PIXELS_TO_BLOCKS;
            p[i][2] = ANCHOR_Z + cloth.z(i) / SkinModel.PIXELS_TO_BLOCKS;
        }

        for (int r = 0; r + 1 < rows; r++) {
            for (int c = 0; c + 1 < cols; c++) {
                int i00 = r * cols + c, i10 = r * cols + c + 1;
                int i01 = (r + 1) * cols + c, i11 = (r + 1) * cols + c + 1;

                // The quad's normal from its own edges, so the fold shades correctly.
                float[] e1 = sub(p[i10], p[i00]);
                float[] e2 = sub(p[i01], p[i00]);
                float[] n = cross(e1, e2);
                float len = (float) Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
                if (len > 1e-6f) { n[0] /= len; n[1] /= len; n[2] /= len; }

                float u = (c + 0.5f) / Math.max(1f, cols - 1f);
                float wv = (r + 0.5f) / Math.max(1f, rows - 1f);
                int clothColor;
                if (authored) {
                    // The authored art already carries the trim and pattern; sample it directly.
                    clothColor = CapeAtlas.sample(atlas, atlasW, atlasH, u, wv);
                    if ((clothColor >>> 24) == 0) {
                        // A transparent pixel on the panel falls back to the weave, so a partly
                        // authored sheet never shows a hole.
                        clothColor = weaveColor(cape, u, wv);
                    }
                } else {
                    clothColor = weaveColor(cape, u, wv);
                }
                int shaded = PreviewLighting.shadeColorForNormal(clothColor, n[0], n[1], n[2]);

                float[][] corners = {p[i00], p[i10], p[i01], p[i11]};
                PreviewScene.Face f = scene.addFlatQuad(corners, shaded);
                if (f == null) continue;
                // A cloth sheet is visible from both sides; the shading already accounts for the
                // fold, so no face is culled.
                f.doubleSided = true;
                f.nx = n[0]; f.ny = n[1]; f.nz = n[2];
            }
        }
    }

    /** The procedural weave colour at a normalised cloth point, including the Optifine monogram. */
    private static int weaveColor(CosmeticCatalog.Cape cape, float u, float v) {
        int clothColor = CapePatterns.colorAt(cape.pattern, u, v, cape.color, cape.accentColor);
        if (cape.pattern == CosmeticCatalog.CapePattern.OPTIFINE
                && CapePatterns.optifineMonogramAt(u, v)) {
            clothColor = cape.accentColor;
        }
        return clothColor;
    }

    private static float[] sub(float[] a, float[] b) {
        return new float[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static float[] cross(float[] a, float[] b) {
        return new float[]{
                a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2],
                a[0] * b[1] - a[1] * b[0]};
    }
}

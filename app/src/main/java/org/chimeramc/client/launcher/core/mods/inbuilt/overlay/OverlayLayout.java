package org.chimeramc.client.core.mods.inbuilt.overlay;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A named set of overlay positions, so a player can keep a PvP arrangement and a survival one and
 * switch between them.
 *
 * <p>Only the positions are stored, not the module set: a layout says "the armour HUD sits here,
 * the CPS readout sits there", and which modules are on is the module list's job. Mixing the two
 * would make switching a layout silently change what is enabled, which is not what "where my HUD
 * sits" means.
 *
 * <p>Positions are surface fractions, so a layout saved in portrait-ish window sizes or
 * after a resolution-scale change still places every widget correctly.
 *
 * <p>A plain POJO so Gson round-trips it and the rules around it stay unit-testable.
 */
public final class OverlayLayout {

    /** The most widgets one layout may position; a wider set is a sign of a corrupt blob. */
    public static final int MAX_ENTRIES = 64;
    /** The most layouts a player may keep, so a corrupt blob cannot grow without bound. */
    public static final int MAX_LAYOUTS = 16;
    /** The longest a layout name may be, so it fits the wheel's sector label. */
    public static final int MAX_NAME_LENGTH = 24;

    /** The stable id a wheel entry refers to. */
    public String id = "";
    /** The display name (shown in the manager and on the wheel). */
    public String name = "";
    /** Widget key (a {@code ModIds} value or an overlay key) to its {@code {xFraction, yFraction}}. */
    public Map<String, float[]> positions = new LinkedHashMap<>();

    public OverlayLayout() {
    }

    public OverlayLayout(String id, String name) {
        this.id = id == null ? "" : id;
        this.name = name == null ? "" : name;
    }

    /** The stored fraction pair for a key, or null when the layout does not position it. */
    public float[] positionOf(String key) {
        if (key == null || positions == null) return null;
        float[] value = positions.get(key);
        if (value == null || value.length < 2) return null;
        return value;
    }

    /** Records a key's position, replacing any previous value. */
    public void put(String key, float xFraction, float yFraction) {
        if (key == null || key.isEmpty()) return;
        if (positions == null) positions = new LinkedHashMap<>();
        if (positions.size() >= MAX_ENTRIES && !positions.containsKey(key)) return;
        positions.put(key, new float[]{clamp01(xFraction), clamp01(yFraction)});
    }

    public int size() {
        return positions == null ? 0 : positions.size();
    }

    /** A layout with no positions cannot restore anything, so it is treated as empty. */
    public boolean isEmpty() {
        return size() == 0;
    }

    private static float clamp01(float value) {
        if (Float.isNaN(value)) return 0f;
        return Math.max(0f, Math.min(1f, value));
    }
}

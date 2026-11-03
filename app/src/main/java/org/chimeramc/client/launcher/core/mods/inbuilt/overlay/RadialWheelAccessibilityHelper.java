package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.customview.widget.ExploreByTouchHelper;

import java.util.List;

/**
 * The radial wheel's screen-reader surface.
 *
 * <p>The wheel is a single custom {@link View} that paints its slots itself, so to the accessibility
 * framework it is one opaque rectangle: a screen reader can focus the whole wheel but cannot reach
 * any individual slot. This bridge publishes each slot as a virtual child node with a label, a
 * bounds rectangle and a click action, which is the only way a touch-exploring user can drive a
 * canvas-drawn control.
 *
 * <p>Every decision the framework consumes is derived from the view's own geometry
 * ({@link RadialMenuView#a11ySlotCenter}) and state ({@link RadialMenuView#a11ySlotSelected}), and
 * the activation path ({@link RadialMenuView#a11yActivateSlot}) is the same one a tap takes, so a
 * screen-reader selection cannot behave differently from a touch. The helper only translates.
 *
 * <p>Kept in its own class rather than inline in the 1,700-line view so the slot model is
 * reviewable on its own; it holds no state beyond the ids the framework hands it.
 */
final class RadialWheelAccessibilityHelper extends ExploreByTouchHelper {

    /** The touch-target size of a virtual slot node, in dp. */
    private static final int NODE_SIZE_DP = 48;
    /** The first virtual id handed out to a chip; ring slots take 0..count-1. */
    private static final int CHIP_ID_BASE = 1000;

    private final RadialMenuView view;

    RadialWheelAccessibilityHelper(RadialMenuView view) {
        super(view);
        this.view = view;
        // The bridge is what makes the view focusable to the framework; it also routes hover and
        // key events through the virtual tree. The window carries FLAG_NOT_FOCUSABLE, so the view
        // itself never holds input focus, but accessibility touch exploration still reaches it via
        // the node provider — which is why the view must be marked important for accessibility.
        view.setFocusable(true);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    private int density() {
        return Math.max(1, Math.round(view.getResources().getDisplayMetrics().density));
    }

    /** The virtual id for a chip node. */
    private int chipVirtualId(int index) {
        return CHIP_ID_BASE + index;
    }

    @Override
    protected int getVirtualViewAt(float x, float y) {
        int slot = view.a11ySlotCount();
        for (int i = 0; i < slot; i++) {
            float[] c = view.a11ySlotCenter(i);
            if (c == null) continue;
            if (Math.hypot(x - c[0], y - c[1]) <= NODE_SIZE_DP * density() / 2f) {
                return i;
            }
        }
        int chip = view.a11yChipIndexAt(x, y);
        if (chip >= 0) return chipVirtualId(chip);
        return HOST_ID;
    }

    @Override
    protected void getVisibleVirtualViews(List<Integer> virtualViewIds) {
        int slot = view.a11ySlotCount();
        for (int i = 0; i < slot; i++) {
            virtualViewIds.add(i);
        }
        List<RadialMenuView.Chip> chips = view.a11yChips();
        for (int i = 0; i < chips.size(); i++) {
            virtualViewIds.add(chipVirtualId(i));
        }
    }

    @Override
    protected void onPopulateNodeForVirtualView(int virtualViewId, AccessibilityNodeInfoCompat node) {
        if (virtualViewId >= CHIP_ID_BASE) {
            populateChipNode(virtualViewId - CHIP_ID_BASE, node);
            return;
        }
        String label = view.a11ySlotLabel(virtualViewId);
        if (label == null) {
            // The framework requires a node to have text and bounds; a slot that no longer resolves
            // (a profile changed under the open wheel) is reported as an empty, non-focusable node
            // rather than throwing.
            node.setContentDescription("");
            node.setBoundsInParent(new Rect());
            return;
        }
        float[] c = view.a11ySlotCenter(virtualViewId);
        int half = Math.round(NODE_SIZE_DP * density() / 2f);
        Rect bounds = new Rect();
        if (c != null) {
            bounds.set(Math.round(c[0]) - half, Math.round(c[1]) - half,
                    Math.round(c[0]) + half, Math.round(c[1]) + half);
        }
        node.setContentDescription(label);
        node.setBoundsInParent(bounds);
        node.setSelected(view.a11ySlotSelected(virtualViewId));
        node.setClickable(true);
        node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
    }

    private void populateChipNode(int index, AccessibilityNodeInfoCompat node) {
        List<RadialMenuView.Chip> chips = view.a11yChips();
        if (index < 0 || index >= chips.size()) {
            node.setContentDescription("");
            node.setBoundsInParent(new Rect());
            return;
        }
        RadialMenuView.Chip chip = chips.get(index);
        // A chip is a toggle, so its state is part of the label: a screen reader announces "Zoom,
        // on" rather than just "Zoom", which is the difference between a usable toggle and a guess.
        String state = view.a11yChipOn(chip) ? "on" : "off";
        node.setContentDescription(chip.label + ", " + state);
        node.setClassName("android.widget.Switch");
        node.setCheckable(true);
        node.setChecked(view.a11yChipOn(chip));
        node.setBoundsInParent(view.a11yChipBounds(index));
        node.setClickable(true);
        node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK);
    }

    @Override
    protected boolean onPerformActionForVirtualView(int virtualViewId, int action, Bundle arguments) {
        if (action != AccessibilityNodeInfoCompat.ACTION_CLICK) return false;
        if (virtualViewId >= CHIP_ID_BASE) {
            List<RadialMenuView.Chip> chips = view.a11yChips();
            int index = virtualViewId - CHIP_ID_BASE;
            if (index < 0 || index >= chips.size()) return false;
            view.a11yActivateChip(chips.get(index));
            return true;
        }
        view.a11yActivateSlot(virtualViewId);
        return true;
    }
}

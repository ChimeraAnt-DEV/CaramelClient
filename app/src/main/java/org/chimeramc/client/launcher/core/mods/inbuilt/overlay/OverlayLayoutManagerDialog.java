package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.chimeramc.client.R;
import org.chimeramc.client.core.replay.ReplayStyle;
import org.chimeramc.client.ui.animation.DynamicAnim;

import java.util.List;
import java.util.function.Consumer;

/**
 * The overlay layout manager (item 212).
 *
 * <p>A layout is a named set of overlay positions, so a player can keep a PvP arrangement and a
 * survival one. {@link OverlayLayoutStore} owns the storage and {@link InbuiltOverlayManager} owns
 * the capture and the apply; this dialog is the screen that drives both, and it writes straight
 * through so the manager and the wheel cannot disagree about what exists.
 *
 * <p>Saving captures the positions on screen right now, which is what a player expects from "save
 * this layout": arrange the HUD, then name it. A layout is applied either here or from a wheel
 * entry whose {@code SWITCH_LAYOUT} target names it.
 */
final class OverlayLayoutManagerDialog {

    private OverlayLayoutManagerDialog() {
    }

    static void show(Activity activity, ModMenuTheme theme) {
        if (activity == null) return;
        final OverlayLayoutStore store = new OverlayLayoutStore(activity);
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(activity, 16), dp(activity, 14), dp(activity, 16), dp(activity, 12));
        root.setBackground(ReplayStyle.roundedStroked(theme.panelSurface(),
                ModMenuPalette.BORDER_HAIRLINE, 18f, activity));

        TextView title = new TextView(activity);
        title.setText(R.string.overlay_layout_title);
        title.setTextColor(theme.textPrimary());
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);

        ScrollView scroll = new ScrollView(activity);
        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 300));
        scrollParams.topMargin = dp(activity, 10);
        root.addView(scroll, scrollParams);

        final Runnable rebuild = new Runnable() {
            @Override
            public void run() {
                list.removeAllViews();
                List<OverlayLayout> layouts = store.load();
                String activeId = store.getActiveId();
                if (layouts.isEmpty()) {
                    TextView empty = new TextView(activity);
                    empty.setText(R.string.overlay_layout_empty);
                    empty.setTextColor(theme.textTertiary());
                    empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
                    list.addView(empty);
                    return;
                }
                for (OverlayLayout layout : layouts) {
                    if (layout == null) continue;
                    addLayoutRow(activity, theme, list, store, this, layout,
                            layout.id.equals(activeId));
                }
            }
        };
        rebuild.run();

        LinearLayout buttons = new LinearLayout(activity);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.END);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.topMargin = dp(activity, 12);

        TextView save = sheetButton(activity, theme, R.string.overlay_layout_save_current, false);
        save.setOnClickListener(v -> promptForName(activity, theme, R.string.overlay_layout_save_title,
                "", name -> {
                    InbuiltOverlayManager manager = InbuiltOverlayManager.getInstance();
                    if (manager == null) return;
                    OverlayLayout captured = manager.captureOverlayLayout(name);
                    store.save(OverlayLayoutStore.upsert(store.load(), captured));
                    store.setActiveId(captured.id);
                    rebuild.run();
                }));
        buttons.addView(save);

        TextView done = sheetButton(activity, theme, R.string.wheel_manager_done, true);
        done.setOnClickListener(v -> dialog.dismiss());
        buttons.addView(done);
        root.addView(buttons, buttonParams);

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ReplayStyle.dpInt(activity, 360f),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
            window.setDimAmount(0.6f);
        }
        DynamicAnim.animateDialogShow(root);
        dialog.show();
    }

    private static void addLayoutRow(Activity activity, ModMenuTheme theme, LinearLayout parent,
                                     OverlayLayoutStore store, Runnable rebuild,
                                     OverlayLayout layout, boolean active) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(activity, 10), dp(activity, 8), dp(activity, 6), dp(activity, 8));
        row.setBackground(ReplayStyle.roundedStroked(
                active ? theme.accentFill(30) : theme.disabledCardColor(),
                ModMenuPalette.BORDER_HAIRLINE, 12f, activity));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowParams.bottomMargin = dp(activity, 6);

        LinearLayout text = new LinearLayout(activity);
        text.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        TextView name = new TextView(activity);
        name.setText(layout.name);
        name.setTextColor(theme.textPrimary());
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        text.addView(name);
        TextView count = new TextView(activity);
        count.setText(activity.getString(R.string.overlay_layout_entry_count, layout.size()));
        count.setTextColor(active ? theme.accent() : theme.textTertiary());
        count.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        text.addView(count);
        row.addView(text, textParams);

        TextView apply = sheetButton(activity, theme, R.string.overlay_layout_apply, false);
        apply.setOnClickListener(v -> {
            InbuiltOverlayManager manager = InbuiltOverlayManager.getInstance();
            if (manager != null) manager.switchOverlayLayout(layout.id);
            rebuild.run();
        });
        row.addView(apply);

        TextView rename = sheetButton(activity, theme, R.string.overlay_layout_rename, false);
        rename.setOnClickListener(v -> promptForName(activity, theme,
                R.string.overlay_layout_rename_title, layout.name, newName -> {
                    OverlayLayout renamed = OverlayLayoutStore.findById(store.load(), layout.id);
                    if (renamed == null) return;
                    renamed.name = newName;
                    store.save(OverlayLayoutStore.upsert(store.load(), renamed));
                    rebuild.run();
                }));
        row.addView(rename);

        TextView delete = sheetButton(activity, theme, R.string.overlay_layout_delete, false);
        delete.setOnClickListener(v -> {
            List<OverlayLayout> remaining = new java.util.ArrayList<>();
            for (OverlayLayout other : store.load()) {
                if (other != null && !other.id.equals(layout.id)) remaining.add(other);
            }
            store.save(remaining);
            if (layout.id.equals(store.getActiveId())) store.setActiveId("");
            rebuild.run();
        });
        row.addView(delete);

        parent.addView(row, rowParams);
    }

    private static void promptForName(Activity activity, ModMenuTheme theme, int titleRes,
                                      String initial, Consumer<String> onName) {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(activity, 16), dp(activity, 14), dp(activity, 16), dp(activity, 12));
        root.setBackground(ReplayStyle.roundedStroked(theme.panelSurface(),
                ModMenuPalette.BORDER_HAIRLINE, 16f, activity));

        TextView title = new TextView(activity);
        title.setText(titleRes);
        title.setTextColor(theme.textPrimary());
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);

        EditText input = new EditText(activity);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(initial == null ? "" : initial);
        input.setHint(R.string.wheel_manager_name_hint);
        input.setTextColor(theme.textPrimary());
        input.setHintTextColor(theme.textTertiary());
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inputParams.topMargin = dp(activity, 10);
        root.addView(input, inputParams);

        LinearLayout buttons = new LinearLayout(activity);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.END);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.topMargin = dp(activity, 12);

        TextView cancel = sheetButton(activity, theme, R.string.wheel_manager_cancel, false);
        cancel.setOnClickListener(v -> dialog.dismiss());
        buttons.addView(cancel);

        TextView save = sheetButton(activity, theme, R.string.wheel_manager_save, true);
        save.setOnClickListener(v -> {
            String name = input.getText() == null ? "" : input.getText().toString().trim();
            if (name.isEmpty()) return;
            dialog.dismiss();
            onName.accept(name);
        });
        buttons.addView(save);
        root.addView(buttons, buttonParams);

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ReplayStyle.dpInt(activity, 300f),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
            window.setDimAmount(0.6f);
        }
        dialog.show();
    }

    private static TextView sheetButton(Activity activity, ModMenuTheme theme, int labelRes,
                                        boolean primary) {
        TextView button = new TextView(activity);
        button.setText(labelRes);
        button.setTextColor(primary ? theme.panelSurface() : theme.textPrimary());
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        button.setTypeface(null, Typeface.BOLD);
        button.setBackground(ReplayStyle.rippled(
                ReplayStyle.pill(primary ? theme.accent() : theme.disabledCardColor(), activity),
                theme.accentFill(60), activity));
        button.setPadding(dp(activity, 10), dp(activity, 6), dp(activity, 10), dp(activity, 6));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMarginStart(dp(activity, 6));
        button.setLayoutParams(params);
        DynamicAnim.applyPressScale(button);
        return button;
    }

    private static int dp(Activity activity, float value) {
        return ReplayStyle.dpInt(activity, value);
    }
}

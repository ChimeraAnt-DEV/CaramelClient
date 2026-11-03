package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.chimeramc.client.R;
import org.chimeramc.client.ui.animation.DynamicAnim;
import org.chimeramc.client.ui.components.UiTokens;

import java.util.List;

/**
 * The in-game quick settings panel.
 *
 * <p>Two settings a player wants mid-session — the render resolution scale and the frame-rate cap —
 * with each choice applied the moment it is tapped and the panel stating whether that took effect
 * now or on the next world load. It is opened from the pause-menu row.
 *
 * <p>It is a plain {@link Dialog} like the wheel's closet chooser, so it inherits back-dismiss and
 * the dim behind it without manual window bookkeeping. The choices and the clamping rules come from
 * {@link QuickSettings}, so the chips, the writer and the tests cannot disagree.
 */
public class QuickSettingsPanel extends Dialog {

    private final Activity activity;
    private int resolutionScale;
    private int refreshCap;

    /** The chips for one row, kept so the selected state can be repainted in place. */
    private LinearLayout resolutionRow;
    private LinearLayout refreshRow;
    private TextView status;

    public QuickSettingsPanel(Activity activity) {
        super(activity);
        this.activity = activity;
        this.resolutionScale = QuickSettings.DEFAULT_RESOLUTION_SCALE;
        this.refreshCap = QuickSettings.CAP_UNLIMITED;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(buildContent());
        setCanceledOnTouchOutside(true);

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
            WindowManager.LayoutParams attrs = window.getAttributes();
            attrs.dimAmount = 0.45f;
            window.setAttributes(attrs);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
    }

    private View buildContent() {
        int accent = UiTokens.resolveAccent(activity);
        int pad = dp(UiTokens.SPACE_4);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setMinimumWidth(dp(280));
        GradientDrawable panel = new GradientDrawable();
        panel.setShape(GradientDrawable.RECTANGLE);
        panel.setCornerRadius(dp(UiTokens.RADIUS_XLARGE));
        panel.setColor(UiTokens.BG_SURFACE);
        panel.setStroke(Math.max(1, dp(1)), UiTokens.STROKE_HAIRLINE);
        root.setBackground(panel);

        TextView title = new TextView(activity);
        title.setText(R.string.quick_settings_title);
        title.setTextColor(UiTokens.TEXT_PRIMARY);
        title.setTextSize(15f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(title);

        root.addView(sectionLabel(R.string.quick_settings_resolution));
        resolutionRow = choiceRow();
        root.addView(resolutionRow);
        root.addView(hint(R.string.quick_settings_resolution_next_load));
        fillResolutionRow(accent);

        root.addView(sectionLabel(R.string.quick_settings_refresh_cap));
        refreshRow = choiceRow();
        root.addView(refreshRow);
        root.addView(hint(R.string.quick_settings_refresh_immediate));
        fillRefreshRow(accent);

        status = new TextView(activity);
        status.setTextColor(UiTokens.TEXT_SECONDARY);
        status.setTextSize(11f);
        status.setGravity(Gravity.CENTER_HORIZONTAL);
        status.setPadding(0, dp(UiTokens.SPACE_2), 0, 0);
        root.addView(status);
        return root;
    }

    private TextView sectionLabel(int textRes) {
        TextView label = new TextView(activity);
        label.setText(textRes);
        label.setTextColor(UiTokens.TEXT_PRIMARY);
        label.setTextSize(12f);
        label.setPadding(0, dp(UiTokens.SPACE_3), 0, dp(UiTokens.SPACE_2));
        return label;
    }

    private TextView hint(int textRes) {
        TextView hint = new TextView(activity);
        hint.setText(textRes);
        hint.setTextColor(UiTokens.TEXT_SECONDARY);
        hint.setTextSize(10f);
        hint.setPadding(0, dp(UiTokens.SPACE_1), 0, 0);
        return hint;
    }

    private LinearLayout choiceRow() {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }

    private void fillResolutionRow(int accent) {
        if (resolutionRow == null) return;
        resolutionRow.removeAllViews();
        List<QuickSettings.Choice> choices = QuickSettings.resolutionChoices(
                new int[]{R.string.quick_settings_scale_50, R.string.quick_settings_scale_75,
                        R.string.quick_settings_scale_100});
        for (QuickSettings.Choice choice : choices) {
            resolutionRow.addView(chip(choice.labelRes, choice.value == resolutionScale, accent,
                    () -> {
                        resolutionScale = choice.value;
                        applyResolution(accent);
                    }));
        }
    }

    private void fillRefreshRow(int accent) {
        if (refreshRow == null) return;
        refreshRow.removeAllViews();
        List<QuickSettings.Choice> choices = QuickSettings.refreshChoices(
                new int[]{R.string.quick_settings_cap_30, R.string.quick_settings_cap_60,
                        R.string.quick_settings_cap_90, R.string.quick_settings_cap_120,
                        R.string.quick_settings_cap_unlimited});
        for (QuickSettings.Choice choice : choices) {
            refreshRow.addView(chip(choice.labelRes, choice.value == refreshCap, accent, () -> {
                refreshCap = choice.value;
                applyRefresh(accent);
            }));
        }
    }

    private View chip(int labelRes, boolean selected, int accent, Runnable onTap) {
        TextView chip = new TextView(activity);
        chip.setText(labelRes);
        chip.setTextSize(11f);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(UiTokens.SPACE_3), dp(UiTokens.SPACE_2),
                dp(UiTokens.SPACE_3), dp(UiTokens.SPACE_2));
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(UiTokens.RADIUS_CHIP));
        bg.setColor(selected ? UiTokens.BG_SURFACE_HOVER : UiTokens.BG_SURFACE);
        bg.setStroke(Math.max(1, dp(1)), selected ? accent : UiTokens.STROKE_HAIRLINE);
        chip.setBackground(bg);
        chip.setTextColor(selected ? accent : UiTokens.TEXT_SECONDARY);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMarginEnd(dp(UiTokens.SPACE_2));
        chip.setLayoutParams(params);
        chip.setOnClickListener(v -> onTap.run());
        DynamicAnim.applyPressScale(chip);
        return chip;
    }

    private void applyResolution(int accent) {
        QuickSettingsApplier.Result result =
                QuickSettingsApplier.applyResolutionScale(activity, resolutionScale);
        fillResolutionRow(accent);
        showStatus(result);
    }

    private void applyRefresh(int accent) {
        QuickSettingsApplier.Result result =
                QuickSettingsApplier.applyRefreshCap(activity, refreshCap);
        fillRefreshRow(accent);
        showStatus(result);
    }

    private void showStatus(QuickSettingsApplier.Result result) {
        if (status == null) return;
        switch (result) {
            case NEXT_LOAD:
                status.setText(R.string.quick_settings_resolution_next_load);
                break;
            case APPLIED:
                status.setText(R.string.quick_settings_applied);
                break;
            case NO_INSTANCE:
                status.setText(R.string.quick_settings_no_instance);
                break;
            case UNSUPPORTED:
                status.setText(R.string.quick_settings_cap_unsupported);
                break;
            case FAILED:
            default:
                status.setText(R.string.quick_settings_write_failed);
                break;
        }
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}

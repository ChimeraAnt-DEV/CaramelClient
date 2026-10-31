package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.chimeramc.client.R;
import org.chimeramc.client.databinding.OverlayPauseMenuRowBinding;
import org.chimeramc.client.core.mods.inbuilt.manager.InbuiltModManager;
import org.chimeramc.client.launcher.core.content.ScreenshotAction;
import org.chimeramc.client.settings.FeatureSettings;
import org.chimeramc.client.util.MainThread;

import java.util.List;

/**
 * The in-game pause-menu button row (item 224).
 *
 * <p>A compact glass strip pinned to the bottom of the screen offering the four actions a player
 * reaches for while paused: take a screenshot, hide the HUD, switch to the next overlay layout and
 * open the log viewer. It appears only while the game's pause menu is open and the Mod Menu is
 * enabled, so it never competes with gameplay.
 *
 * <p>The entries come from {@link PauseMenuActions}, and each is wired through the same code the
 * Mod Menu's own actions use, so a screenshot taken from the row and one taken from the catalogue
 * module are the same operation.
 */
public class PauseMenuRow {

    private static final String TAG = "PauseMenuRow";

    private final Activity activity;
    private final WindowManager windowManager;
    private final Handler handler = MainThread.handler();

    private View rowView;
    private WindowManager.LayoutParams wmParams;
    private boolean isShowing = false;
    private boolean isAttached = false;

    public PauseMenuRow(Activity activity) {
        this.activity = activity;
        this.windowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
    }

    /** Attaches the row; it stays hidden until {@link #setPauseMenuOpen(boolean)} says otherwise. */
    public void show() {
        if (isShowing || activity.isFinishing() || activity.isDestroyed()) return;
        handler.post(this::attach);
    }

    private void attach() {
        if (isAttached || activity.isFinishing() || activity.isDestroyed()) return;
        try {
            OverlayPauseMenuRowBinding binding =
                    OverlayPauseMenuRowBinding.inflate(LayoutInflater.from(activity));
            rowView = binding.getRoot();
            buildButtons(binding.pauseMenuRowButtons);

            int width = WindowManager.LayoutParams.WRAP_CONTENT;
            wmParams = new WindowManager.LayoutParams(
                    width,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
                    PixelFormat.TRANSLUCENT);
            wmParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            wmParams.y = (int) (24 * activity.getResources().getDisplayMetrics().density);
            wmParams.token = activity.getWindow().getDecorView().getWindowToken();

            rowView.setVisibility(View.GONE);
            windowManager.addView(rowView, wmParams);
            isAttached = true;
            isShowing = true;
        } catch (Exception e) {
            attachToContentRoot();
        }
    }

    /**
     * Fallback attach into the activity's own content view.
     *
     * <p>{@code addView} can be refused (a missing window token during a transition), and a refused
     * add would leave the row with no way to appear at all, so the row falls back to the content
     * root the same way the Mod Menu button does.
     */
    private void attachToContentRoot() {
        if (isAttached) return;
        ViewGroup rootView = activity.findViewById(android.R.id.content);
        if (rootView == null) return;
        OverlayPauseMenuRowBinding binding =
                OverlayPauseMenuRowBinding.inflate(LayoutInflater.from(activity));
        rowView = binding.getRoot();
        buildButtons(binding.pauseMenuRowButtons);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.bottomMargin = (int) (24 * activity.getResources().getDisplayMetrics().density);
        rowView.setVisibility(View.GONE);
        rootView.addView(rowView, params);
        wmParams = null;
        isAttached = true;
        isShowing = true;
    }

    private void buildButtons(LinearLayout container) {
        if (container == null) return;
        List<PauseMenuActions.Entry> entries = PauseMenuActions.all();
        float density = activity.getResources().getDisplayMetrics().density;
        int size = (int) (44 * density);
        for (PauseMenuActions.Entry entry : entries) {
            ImageButton button = new ImageButton(activity);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMarginStart((int) (4 * density));
            params.setMarginEnd((int) (4 * density));
            button.setLayoutParams(params);
            button.setBackgroundResource(R.drawable.bg_vip_icon_button);
            button.setImageResource(entry.getIconRes());
            button.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
            button.setContentDescription(activity.getString(entry.getLabelRes()));
            button.setTooltipText(activity.getString(entry.getLabelRes()));
            int padding = (int) (10 * density);
            button.setPadding(padding, padding, padding, padding);
            button.setOnClickListener(v -> runAction(entry.getId()));
            container.addView(button);
        }
    }

    /** Runs one row entry. */
    private void runAction(String actionId) {
        if (actionId == null) return;
        switch (actionId) {
            case PauseMenuActions.ACTION_SCREENSHOT:
                ScreenshotAction.capture(activity);
                break;
            case PauseMenuActions.ACTION_HIDE_HUD:
                toggleHudVisibility();
                break;
            case PauseMenuActions.ACTION_SWITCH_LAYOUT:
                cycleOverlayLayout();
                break;
            case PauseMenuActions.ACTION_OPEN_LOGS:
                toggleLogcat();
                break;
            case PauseMenuActions.ACTION_QUICK_SETTINGS:
                new QuickSettingsPanel(activity).show();
                break;
            default:
                break;
        }
    }

    private void toggleHudVisibility() {
        InbuiltOverlayManager manager = InbuiltOverlayManager.getInstance();
        if (manager == null) return;
        boolean hidden = !manager.isOverlaysHiddenForScreenshot();
        manager.setOverlaysHiddenForScreenshot(hidden);
        Toast.makeText(activity, hidden ? R.string.mod_menu_hud_hidden : R.string.mod_menu_hud_shown,
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Switches to the next saved overlay layout.
     *
     * <p>The list is cycled rather than named because the row is a quick action: a player who wants
     * to pick a specific layout uses the wheel's layout entries or the manager dialog. When no
     * layout exists the row says so instead of doing nothing.
     */
    private void cycleOverlayLayout() {
        InbuiltOverlayManager manager = InbuiltOverlayManager.getInstance();
        if (manager == null) return;
        OverlayLayoutStore store = new OverlayLayoutStore(activity);
        List<OverlayLayout> layouts = store.load();
        if (layouts.isEmpty()) {
            Toast.makeText(activity, R.string.pause_menu_no_layouts, Toast.LENGTH_SHORT).show();
            return;
        }
        String activeId = store.getActiveId();
        int index = -1;
        for (int i = 0; i < layouts.size(); i++) {
            if (layouts.get(i).id.equals(activeId)) {
                index = i;
                break;
            }
        }
        OverlayLayout next = layouts.get((index + 1) % layouts.size());
        String name = manager.switchOverlayLayout(next.id);
        if (name != null) {
            Toast.makeText(activity, activity.getString(R.string.pause_menu_layout_switched, name),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleLogcat() {
        FeatureSettings settings = FeatureSettings.getInstance();
        if (settings == null) return;
        boolean enabled = !settings.isLogcatOverlayEnabled();
        settings.setLogcatOverlayEnabled(enabled);
        org.chimeramc.client.ui.dialogs.LogcatOverlayManager manager =
                org.chimeramc.client.ui.dialogs.LogcatOverlayManager.getInstance();
        if (manager != null) manager.refreshVisibility();
    }

    /**
     * Shows or hides the row from the game's own pause state.
     *
     * <p>Called from the overlay manager's frame tick with the two native flags it has already read,
     * so the row and the rest of the overlay visibility cannot disagree about whether the pause
     * menu is open. The row is only ever shown while the pause menu is genuinely open, so it cannot
     * float over gameplay.
     */
    public void setPauseMenuOpen(boolean pauseMenuOpen, boolean showingMenu) {
        if (rowView == null) return;
        boolean visible = pauseMenuOpen && showingMenu
                && InbuiltModManager.getInstance(activity).isModMenuEnabled();
        int state = visible ? View.VISIBLE : View.GONE;
        activity.runOnUiThread(() -> {
            if (rowView != null && rowView.getVisibility() != state) {
                rowView.setVisibility(state);
            }
        });
    }

    /** True when the row has been attached to the window or the content root. */
    public boolean isAttached() {
        return isAttached;
    }

    public void hide() {
        handler.removeCallbacksAndMessages(null);
        if (!isAttached || rowView == null) {
            isAttached = false;
            isShowing = false;
            return;
        }
        try {
            if (wmParams != null) {
                windowManager.removeView(rowView);
            } else {
                ViewGroup rootView = activity.findViewById(android.R.id.content);
                if (rootView != null) rootView.removeView(rowView);
            }
        } catch (Exception ignored) {
            // A view already detached by the framework is not an error.
        }
        rowView = null;
        isAttached = false;
        isShowing = false;
    }
}

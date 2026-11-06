package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import org.chimeramc.client.R;

/**
 * The Mod Menu's HUD editor: the drag handle, the safe-area guide, the button size/opacity controls
 * and the mode switch.
 *
 * <p>Extracted from {@code ModMenuOverlay} to give that class real headroom under the file-size
 * budget. The editor is a coherent unit -- it owns the HUD tool views, the two "we are updating the
 * slider programmatically" guards, and the window-parameter dance that shrinks the overlay to a
 * floating tool window and restores it -- so it belongs in one place rather than inline in the menu.
 *
 * <p>The controller does not own the window. It reads the current {@link WindowManager} and its
 * {@link WindowManager.LayoutParams} from the {@link Host} on every show, because the menu
 * reassigns those each time it opens; holding a stale copy would move a window that no longer
 * exists.
 *
 * <p>The package is {@code ...core.mods.inbuilt.overlay} to match the sibling sources in this
 * directory, not the {@code ...launcher.core...} path the file sits under.
 */
final class HudEditorController {

    /** The menu-side effects the editor cannot perform itself. */
    interface Host {
        /** Switch the navigation highlight to the HUD section. */
        void onEditorNavigation();

        /** Return the menu to the module list. */
        void onEditorClosed();

        /** Play the menu's entrance animation on the container the editor restored. */
        void animateMenuEnter(View menuContainer);
    }

    private final Activity activity;
    private final Host host;

    private View overlayView;
    private WindowManager windowManager;
    private WindowManager.LayoutParams wmParams;

    private View hudEditorToolsView;
    private HudSafeAreaView hudSafeAreaView;
    private SeekBar hudButtonSizeSeekBar;
    private SeekBar hudButtonOpacitySeekBar;
    private TextView hudButtonSizeText;
    private TextView hudButtonOpacityText;

    /** Guards so setting a slider's progress does not loop back through its own listener. */
    private boolean updatingHudButtonSize;
    private boolean updatingHudButtonOpacity;

    HudEditorController(Activity activity, Host host) {
        this.activity = activity;
        this.host = host;
    }

    /**
     * Binds the HUD tool views inside a freshly inflated overlay and wires their listeners.
     *
     * <p>Called from the menu's {@code setupViews}, after the overlay is inflated. The window
     * references are taken here and refreshed by {@link #updateWindow} on every later show, because
     * the menu re-creates its layout params each time.
     */
    void bind(View overlayView, WindowManager windowManager, WindowManager.LayoutParams wmParams) {
        this.overlayView = overlayView;
        updateWindow(windowManager, wmParams);

        hudEditorToolsView = overlayView.findViewById(R.id.hud_editor_tools);
        hudSafeAreaView = overlayView.findViewById(R.id.hud_safe_area);
        View dragHandle = overlayView.findViewById(R.id.hud_editor_drag_handle);
        if (dragHandle != null) bindDragHandle(dragHandle, hudEditorToolsView);

        hudButtonSizeSeekBar = overlayView.findViewById(R.id.seekbar_hud_button_size);
        hudButtonSizeText = overlayView.findViewById(R.id.text_hud_button_size);
        hudButtonOpacitySeekBar = overlayView.findViewById(R.id.seekbar_hud_button_opacity);
        hudButtonOpacityText = overlayView.findViewById(R.id.text_hud_button_opacity);

        if (hudButtonSizeSeekBar != null) {
            hudButtonSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (!fromUser || updatingHudButtonSize) return;
                    InbuiltOverlayManager manager = InbuiltOverlayManager.getInstance();
                    if (manager != null) {
                        manager.setSelectedHudEditorButtonSize(progress);
                    }
                    updateSizeText(progress);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
            updateSizeControls(0);
        }

        if (hudButtonOpacitySeekBar != null) {
            hudButtonOpacitySeekBar.setOnSeekBarChangeListener(
                    new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress,
                                                      boolean fromUser) {
                            if (!fromUser || updatingHudButtonOpacity) return;
                            InbuiltOverlayManager manager = InbuiltOverlayManager.getInstance();
                            if (manager != null) {
                                manager.setSelectedHudEditorOpacity(progress);
                            }
                            updateOpacityText(progress);
                        }

                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {
                        }

                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) {
                        }
                    });
            updateOpacityControls(0);
        }

        View btnHudSave = overlayView.findViewById(R.id.btn_hud_save);
        if (btnHudSave != null) {
            btnHudSave.setOnClickListener(v -> exit(menuContainer()));
        }

        View btnHudReset = overlayView.findViewById(R.id.btn_hud_reset);
        if (btnHudReset != null) {
            btnHudReset.setOnClickListener(v -> {
                InbuiltOverlayManager manager = InbuiltOverlayManager.getInstance();
                if (manager != null) manager.resetAllPositionsToCenter();
            });
        }

        View btnHudCancel = overlayView.findViewById(R.id.btn_hud_cancel);
        if (btnHudCancel != null) {
            btnHudCancel.setOnClickListener(v -> exit(menuContainer()));
        }
    }

    private View menuContainer() {
        return overlayView == null ? null : overlayView.findViewById(R.id.mod_menu_container);
    }

    /** Refreshes the window references after the menu re-created its layout params. */
    void updateWindow(WindowManager windowManager, WindowManager.LayoutParams wmParams) {
        this.windowManager = windowManager;
        this.wmParams = wmParams;
    }

    /** The HUD tool container, or null before {@link #bind}. The menu reads it to open the editor. */
    View toolsView() {
        return hudEditorToolsView;
    }

    /** True while the tool container is on screen. */
    boolean isOpen() {
        return hudEditorToolsView != null && hudEditorToolsView.getVisibility() == View.VISIBLE;
    }

    /** Opens the editor: hides the menu, floats the tool window and shows the safe-area guide. */
    void enter(View modMenuContainer) {
        host.onEditorNavigation();

        if (modMenuContainer != null) {
            modMenuContainer.setVisibility(View.GONE);
        }
        if (hudEditorToolsView != null) {
            hudEditorToolsView.setVisibility(View.VISIBLE);
            hudEditorToolsView.setAlpha(1f);
            hudEditorToolsView.setTranslationX(0f);
        }
        showSafeArea(true);
        if (overlayView != null) {
            overlayView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            overlayView.setClickable(false);
        }
        applyWindowMode(false);
        // Wait for the compact tool window to be measured before centering it.
        final View editorRoot = overlayView;
        if (editorRoot != null) {
            editorRoot.post(() -> {
                if (overlayView != editorRoot || !isOpen()) return;
                OverlayBounds.Position rightEdge = OverlayBounds.clampPosition(
                        activity, editorRoot, Integer.MAX_VALUE, 0);
                moveTools(rightEdge.x / 2, 0);
            });
        }
        InbuiltOverlayManager overlayManager = InbuiltOverlayManager.getInstance();
        if (overlayManager != null) {
            overlayManager.setHudEditorSelectionListener(this::onSelectionChanged);
            overlayManager.setHudEditorMode(true);
        }
    }

    /** Closes the editor: restores the menu window and returns to the module list. */
    void exit(View modMenuContainer) {
        if (hudEditorToolsView != null) {
            hudEditorToolsView.setVisibility(View.GONE);
        }
        showSafeArea(false);
        if (overlayView != null) {
            overlayView.setClickable(true);
        }
        applyWindowMode(true);
        if (modMenuContainer != null) {
            modMenuContainer.setVisibility(View.VISIBLE);
            host.animateMenuEnter(modMenuContainer);
        }
        InbuiltOverlayManager overlayManager = InbuiltOverlayManager.getInstance();
        if (overlayManager != null) {
            overlayManager.setHudEditorMode(false);
            overlayManager.setHudEditorSelectionListener(null);
        }
        host.onEditorClosed();
    }

    /**
     * Switches the overlay window between the floating tool window and the full-screen menu.
     *
     * @param fullScreen true restores the menu's full-screen window, false shrinks it to the
     *                   wrap-content tool window the editor drags around
     */
    private void applyWindowMode(boolean fullScreen) {
        final int width = fullScreen
                ? WindowManager.LayoutParams.MATCH_PARENT
                : WindowManager.LayoutParams.WRAP_CONTENT;
        final int height = fullScreen
                ? WindowManager.LayoutParams.MATCH_PARENT
                : WindowManager.LayoutParams.WRAP_CONTENT;
        final int gravity = fullScreen ? Gravity.CENTER : (Gravity.TOP | Gravity.LEFT);

        if (wmParams != null && windowManager != null && overlayView != null) {
            wmParams.width = width;
            wmParams.height = height;
            wmParams.gravity = gravity;
            wmParams.x = 0;
            wmParams.y = 0;
            windowManager.updateViewLayout(overlayView, wmParams);
        } else if (overlayView != null
                && overlayView.getLayoutParams() instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams params =
                    (FrameLayout.LayoutParams) overlayView.getLayoutParams();
            params.width = width;
            params.height = height;
            params.gravity = gravity;
            params.leftMargin = 0;
            params.topMargin = 0;
            overlayView.setLayoutParams(params);
        }
    }

    /** The drag handle that moves the floating tool window. */
    private void bindDragHandle(View handle, View tools) {
        final int touchSlop = ViewConfiguration.get(activity).getScaledTouchSlop();
        handle.setOnTouchListener(new View.OnTouchListener() {
            private float downX;
            private float downY;
            private int startX;
            private int startY;
            private int pointerId = -1;
            private boolean dragging;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                if (overlayView == null || tools == null
                        || tools.getVisibility() != View.VISIBLE) {
                    return false;
                }
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        pointerId = event.getPointerId(0);
                        downX = event.getRawX();
                        downY = event.getRawY();
                        dragging = false;
                        if (wmParams != null) {
                            startX = wmParams.x;
                            startY = wmParams.y;
                        } else if (overlayView.getLayoutParams()
                                instanceof FrameLayout.LayoutParams) {
                            FrameLayout.LayoutParams params =
                                    (FrameLayout.LayoutParams) overlayView.getLayoutParams();
                            startX = params.leftMargin;
                            startY = params.topMargin;
                        }
                        view.getParent().requestDisallowInterceptTouchEvent(true);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (pointerId < 0 || event.getPointerId(0) != pointerId) return true;
                        float dx = event.getRawX() - downX;
                        float dy = event.getRawY() - downY;
                        if (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop) dragging = true;
                        if (dragging) {
                            moveTools(startX + Math.round(dx), startY + Math.round(dy));
                        }
                        return true;
                    case MotionEvent.ACTION_POINTER_UP:
                        if (event.getPointerId(event.getActionIndex()) != pointerId) return true;
                        pointerId = -1;
                        view.getParent().requestDisallowInterceptTouchEvent(false);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (pointerId >= 0 && !dragging) view.performClick();
                        // Fall through: an up and a cancel both release the gesture.
                    case MotionEvent.ACTION_CANCEL:
                        pointerId = -1;
                        dragging = false;
                        view.getParent().requestDisallowInterceptTouchEvent(false);
                        return true;
                    default:
                        return true;
                }
            }
        });
    }

    /** Moves the floating tool window to a clamped position. */
    private void moveTools(int x, int y) {
        if (overlayView == null) return;
        OverlayBounds.Position position = OverlayBounds.clampPosition(activity, overlayView, x, y);
        if (wmParams != null && windowManager != null) {
            wmParams.x = position.x;
            wmParams.y = position.y;
            windowManager.updateViewLayout(overlayView, wmParams);
        } else if (overlayView.getLayoutParams() instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams params =
                    (FrameLayout.LayoutParams) overlayView.getLayoutParams();
            params.leftMargin = position.x;
            params.topMargin = position.y;
            overlayView.setLayoutParams(params);
        }
    }

    /**
     * Shows or hides the safe-area frame.
     *
     * <p>The frame is measured against the window it will cover, so it is populated after the first
     * layout pass rather than at inflation, when the window size is still unknown.
     */
    private void showSafeArea(boolean show) {
        if (hudSafeAreaView == null) return;
        if (!show) {
            hudSafeAreaView.setVisibility(View.GONE);
            return;
        }
        hudSafeAreaView.setVisibility(View.VISIBLE);
        hudSafeAreaView.post(() -> {
            if (hudSafeAreaView == null || overlayView == null) return;
            int width = overlayView.getWidth();
            int height = overlayView.getHeight();
            if (width <= 0 || height <= 0) return;
            OverlaySafeArea.Rect safe = OverlaySafeArea.forView(overlayView, width, height);
            int gridPx = Math.round(OverlaySafeArea.DEFAULT_GRID_DP
                    * activity.getResources().getDisplayMetrics().density);
            hudSafeAreaView.setGeometry(safe, OverlaySafeArea.cutoutBounds(overlayView), gridPx);
        });
    }

    /** A selection arrived from the overlay manager: show its size and opacity. */
    private void onSelectionChanged(int sizeDp, int opacity) {
        updateSizeControls(sizeDp);
        updateOpacityControls(opacity);
    }

    private void updateSizeControls(int sizeDp) {
        if (hudButtonSizeSeekBar == null) return;
        boolean hasSelection = sizeDp > 0;
        hudButtonSizeSeekBar.setEnabled(hasSelection);
        if (hudButtonOpacitySeekBar != null) {
            hudButtonOpacitySeekBar.setEnabled(hasSelection);
        }
        if (!hasSelection) {
            updateSizeText(0);
            updateOpacityText(0);
            return;
        }
        updatingHudButtonSize = true;
        hudButtonSizeSeekBar.setProgress(sizeDp);
        updatingHudButtonSize = false;
        updateSizeText(sizeDp);
    }

    private void updateOpacityControls(int opacity) {
        if (hudButtonOpacitySeekBar == null) return;
        if (opacity <= 0) {
            updateOpacityText(0);
            return;
        }
        updatingHudButtonOpacity = true;
        hudButtonOpacitySeekBar.setProgress(opacity);
        updatingHudButtonOpacity = false;
        updateOpacityText(opacity);
    }

    private void updateOpacityText(int opacity) {
        if (hudButtonOpacityText == null) return;
        if (opacity <= 0) {
            hudButtonOpacityText.setText(activity.getString(R.string.overlay_button_opacity));
        } else {
            hudButtonOpacityText.setText(
                    activity.getString(R.string.overlay_button_opacity_value, opacity));
        }
    }

    private void updateSizeText(int size) {
        if (hudButtonSizeText == null) return;
        if (size <= 0) {
            hudButtonSizeText.setText(activity.getString(R.string.overlay_button_size));
        } else {
            hudButtonSizeText.setText(
                    activity.getString(R.string.overlay_button_size_value, size));
        }
    }
}

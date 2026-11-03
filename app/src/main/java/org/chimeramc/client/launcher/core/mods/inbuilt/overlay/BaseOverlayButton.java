package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import org.chimeramc.client.R;
import org.chimeramc.client.core.mods.inbuilt.manager.InbuiltModManager;
import org.chimeramc.client.util.MainThread;

public abstract class BaseOverlayButton {
    protected final Activity activity;
    protected View overlayView;
    protected WindowManager windowManager;
    protected WindowManager.LayoutParams wmParams;
    private float initialX, initialY;
    private float initialTouchX, initialTouchY;
    private boolean isDragging = false;
    private boolean isLocked = false;
    // The corner being resized in the HUD editor, or null while moving.
    private OverlayResizeHandle.Corner resizeCorner = null;
    private OverlayResizeHandle.Geometry resizeStart;
    private long touchDownTime = 0;
    private static final long TAP_TIMEOUT = 200;
    private static final float DRAG_THRESHOLD = 10f;
    private final Handler handler = MainThread.handler();
    private boolean isShowing = false;
    private boolean isHiding = false;
    private Runnable pendingShowRunnable;
    private boolean isHudEditorSelected = false;
    
    public BaseOverlayButton(Activity activity) {
        this.activity = activity;
        this.windowManager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
    }

    protected int getButtonSizePx() {
        int sizeDp = InbuiltModManager.getInstance(activity).getOverlayButtonSize(getModId());
        float density = activity.getResources().getDisplayMetrics().density;
        return (int) (sizeDp * density);
    }

    protected int getButtonWidthPx() {
        return (int) (getButtonSizePx() * getWidthScale());
    }

    protected int getButtonHeightPx() {
        return (int) (getButtonSizePx() * getHeightScale());
    }

    protected float getWidthScale() {
        return 1.0f;
    }

    protected float getHeightScale() {
        return 1.0f;
    }

    protected float getButtonOpacity() {
        int opacity = InbuiltModManager.getInstance(activity).getOverlayOpacity(getModId());
        return opacity / 100f;
    }

    protected void applyOpacity() {
        if (overlayView != null) {
            overlayView.setAlpha(getButtonOpacity());
        }
    }

    protected void updateLockState() {
        isLocked = InbuiltModManager.getInstance(activity).isOverlayLocked(getModId());
    }

    protected abstract String getModId();

    protected int getLayoutResource() {
        return R.layout.overlay_mod_button;
    }

    public void tick() {}

    protected void onOverlayGeometryChanged(int x, int y, int width, int height) {}

    private void notifyOverlayGeometryChanged() {
        if (overlayView == null) return;
        if (wmParams != null) {
            onOverlayGeometryChanged(wmParams.x, wmParams.y, wmParams.width, wmParams.height);
            return;
        }
        ViewGroup.LayoutParams params = overlayView.getLayoutParams();
        if (params instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams frameParams = (FrameLayout.LayoutParams) params;
            onOverlayGeometryChanged(frameParams.leftMargin, frameParams.topMargin, frameParams.width, frameParams.height);
        }
    }

    public void show(int startX, int startY) {
        if (isShowing || isHiding) return;
        if (pendingShowRunnable != null) {
            handler.removeCallbacks(pendingShowRunnable);
        }
        pendingShowRunnable = () -> showInternal(startX, startY);
        handler.postDelayed(pendingShowRunnable, 500);
    }

    private void showInternal(int startX, int startY) {
        pendingShowRunnable = null;
        if (isShowing || isHiding || activity.isFinishing() || activity.isDestroyed()) return;
        
        try {
            overlayView = LayoutInflater.from(activity).inflate(getLayoutResource(), null);
            if (overlayView instanceof ImageButton) {
                ImageButton btn = (ImageButton) overlayView;
                if (getIconResource() != 0) {
                    btn.setImageResource(getIconResource());
                }
                btn.setScaleType(ImageButton.ScaleType.FIT_CENTER);
            }
            configureOverlayView(overlayView);

            int buttonWidth = getButtonWidthPx();
            int buttonHeight = getButtonHeightPx();
            wmParams = new WindowManager.LayoutParams(
                buttonWidth,
                buttonHeight,
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            );
            wmParams.gravity = Gravity.TOP | Gravity.START;
            OverlayBounds.Position position = OverlayBounds.clampPosition(
                activity, startX, startY, buttonWidth, buttonHeight);
            wmParams.x = position.x;
            wmParams.y = position.y;
            wmParams.token = activity.getWindow().getDecorView().getWindowToken();

            overlayView.setOnTouchListener(this::handleTouch);
            overlayView.setVisibility(View.GONE);
            windowManager.addView(overlayView, wmParams);
            isShowing = true;
            applyOpacity();
            updateLockState();
            notifyOverlayGeometryChanged();
            refreshRuntimeVisibility();
        } catch (Exception e) {
            showFallback(startX, startY);
        }
    }

    private void showFallback(int startX, int startY) {
        if (isShowing) return;
        
        ViewGroup rootView = activity.findViewById(android.R.id.content);
        if (rootView == null) return;

        overlayView = LayoutInflater.from(activity).inflate(getLayoutResource(), null);
        if (overlayView instanceof ImageButton) {
            ImageButton btn = (ImageButton) overlayView;
            if (getIconResource() != 0) {
                btn.setImageResource(getIconResource());
            }
            btn.setScaleType(ImageButton.ScaleType.FIT_CENTER);
        }
        configureOverlayView(overlayView);

        int buttonWidth = getButtonWidthPx();
        int buttonHeight = getButtonHeightPx();
        
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            buttonWidth,
            buttonHeight
        );
        params.gravity = Gravity.TOP | Gravity.START;
        OverlayBounds.Position position = OverlayBounds.clampPosition(
            activity, startX, startY, buttonWidth, buttonHeight);
        params.leftMargin = position.x;
        params.topMargin = position.y;

        overlayView.setOnTouchListener(this::handleTouchFallback);
        overlayView.setVisibility(View.GONE);
        rootView.addView(overlayView, params);
        isShowing = true;
        wmParams = null;
        applyOpacity();
        updateLockState();
        notifyOverlayGeometryChanged();
        refreshRuntimeVisibility();
    }

    private void refreshRuntimeVisibility() {
        InbuiltOverlayManager manager = InbuiltOverlayManager.getInstance();
        if (manager != null) {
            manager.refreshRuntimeVisibility();
        }
    }

    public void hide() {
        if (pendingShowRunnable != null) {
            handler.removeCallbacks(pendingShowRunnable);
            pendingShowRunnable = null;
        }
        if (!isShowing || overlayView == null) {
            isShowing = false;
            return;
        }
        isHiding = true;
        try {
            if (wmParams != null && windowManager != null) {
                windowManager.removeView(overlayView);
            } else {
                ViewGroup rootView = activity.findViewById(android.R.id.content);
                if (rootView != null) {
                    rootView.removeView(overlayView);
                }
            }
        } catch (Exception ignored) {}
        overlayView = null;
        isShowing = false;
        isHiding = false;
    }

    private boolean isHudEditorMode = false;
    
    public void setHudEditorMode(boolean active) {
        this.isHudEditorMode = active;
        if (!active) {
            isHudEditorSelected = false;
        }
        refreshHudEditorBackground();
        refreshResizeHandles();
    }

    public void setHudEditorSelected(boolean selected) {
        isHudEditorSelected = selected;
        refreshHudEditorBackground();
        refreshResizeHandles();
    }

    /**
     * Shows the corner handles only on the selected overlay.
     *
     * <p>Painting handles on every widget at once would cover the HUD the player is trying to place;
     * showing them on the selection is what makes "grab the corner" discoverable without clutter.
     */
    private void refreshResizeHandles() {
        if (overlayView == null) return;
        if (isHudEditorMode && isHudEditorSelected) {
            float density = activity.getResources().getDisplayMetrics().density;
            OverlayResizeHandles.attach(overlayView, density, 0xFF3E6FF0);
        } else {
            OverlayResizeHandles.detach(overlayView);
        }
    }

    /** The dp size currently stored for this overlay. */
    protected int getStoredSizeDp() {
        return InbuiltModManager.getInstance(activity).getOverlayButtonSize(getModId());
    }

    /** Stores a new dp size and re-applies it to the live view. */
    protected void applySizeDp(int sizeDp) {
        InbuiltModManager.getInstance(activity).setOverlayButtonSize(getModId(), sizeDp);
        if (wmParams != null && windowManager != null && overlayView != null && isShowing) {
            int newWidth = getButtonWidthPx();
            int newHeight = getButtonHeightPx();
            wmParams.width = newWidth;
            wmParams.height = newHeight;
            OverlayBounds.Position position = OverlayBounds.clampPosition(
                    activity, wmParams.x, wmParams.y, newWidth, newHeight);
            wmParams.x = position.x;
            wmParams.y = position.y;
            try {
                windowManager.updateViewLayout(overlayView, wmParams);
            } catch (Exception ignored) {
            }
            notifyOverlayGeometryChanged();
        } else if (overlayView != null && isShowing) {
            ViewGroup.LayoutParams params = overlayView.getLayoutParams();
            if (params != null) {
                params.width = getButtonWidthPx();
                params.height = getButtonHeightPx();
                overlayView.setLayoutParams(params);
                notifyOverlayGeometryChanged();
            }
        }
        onButtonSizeChanged();
        refreshResizeHandles();
    }

    public String getOverlayConfigKey() {
        return getModId();
    }

    public int getCurrentButtonSizeDp() {
        return InbuiltModManager.getInstance(activity).getOverlayButtonSize(getModId());
    }

    private void refreshHudEditorBackground() {
        if (overlayView != null) {
            if (isHudEditorMode) {
                overlayView.setBackgroundColor(isHudEditorSelected ? 0x666236E8 : 0x44FFFFFF);
            } else {
                overlayView.setBackgroundColor(Color.TRANSPARENT);
            }
        }
    }

    public void updatePosition(int x, int y) {
        if (wmParams != null && windowManager != null && overlayView != null && isShowing) {
            OverlayBounds.Position position = OverlayBounds.clampPosition(activity, overlayView, x, y);
            wmParams.x = position.x;
            wmParams.y = position.y;
            windowManager.updateViewLayout(overlayView, wmParams);
        } else if (overlayView != null && isShowing) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) overlayView.getLayoutParams();
            if (params != null) {
                OverlayBounds.Position position = OverlayBounds.clampPosition(activity, overlayView, x, y);
                params.leftMargin = position.x;
                params.topMargin = position.y;
                overlayView.setLayoutParams(params);
            }
        }
    }

    private boolean handleTouch(View v, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (isHudEditorMode) {
                    InbuiltOverlayManager manager = InbuiltOverlayManager.getInstance();
                    if (manager != null) {
                        manager.selectHudEditorOverlay(this);
                    }
                    // A press on a corner starts a resize, not a move, so the widget grows from the
                    // corner the player grabbed instead of sliding.
                    beginResize(event.getX(), event.getY());
                }
                initialX = wmParams.x;
                initialY = wmParams.y;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                isDragging = false;
                touchDownTime = SystemClock.uptimeMillis();
                v.getParent().requestDisallowInterceptTouchEvent(!isLocked || isHudEditorMode);
                if (!isHudEditorMode) {
                    onButtonPressStart();
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - initialTouchX;
                float dy = event.getRawY() - initialTouchY;
                if (resizeCorner != null) {
                    updateResize(dx, dy);
                    return true;
                }
                if (Math.abs(dx) > DRAG_THRESHOLD || Math.abs(dy) > DRAG_THRESHOLD) {
                    if (isHudEditorMode) {
                        isDragging = true;
                    }
                }
                if (isDragging && isHudEditorMode && windowManager != null && overlayView != null) {
                    OverlayBounds.Position position = OverlayBounds.clampPosition(
                        activity, overlayView, (int) (initialX + dx), (int) (initialY + dy));
                    wmParams.x = position.x;
                    wmParams.y = position.y;
                    windowManager.updateViewLayout(overlayView, wmParams);
                    notifyOverlayGeometryChanged();
                }
                return isHudEditorMode || !isDragging;

            case MotionEvent.ACTION_UP:
                long elapsed = SystemClock.uptimeMillis() - touchDownTime;
                float totalDx = event.getRawX() - initialTouchX;
                float totalDy = event.getRawY() - initialTouchY;
                boolean wasDragging = Math.abs(totalDx) > DRAG_THRESHOLD || Math.abs(totalDy) > DRAG_THRESHOLD;

                if (resizeCorner != null) {
                    commitResize();
                } else if (!wasDragging && elapsed < TAP_TIMEOUT && !isHudEditorMode) {
                    handler.post(this::onButtonClick);
                } else if (isDragging && isHudEditorMode) {
                    savePosition(wmParams.x, wmParams.y);
                }
                if (!isHudEditorMode) {
                    onButtonPressEnd();
                }
                isDragging = false;
                v.getParent().requestDisallowInterceptTouchEvent(false);
                return true;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_OUTSIDE:
                if (!isHudEditorMode) {
                    onButtonPressEnd();
                }
                resizeCorner = null;
                isDragging = false;
                v.getParent().requestDisallowInterceptTouchEvent(false);
                return false;
        }
        return false;
    }

    /**
     * Starts a resize if the press landed on a corner handle.
     *
     * <p>Only in the HUD editor and only on the selected overlay, so a gameplay tap is never turned
     * into a resize and an unselected widget still moves when dragged.
     */
    private void beginResize(float localX, float localY) {
        if (!isHudEditorMode || !isHudEditorSelected || overlayView == null || wmParams == null) {
            resizeCorner = null;
            return;
        }
        float density = activity.getResources().getDisplayMetrics().density;
        resizeCorner = OverlayResizeHandle.cornerAt(
                localX, localY, wmParams.width, wmParams.height, density);
        if (resizeCorner != null) {
            resizeStart = new OverlayResizeHandle.Geometry(
                    wmParams.x, wmParams.y, wmParams.width, wmParams.height);
        }
    }

    /** Applies a resize drag to the live view. */
    private void updateResize(float dx, float dy) {
        if (resizeStart == null || overlayView == null || wmParams == null) return;
        float density = activity.getResources().getDisplayMetrics().density;
        OverlayResizeHandle.Geometry next = OverlayResizeHandle.resize(
                resizeCorner, resizeStart, (int) dx, (int) dy,
                getWidthScale(), getHeightScale(), density);
        wmParams.x = next.x;
        wmParams.y = next.y;
        wmParams.width = next.width;
        wmParams.height = next.height;
        try {
            windowManager.updateViewLayout(overlayView, wmParams);
        } catch (Exception ignored) {
            return;
        }
        notifyOverlayGeometryChanged();
    }

    /** Stores the size the drag settled on, so the slider and the handle agree afterwards. */
    private void commitResize() {
        if (resizeStart != null && wmParams != null) {
            float density = activity.getResources().getDisplayMetrics().density;
            int sizeDp = OverlayResizeHandle.sizeDpFor(
                    wmParams.width, wmParams.height, getWidthScale(), getHeightScale(), density);
            InbuiltModManager.getInstance(activity).setOverlayButtonSize(getModId(), sizeDp);
            onButtonSizeChanged();
        }
        resizeCorner = null;
        resizeStart = null;
        savePosition(wmParams == null ? 0 : wmParams.x, wmParams == null ? 0 : wmParams.y);
    }

    protected void savePosition(int x, int y) {
        InbuiltModManager.getInstance(activity).setOverlayPosition(getModId(), x, y);
    }

    private boolean handleTouchFallback(View v, MotionEvent event) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) overlayView.getLayoutParams();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (isHudEditorMode) {
                    InbuiltOverlayManager manager = InbuiltOverlayManager.getInstance();
                    if (manager != null) {
                        manager.selectHudEditorOverlay(this);
                    }
                }
                initialX = params.leftMargin;
                initialY = params.topMargin;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                isDragging = false;
                touchDownTime = SystemClock.uptimeMillis();
                v.getParent().requestDisallowInterceptTouchEvent(!isLocked || isHudEditorMode);
                if (!isHudEditorMode) {
                    onButtonPressStart();
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - initialTouchX;
                float dy = event.getRawY() - initialTouchY;
                if (Math.abs(dx) > DRAG_THRESHOLD || Math.abs(dy) > DRAG_THRESHOLD) {
                    if (!isLocked || isHudEditorMode) {
                        isDragging = true;
                    }
                }
                if (isDragging && (!isLocked || isHudEditorMode)) {
                    OverlayBounds.Position position = OverlayBounds.clampPosition(
                        activity, overlayView, (int) (initialX + dx), (int) (initialY + dy));
                    params.leftMargin = position.x;
                    params.topMargin = position.y;
                    overlayView.setLayoutParams(params);
                    notifyOverlayGeometryChanged();
                }
                return isHudEditorMode || !isLocked || !isDragging;

            case MotionEvent.ACTION_UP:
                long elapsed = SystemClock.uptimeMillis() - touchDownTime;
                float totalDx = event.getRawX() - initialTouchX;
                float totalDy = event.getRawY() - initialTouchY;
                boolean wasDragging = Math.abs(totalDx) > DRAG_THRESHOLD || Math.abs(totalDy) > DRAG_THRESHOLD;

                if (!wasDragging && elapsed < TAP_TIMEOUT && !isHudEditorMode) {
                    handler.post(this::onButtonClick);
                } else if (isDragging && (!isLocked || isHudEditorMode)) {
                    savePosition(params.leftMargin, params.topMargin);
                }
                if (!isHudEditorMode) {
                    onButtonPressEnd();
                }
                isDragging = false;
                v.getParent().requestDisallowInterceptTouchEvent(false);
                return true;

            case MotionEvent.ACTION_CANCEL:
                if (!isHudEditorMode) {
                    onButtonPressEnd();
                }
                isDragging = false;
                v.getParent().requestDisallowInterceptTouchEvent(false);
                return true;
        }
        return false;
    }

    protected void sendKey(int keyCode) {
        handler.post(() -> {
            long time = SystemClock.uptimeMillis();
            KeyEvent down = new KeyEvent(time, time, KeyEvent.ACTION_DOWN, keyCode, 0, 0, -1, 0, 0, InputDevice.SOURCE_KEYBOARD);
            KeyEvent up = new KeyEvent(time, time + 10, KeyEvent.ACTION_UP, keyCode, 0, 0, -1, 0, 0, InputDevice.SOURCE_KEYBOARD);
            activity.dispatchKeyEvent(down);
            activity.dispatchKeyEvent(up);
        });
    }

    protected void sendKeyDown(int keyCode) {
        handler.post(() -> {
            long time = SystemClock.uptimeMillis();
            KeyEvent down = new KeyEvent(time, time, KeyEvent.ACTION_DOWN, keyCode, 0, 0, -1, 0, 0, InputDevice.SOURCE_KEYBOARD);
            activity.dispatchKeyEvent(down);
        });
    }

    protected void sendKeyUp(int keyCode) {
        handler.post(() -> {
            long time = SystemClock.uptimeMillis();
            KeyEvent up = new KeyEvent(time, time, KeyEvent.ACTION_UP, keyCode, 0, 0, -1, 0, 0, InputDevice.SOURCE_KEYBOARD);
            activity.dispatchKeyEvent(up);
        });
    }

    protected abstract int getIconResource();
    protected abstract void onButtonClick();

    protected void configureOverlayView(View view) {}

    protected void onButtonPressStart() {}

    protected void onButtonPressEnd() {}

    public void applyConfigurationChanges() {
        if (!isShowing || overlayView == null) return;

        int newWidth = getButtonWidthPx();
        int newHeight = getButtonHeightPx();
        if (wmParams != null) {
            wmParams.width = newWidth;
            wmParams.height = newHeight;
            OverlayBounds.Position position = OverlayBounds.clampPosition(
                activity, wmParams.x, wmParams.y, newWidth, newHeight);
            wmParams.x = position.x;
            wmParams.y = position.y;
            try {
                windowManager.updateViewLayout(overlayView, wmParams);
            } catch (Exception ignored) {}
            savePosition(wmParams.x, wmParams.y);
        } else {
            ViewGroup.LayoutParams params = overlayView.getLayoutParams();
            if (params != null) {
                params.width = newWidth;
                params.height = newHeight;
                if (params instanceof FrameLayout.LayoutParams) {
                    FrameLayout.LayoutParams frameParams = (FrameLayout.LayoutParams) params;
                    OverlayBounds.Position position = OverlayBounds.clampPosition(
                        activity, frameParams.leftMargin, frameParams.topMargin, newWidth, newHeight);
                    frameParams.leftMargin = position.x;
                    frameParams.topMargin = position.y;
                    savePosition(position.x, position.y);
                }
                overlayView.setLayoutParams(params);
            }
        }

        applyOpacity();

        updateLockState();
        notifyOverlayGeometryChanged();
        onButtonSizeChanged();
        refreshResizeHandles();
    }

    protected void onButtonSizeChanged() {}
}

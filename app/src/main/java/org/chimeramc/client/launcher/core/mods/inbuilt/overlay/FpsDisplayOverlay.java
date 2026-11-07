package org.chimeramc.client.core.mods.inbuilt.overlay;

import org.chimeramc.client.util.AppLog;

import android.app.Activity;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.chimeramc.client.R;
import org.chimeramc.client.core.mods.inbuilt.manager.InbuiltModManager;
import org.chimeramc.client.core.mods.inbuilt.model.ModIds;
import org.chimeramc.client.launcher.core.mods.inbuilt.nativemod.FpsMod;
import org.chimeramc.client.launcher.core.mods.inbuilt.overlay.FrameTimeGraphView;
import org.chimeramc.client.util.MainThread;
import org.chimeramc.client.util.OverlayRefreshGate;

public class FpsDisplayOverlay {
    private static final int UPDATE_INTERVAL = 250;
    private static final float DRAG_THRESHOLD = 10f;

    private final Activity activity;
    private final WindowManager windowManager;
    private final Handler handler = MainThread.handler();

    private View overlayView;
    private TextView statsText;
    private TextView hudStats;
    private View hudAccent;
    private FrameTimeGraphView graphView;
    private boolean statsEnabled;
    private WindowManager.LayoutParams wmParams;
    private boolean isShowing = false;
    private Runnable pendingShowRunnable;
    private boolean initialized = false;

    /**
     * Host-side fallback meter. The native {@code FpsMod} only reports when its hook resolves a
     * game address, which not every build does; without this the readout stayed on "FPS: --"
     * forever. The Choreographer count works on any build and is what the overlay shows when the
     * native value is unavailable.
     */
    private final FrameRateMeter fallbackMeter = new FrameRateMeter();
    private final OverlayRefreshGate refreshGate =
            new OverlayRefreshGate(UPDATE_INTERVAL * 4L);
    private volatile int fallbackFps;

    private float initialX, initialY;
    private float initialTouchX, initialTouchY;
    private boolean isDragging = false;
    private boolean isLocked = false;
    private long touchDownTime = 0;

    private final Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isShowing) return;
            updateDisplay();
            handler.postDelayed(this, UPDATE_INTERVAL);
        }
    };

    public FpsDisplayOverlay(Activity activity) {
        this.activity = activity;
        this.windowManager = (WindowManager) activity.getSystemService(Activity.WINDOW_SERVICE);
    }

    public void show(int startX, int startY) {
        if (isShowing) return;
        if (pendingShowRunnable != null) {
            handler.removeCallbacks(pendingShowRunnable);
        }
        pendingShowRunnable = () -> showInternal(startX, startY);
        handler.postDelayed(pendingShowRunnable, 500);
    }

    private void initializeNative() {
        handler.postDelayed(() -> {
            if (FpsMod.init()) {
                initialized = true;
            }
        }, 1000);
    }

    /**
     * Inflates the overlay body: the plain readout, or the readout plus the frame-time graph when
     * the user enabled it. Both layouts expose the same {@code stats_text} id, so the
     * rest of the overlay is unchanged; the graph is only present in the graph layout.
     */
    /**
     * Inflates the HUD readout: the number, the unit label, the level accent bar and, when the
     * user enabled it, the frame-time graph.
     *
     * <p>One layout rather than two. The old pair (a plain readout and a graph variant) had to be
     * kept in step by hand and the readout was a monospace line on a black rectangle; the panel is
     * the shared glass surface now and the graph is a child that is hidden unless it is wanted.
     */
    private void inflateOverlay() {
        InbuiltModManager manager = InbuiltModManager.getInstance(activity);
        statsEnabled = manager.isFpsStatsEnabled(ModIds.FPS_DISPLAY);
        boolean graph = manager.isFrameTimeGraphEnabled(ModIds.FPS_DISPLAY);
        overlayView = LayoutInflater.from(activity).inflate(R.layout.overlay_hud_readout, null);
        statsText = overlayView.findViewById(R.id.stats_text);
        hudStats = overlayView.findViewById(R.id.hud_stats);
        hudAccent = overlayView.findViewById(R.id.hud_accent);
        graphView = overlayView.findViewById(R.id.frame_time_graph);
        if (graphView != null) {
            graphView.setVisibility(graph ? View.VISIBLE : View.GONE);
            if (graph) {
                graphView.setHistory(fallbackMeter.history());
                graphView.setLineColor(activity.getColor(R.color.overlay_text_primary));
            } else {
                graphView = null;
            }
        }
    }

    private void showInternal(int startX, int startY) {
        pendingShowRunnable = null;
        if (isShowing || activity.isFinishing() || activity.isDestroyed()) return;

        if (!initialized) {
            initializeNative();
        }

        try {
            inflateOverlay();
            statsText.setText(activity.getString(R.string.mod_overlay_fps_no_reading));
            refreshGate.reset();

            wmParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            );
            wmParams.gravity = Gravity.TOP | Gravity.START;
            wmParams.x = startX;
            wmParams.y = startY;
            wmParams.token = activity.getWindow().getDecorView().getWindowToken();

            overlayView.setOnTouchListener(this::handleTouch);
            overlayView.setVisibility(View.GONE);
            windowManager.addView(overlayView, wmParams);
            isShowing = true;

            startFallbackMeter();
            handler.post(updateRunnable);
            applyOpacity();
            updateLockState();
            applySize();
            updatePosition(wmParams.x, wmParams.y);
            refreshRuntimeVisibility();
        } catch (Exception e) {
            showFallback(startX, startY);
        }
    }

    private void showFallback(int startX, int startY) {
        if (isShowing) return;
        ViewGroup rootView = activity.findViewById(android.R.id.content);
        if (rootView == null) return;

        inflateOverlay();
        statsText.setText(activity.getString(R.string.mod_overlay_fps_no_reading));

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        OverlayBounds.Position position = OverlayBounds.clampPosition(activity, overlayView, startX, startY);
        params.leftMargin = position.x;
        params.topMargin = position.y;

        overlayView.setOnTouchListener(this::handleTouchFallback);
        overlayView.setVisibility(View.GONE);
        rootView.addView(overlayView, params);
        isShowing = true;
        wmParams = null;

        startFallbackMeter();
        handler.post(updateRunnable);
        applyOpacity();
        updateLockState();
        applySize();
        refreshRuntimeVisibility();
    }

    private void refreshRuntimeVisibility() {
        InbuiltOverlayManager manager = InbuiltOverlayManager.getInstance();
        if (manager != null) {
            manager.refreshRuntimeVisibility();
        }
    }

    private void updateDisplay() {
        if (statsText == null) return;
        int fps = nativeFps();
        if (fps <= 0 && fallbackMeter.isRunning() && fallbackFps > 0) {
            // The native hook did not resolve on this build; show the host's own present rate
            // rather than a permanent "--", which read as a broken module.
            fps = fallbackFps;
        }
        // The timer runs four times a second but the readout changes far less often, so the
        // text is built and set only when the number actually differs.
        if (refreshGate.shouldRefresh(Integer.toString(fps), SystemClock.uptimeMillis())) {
            HudThreshold.Level level = HudThreshold.levelForFps(fps);
            statsText.setText(fps > 0
                    ? Integer.toString(fps)
                    : activity.getString(R.string.mod_overlay_fps_no_reading));
            // The level is carried by the accent bar's colour and by the shape cue on the stats
            // line, never by colouring the number: a coloured number is hard to read on a bright
            // scene and says nothing at all to a colour-blind player.
            if (hudAccent != null) {
                hudAccent.setBackgroundColor(HudThreshold.colourFor(level));
            }
            if (hudStats != null) {
                String stats = statsEnabled ? fpsStatsText() : null;
                String cue = HudThreshold.cue(level);
                String line = stats == null ? "" : stats;
                if (!cue.isEmpty() && !line.isEmpty()) line = cue + " " + line;
                else if (!cue.isEmpty()) line = cue;
                hudStats.setText(line);
                hudStats.setVisibility(line.isEmpty() ? View.GONE : View.VISIBLE);
            }
            if (graphView != null) {
                graphView.refresh();
            }
            clampCurrentPosition(false);
        }
    }

    /**
     * The average and 1% low over the configured window, as "60 avg / 41 low", or null
     * when there is not yet a reading. Computed from the same history the graph draws, so the two
     * can never disagree, and only on the once-a-second refresh — never on the frame path.
     */
    private String fpsStatsText() {
        FrameTimeHistory history = fallbackMeter.history();
        if (history == null || history.size() < 2) return null;
        int window = InbuiltModManager.getInstance(activity).getFpsStatsWindow(ModIds.FPS_DISPLAY);
        float avgMs = history.averageIntervalMs(window);
        if (avgMs <= 0f) return null;
        int avgFps = Math.round(1000f / avgMs);
        // The 1% low is the worst 1% of frames, i.e. the 99th percentile interval.
        float lowMs = history.percentileIntervalMs(window, 0.99f);
        int lowFps = lowMs > 0f ? Math.round(1000f / lowMs) : avgFps;
        return activity.getString(R.string.mod_overlay_fps_stats, avgFps, lowFps);
    }

    /**
     * Starts the host-side frame-rate meter, which drives the readout whenever the native hook is
     * unavailable. The listener runs on the main thread (Choreographer's thread) and only writes an
     * int the update loop reads, so it never touches a View from off the UI thread.
     */
    private void startFallbackMeter() {
        fallbackMeter.start(fps -> fallbackFps = fps);
    }

    /** The native FPS, or 0 when the hook is not available/initialised. */
    private int nativeFps() {
        try {
            if (initialized && FpsMod.nativeIsInitialized()) {
                int fps = FpsMod.nativeGetFps();
                return fps > 0 ? fps : 0;
            }
        } catch (Throwable ignored) {
            AppLog.warnOnce("GlowberryClient", "FpsDisplayOverlay:262", "Ignored a failure at FpsDisplayOverlay:262", ignored);
        }
        return 0;
    }

    private void applyOpacity() {
        if (overlayView != null) {
            int opacity = InbuiltModManager.getInstance(activity).getOverlayOpacity(ModIds.FPS_DISPLAY);
            if (overlayView.getBackground() != null) {
                overlayView.getBackground().mutate().setAlpha((int) (opacity * 2.55f));
            }
        }
    }

    private void applySize() {
        if (statsText != null) {
            int sizeDp = InbuiltModManager.getInstance(activity).getOverlayButtonSize(ModIds.FPS_DISPLAY);
            float scale = sizeDp / 56f;
            statsText.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12 * scale);
            
            float density = activity.getResources().getDisplayMetrics().density;
            int hPad = (int) (8 * scale * density);
            int vPad = (int) (4 * scale * density);
            statsText.setPadding(hPad, vPad, hPad, vPad);
            
            if (wmParams != null && windowManager != null && overlayView != null && isShowing) {
                try {
                    clampCurrentPosition(true);
                } catch (Exception ignored) {
                    AppLog.warnOnce("GlowberryClient", "FpsDisplayOverlay:291", "Ignored a failure at FpsDisplayOverlay:291", ignored);
                }
            }
        }
    }

    private void updateLockState() {
        isLocked = InbuiltModManager.getInstance(activity).isOverlayLocked(ModIds.FPS_DISPLAY);
    }

    private boolean isHudEditorMode = false;
    
    public void setHudEditorMode(boolean active) {
        this.isHudEditorMode = active;
        if (overlayView != null) {
            overlayView.setBackgroundColor(active ? 0x44FFFFFF : android.graphics.Color.TRANSPARENT);
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
                        manager.selectHudEditorDisplay(ModIds.FPS_DISPLAY);
                    }
                }
                initialX = wmParams.x;
                initialY = wmParams.y;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                isDragging = false;
                touchDownTime = SystemClock.uptimeMillis();
                v.getParent().requestDisallowInterceptTouchEvent(isHudEditorMode);
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - initialTouchX;
                float dy = event.getRawY() - initialTouchY;
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
                }
                return isHudEditorMode || !isDragging;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging && isHudEditorMode) {
                    savePosition(wmParams.x, wmParams.y);
                }
                isDragging = false;
                v.getParent().requestDisallowInterceptTouchEvent(false);
                return true;
        }
        return false;
    }

    private void savePosition(int x, int y) {
        InbuiltModManager.getInstance(activity).setOverlayPosition(ModIds.FPS_DISPLAY, x, y);
    }

    private boolean handleTouchFallback(View v, MotionEvent event) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) overlayView.getLayoutParams();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (isHudEditorMode) {
                    InbuiltOverlayManager manager = InbuiltOverlayManager.getInstance();
                    if (manager != null) {
                        manager.selectHudEditorDisplay(ModIds.FPS_DISPLAY);
                    }
                }
                initialX = params.leftMargin;
                initialY = params.topMargin;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                isDragging = false;
                touchDownTime = SystemClock.uptimeMillis();
                v.getParent().requestDisallowInterceptTouchEvent(isHudEditorMode);
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - initialTouchX;
                float dy = event.getRawY() - initialTouchY;
                if (Math.abs(dx) > DRAG_THRESHOLD || Math.abs(dy) > DRAG_THRESHOLD) {
                    if (isHudEditorMode) {
                        isDragging = true;
                    }
                }
                if (isDragging && isHudEditorMode) {
                    OverlayBounds.Position position = OverlayBounds.clampPosition(
                            activity, overlayView, (int) (initialX + dx), (int) (initialY + dy));
                    params.leftMargin = position.x;
                    params.topMargin = position.y;
                    overlayView.setLayoutParams(params);
                }
                return isHudEditorMode || !isDragging;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging && isHudEditorMode) {
                    savePosition(params.leftMargin, params.topMargin);
                }
                isDragging = false;
                v.getParent().requestDisallowInterceptTouchEvent(false);
                return true;
        }
        return false;
    }

    public void hide() {
        if (pendingShowRunnable != null) {
            handler.removeCallbacks(pendingShowRunnable);
            pendingShowRunnable = null;
        }
        if (!isShowing) return;
        isShowing = false;
        fallbackMeter.stop();
        fallbackFps = 0;
        handler.removeCallbacks(updateRunnable);
        try {
            if (wmParams != null && windowManager != null) {
                windowManager.removeView(overlayView);
            } else {
                ViewGroup rootView = activity.findViewById(android.R.id.content);
                if (rootView != null) {
                    rootView.removeView(overlayView);
                }
            }
        } catch (Exception ignored) {
            AppLog.warnOnce("GlowberryClient", "FpsDisplayOverlay:442", "Ignored a failure at FpsDisplayOverlay:442", ignored);
        }
        overlayView = null;
        statsText = null;
        hudStats = null;
        hudAccent = null;
        graphView = null;
        statsEnabled = false;
    }

    public void applyConfigurationChanges() {
        if (!isShowing || overlayView == null) return;
        applyOpacity();
        updateLockState();
        applySize();
    }

    public void setVisibility(int visibility) {
        if (overlayView != null) {
            overlayView.setVisibility(visibility);
        }
    }

    private void clampCurrentPosition(boolean save) {
        if (overlayView == null || !isShowing) return;
        if (wmParams != null && windowManager != null) {
            OverlayBounds.Position position = OverlayBounds.clampPosition(activity, overlayView, wmParams.x, wmParams.y);
            if (position.x != wmParams.x || position.y != wmParams.y) {
                wmParams.x = position.x;
                wmParams.y = position.y;
                windowManager.updateViewLayout(overlayView, wmParams);
                if (save) {
                    savePosition(position.x, position.y);
                }
            }
        } else {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) overlayView.getLayoutParams();
            if (params == null) return;
            OverlayBounds.Position position = OverlayBounds.clampPosition(
                    activity, overlayView, params.leftMargin, params.topMargin);
            if (position.x != params.leftMargin || position.y != params.topMargin) {
                params.leftMargin = position.x;
                params.topMargin = position.y;
                overlayView.setLayoutParams(params);
                if (save) {
                    savePosition(position.x, position.y);
                }
            }
        }
    }
}

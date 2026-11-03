package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.chimeramc.client.R;
import org.chimeramc.client.databinding.OverlayZoomButtonBinding;
import org.chimeramc.client.core.mods.inbuilt.manager.InbuiltModManager;
import org.chimeramc.client.core.mods.inbuilt.model.ModIds;
import org.chimeramc.client.launcher.core.mods.inbuilt.nativemod.ZoomMod;
import org.chimeramc.client.util.MainThread;

public class ZoomOverlay extends BaseOverlayButton {
    private boolean isZooming = false;
    private boolean initialized = false;
    private final Handler handler = MainThread.handler();
    private final ZoomScrollFilter scrollFilter = new ZoomScrollFilter();
    private ImageView iconView;
    private TextView levelView;

    public ZoomOverlay(Activity activity) {
        super(activity);
    }

    @Override
    protected String getModId() {
        return ModIds.ZOOM;
    }

    @Override
    protected int getIconResource() {
        return isZooming ? R.drawable.ic_zoom_pressed : R.drawable.ic_zoom_normal;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.overlay_zoom_button;
    }

    @Override
    protected void configureOverlayView(View view) {
        OverlayZoomButtonBinding binding = OverlayZoomButtonBinding.bind(view);
        iconView = binding.zoomIcon;
        levelView = binding.zoomLevel;
        updateZoomLevelLabel();
    }

    @Override
    public void show(int startX, int startY) {
        if (!initialized) {
            initializeNative();
        }
        super.show(startX, startY);
    }

    public void initializeForKeyboard() {
        if (!initialized) {
            initializeNative();
        }
    }

    private void initializeNative() {
        handler.postDelayed(() -> {
            if (ZoomMod.init()) {
                initialized = true;
                applyZoomLevel();
            }
        }, 1000);
    }

    private void applyZoomLevel() {
        int zoomPercent = InbuiltModManager.getInstance(activity).getZoomLevel();
        long normalFov = 5360000000L;
        long zoomDifference = 250000000L;
        long zoomLevel = normalFov - (long)(zoomDifference * (zoomPercent / 100.0));
        ZoomMod.nativeSetZoomLevel(zoomLevel);
        
        int transitionDuration = InbuiltModManager.getInstance(activity).getZoomTransitionDuration();
        ZoomMod.nativeSetTransitionDuration(transitionDuration);
    }

    @Override
    protected void onButtonClick() {
        toggleZoom();
    }

    public void onKeyDown() {
        if (!initialized) {
            return;
        }
        if (isZooming) return;
        
        isZooming = true;
        scrollFilter.reset();
        applyZoomLevel();
        ZoomMod.nativeOnKeyDown();
        updateButtonState(true);
    }

    public void onKeyUp() {
        if (!initialized || !isZooming) return;
        
        isZooming = false;
        ZoomMod.nativeOnKeyUp();
        updateButtonState(false);
    }

    private void toggleZoom() {
        if (!initialized) {
            return;
        }

        isZooming = !isZooming;

        if (isZooming) {
            applyZoomLevel();
            ZoomMod.nativeOnKeyDown();
            updateButtonState(true);
        } else {
            ZoomMod.nativeOnKeyUp();
            updateButtonState(false);
        }
    }

    private void updateButtonState(boolean active) {
        float userOpacity = getButtonOpacity();
        if (iconView != null) {
            iconView.setAlpha(userOpacity);
            iconView.setImageResource(active ? R.drawable.ic_zoom_pressed : R.drawable.ic_zoom_normal);
        } else if (overlayView instanceof ImageButton) {
            ImageButton btn = (ImageButton) overlayView;
            btn.setAlpha(userOpacity);
            btn.setImageResource(active ? R.drawable.ic_zoom_pressed : R.drawable.ic_zoom_normal);
        }
    }

    /** Prints the current zoom level beside the icon. */
    private void updateZoomLevelLabel() {
        if (levelView == null) return;
        levelView.setText(zoomLevelText());
    }

    /**
     * The zoom as a multiplier: the configured percentage maps the field of view onto a factor, so
     * "0%" is 1.0x and a larger percentage zooms further in. Kept in one place so the overlay's
     * label and any future readout cannot disagree.
     */
    private String zoomLevelText() {
        int percent = InbuiltModManager.getInstance(activity).getZoomLevel();
        float factor = 1f + Math.max(0, percent) / 50f;
        return activity.getString(R.string.mod_overlay_zoom_level, factor);
    }

    public void onScroll(float delta) {
        if (!initialized || !isZooming) return;
        InbuiltModManager manager = InbuiltModManager.getInstance(activity);
        ZoomMod.nativeOnScroll(scrollFilter.apply(
                delta, manager.getZoomScrollSensitivity(), manager.getZoomSmoothing()));
    }

    @Override
    public void hide() {
        handler.removeCallbacksAndMessages(null);
        iconView = null;
        levelView = null;
        if (isZooming && initialized) {
            ZoomMod.nativeOnKeyUp();
            isZooming = false;
        }
        super.hide();
    }

    public boolean isZooming() {
        return isZooming;
    }

    @Override
    public void applyConfigurationChanges() {
        super.applyConfigurationChanges();
        updateZoomLevelLabel();
        if (initialized) {
            applyZoomLevel();
        }
    }

    public boolean isInitialized() {
        return initialized;
    }
}

package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.chimeramc.client.R;
import org.chimeramc.client.databinding.OverlaySnaplookButtonBinding;
import org.chimeramc.client.core.mods.inbuilt.model.ModIds;
import org.chimeramc.client.launcher.core.mods.inbuilt.nativemod.SnaplookMod;
import org.chimeramc.client.util.MainThread;

public class SnaplookOverlay extends BaseOverlayButton {
    private boolean isActive = false;
    private boolean initialized = false;
    private final Handler handler = MainThread.handler();
    private ImageView iconView;
    private TextView stateView;

    public SnaplookOverlay(Activity activity) {
        super(activity);
    }

    @Override
    protected String getModId() {
        return ModIds.SNAPLOOK;
    }

    @Override
    protected int getIconResource() {
        return isActive ? R.drawable.ic_snaplook_pressed : R.drawable.ic_snaplook_normal;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.overlay_snaplook_button;
    }

    @Override
    protected void configureOverlayView(View view) {
        OverlaySnaplookButtonBinding binding = OverlaySnaplookButtonBinding.bind(view);
        iconView = binding.snaplookIcon;
        stateView = binding.snaplookState;
        updateStateLabel();
    }

    /** Shows whether snap-look is armed. */
    private void updateStateLabel() {
        if (stateView == null) return;
        stateView.setText(isActive ? R.string.mod_overlay_snaplook_on : R.string.mod_overlay_snaplook_off);
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
            if (SnaplookMod.init()) {
                initialized = true;
            }
        }, 1000);
    }

    @Override
    protected void onButtonClick() {
        if (!initialized) {
            return;
        }

        isActive = !isActive;

        if (isActive) {
            SnaplookMod.nativeOnKeyDown();
            updateButtonState(true);
        } else {
            SnaplookMod.nativeOnKeyUp();
            updateButtonState(false);
        }
    }

    public void onKeyDown() {
        if (!initialized) {
            return;
        }
        if (isActive) return;

        isActive = true;
        SnaplookMod.nativeOnKeyDown();
        updateButtonState(true);
    }

    public void onKeyUp() {
        if (!initialized || !isActive) return;

        isActive = false;
        SnaplookMod.nativeOnKeyUp();
        updateButtonState(false);
    }

    private void updateButtonState(boolean active) {
        float userOpacity = getButtonOpacity();
        if (iconView != null) {
            iconView.setAlpha(userOpacity);
            iconView.setImageResource(active ? R.drawable.ic_snaplook_pressed : R.drawable.ic_snaplook_normal);
        } else if (overlayView instanceof ImageButton) {
            ImageButton btn = (ImageButton) overlayView;
            btn.setAlpha(userOpacity);
            btn.setImageResource(active ? R.drawable.ic_snaplook_pressed : R.drawable.ic_snaplook_normal);
        }
        updateStateLabel();
    }

    @Override
    public void hide() {
        handler.removeCallbacksAndMessages(null);
        if (isActive && initialized) {
            SnaplookMod.nativeOnKeyUp();
            isActive = false;
        }
        iconView = null;
        stateView = null;
        super.hide();
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void applyConfigurationChanges() {
        super.applyConfigurationChanges();
    }
}

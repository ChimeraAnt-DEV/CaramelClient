package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.chimeramc.client.R;
import org.chimeramc.client.core.mods.inbuilt.model.ModIds;
import org.chimeramc.client.databinding.OverlayAutosprintButtonBinding;
import org.chimeramc.client.launcher.core.mods.inbuilt.nativemod.AutoSprintMod;

public class AutoSprintOverlay extends BaseOverlayButton {
    private boolean isActive = false;
    private ImageView sprintIcon;
    private TextView sprintState;

    public AutoSprintOverlay(Activity activity) {
        super(activity);
    }

    @Override
    protected String getModId() {
        return ModIds.AUTO_SPRINT;
    }

    @Override
    protected int getIconResource() {
        return isActive ? R.drawable.ic_sprint_pressed : R.drawable.ic_sprint_normal;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.overlay_autosprint_button;
    }

    @Override
    protected void configureOverlayView(View view) {
        OverlayAutosprintButtonBinding binding = OverlayAutosprintButtonBinding.bind(view);
        sprintIcon = binding.autosprintIcon;
        sprintState = binding.autosprintState;
        updateButtonState(isActive);
    }

    @Override
    protected void onButtonClick() {
        boolean nextState = !isActive;
        if (!AutoSprintMod.setEnabled(nextState)) {
            nextState = false;
        }
        isActive = nextState;
        updateButtonState(isActive);
    }

    /**
     * Drops the module when the game loses focus.
     *
     * <p>The backend's {@code releaseAll} already stops holding forward, but the overlay's own flag
     * would stay armed, so returning to the game would show a "sprint on" indicator for a state the
     * backend has forgotten. Releasing here keeps the two in step.
     */
    public void onFocusLost() {
        if (!isActive) return;
        AutoSprintMod.setEnabled(false);
        isActive = false;
        updateButtonState(false);
    }

    private void updateButtonState(boolean active) {
        float opacity = getButtonOpacity();
        if (sprintIcon != null) {
            sprintIcon.setAlpha(opacity);
            sprintIcon.setImageResource(active ? R.drawable.ic_sprint_pressed : R.drawable.ic_sprint_normal);
        } else if (overlayView instanceof ImageButton) {
            ImageButton btn = (ImageButton) overlayView;
            btn.setAlpha(opacity);
            btn.setImageResource(active ? R.drawable.ic_sprint_pressed : R.drawable.ic_sprint_normal);
        }
        if (sprintState != null) {
            sprintState.setText(active
                    ? R.string.mod_overlay_autosprint_on
                    : R.string.mod_overlay_autosprint_off);
        }
    }

    @Override
    public void hide() {
        if (isActive) {
            AutoSprintMod.setEnabled(false);
            isActive = false;
            updateButtonState(false);
        }
        sprintIcon = null;
        sprintState = null;
        super.hide();
    }
}

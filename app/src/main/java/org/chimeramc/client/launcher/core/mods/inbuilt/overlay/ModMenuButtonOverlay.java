package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.widget.ImageButton;

import org.chimeramc.client.R;
import org.chimeramc.client.core.mods.inbuilt.model.ModIds;

/**
 * The floating button that opens the in-game Mod Menu on a touch screen.
 *
 * <p>The menu itself is drawn natively (Dear ImGui), and until this button existed the only way to
 * open it was the keyboard or controller bind — a phone with neither had no entry point at all, and
 * the "Open the Mod Menu" description had nothing to describe. The glyph is the Caramel apple, the
 * same mark the launcher wears, so the button reads as the client's own.
 */
public class ModMenuButtonOverlay extends BaseOverlayButton {

    public ModMenuButtonOverlay(Activity activity) {
        super(activity);
    }

    @Override
    protected String getModId() {
        return ModIds.MOD_MENU;
    }

    @Override
    protected int getIconResource() {
        return R.drawable.mod_menu_icon;
    }

    @Override
    protected void configureOverlayView(android.view.View view) {
        view.setContentDescription(activity.getString(R.string.mod_menu_open_button));
    }

    @Override
    protected void onButtonClick() {
        org.chimeramc.client.launcher.core.mods.inbuilt.overlay.InGameMenuController menu =
                org.chimeramc.client.launcher.core.mods.inbuilt.overlay.InGameMenuController.getInstance();
        if (menu == null) return;
        menu.toggle();
        setPressedIcon(menu.isMenuOpen());
    }

    private void setPressedIcon(boolean open) {
        if (overlayView instanceof ImageButton) {
            ((ImageButton) overlayView).setAlpha(open ? 0.65f : 1f);
        }
    }
}

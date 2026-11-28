package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.widget.ImageButton;

import org.chimeramc.client.R;
import org.chimeramc.client.core.mods.inbuilt.model.ModIds;
import org.chimeramc.client.launcher.core.mods.inbuilt.overlay.InGameMenuController;

/**
 * The floating button that opens the in-game Mod Menu on a touch screen.
 *
 * <p>The menu itself is drawn natively (Dear ImGui), and until this button existed the only way to
 * open it was the keyboard or controller bind — a phone with neither had no entry point at all, and
 * the "Open the Mod Menu" description had nothing to describe.
 */
public class ModMenuButtonOverlay extends BaseOverlayButton {

    public ModMenuButtonOverlay(Activity activity) {
        super(activity);
    }

    @Override
    protected String getModId() {
        return ModIds.MOD_MENU;
    }

    /**
     * The drawable the floating button wears.
     *
     * <p>A plain three-bar handle, not the client's apple: the button sits on top of the game and
     * every screenshot, and the brand mark there announced the client rather than the menu.
     */
    static int iconResource() {
        return R.drawable.mod_menu_handle;
    }

    @Override
    protected int getIconResource() {
        return iconResource();
    }

    @Override
    protected void configureOverlayView(android.view.View view) {
        view.setContentDescription(activity.getString(R.string.mod_menu_open_button));
    }

    @Override
    protected void onButtonClick() {
        InGameMenuController menu = InGameMenuController.getInstance();
        if (menu == null) {
            // The game activity installs the controller when a session starts; installing here keeps
            // the button working when it is pressed before that (or from another host).
            menu = InGameMenuController.install(activity);
        }
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

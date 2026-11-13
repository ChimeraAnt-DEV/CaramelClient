package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.compose.runtime.MutableState;
import androidx.compose.ui.platform.ComposeView;

import org.chimeramc.client.R;
import org.chimeramc.client.core.mods.inbuilt.UnifiedMod;
import org.chimeramc.client.core.mods.inbuilt.manager.InbuiltModManager;
import org.chimeramc.client.ui.compose.modmenu.ModMenuDestination;
import org.chimeramc.client.ui.compose.modmenu.ModMenuHost;
import org.chimeramc.client.ui.compose.modmenu.ModMenuState;
import org.chimeramc.client.ui.compose.modmenu.ModMenuStateBuilder;
import org.chimeramc.client.util.AppLog;

import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * The Compose presentation of the Mod Menu, layered over the View menu's container.
 *
 * <p>Extracted from {@code ModMenuOverlay} so that class stays inside its size budget. The overlay
 * keeps the View tree, the persistence and the callbacks; this class owns the Compose view, its
 * state, and the mapping from Compose interactions back to the overlay's persistence. That split is
 * also what lets the Compose layer be dropped entirely on a device where it fails to initialise.
 *
 * <p>The Compose tree is added over the already-inflated View tree, so a failure here is caught and
 * logged and the working View menu stays visible rather than a blank window.
 *
 * <p>The package is {@code ...core.mods.inbuilt.overlay} (not the {@code ...launcher.core...} path
 * this file sits under) because that is what the sibling sources declare. A file placed by path with
 * the path-derived package would not be a package-mate of {@code ModMenuOverlay} and could not reach
 * its package-private members.
 */
final class ModMenuScreen {

    /** Called when a module toggle arrives from the Compose UI, so the overlay can persist it. */
    interface ModuleToggle {
        void onToggle(String stableKey, boolean enabled);
    }

    private static final String TAG = "ModMenuScreen";

    private final Activity activity;
    private final View overlayView;
    private final List<UnifiedMod> mods;
    private final Set<String> favoriteKeys;
    private final ModuleToggle moduleToggle;
    private final Runnable onClose;

    private ComposeView composeView;
    private MutableState<ModMenuState> state;
    private boolean attached;

    ModMenuScreen(Activity activity,
                  View overlayView,
                  List<UnifiedMod> mods,
                  Set<String> favoriteKeys,
                  ModuleToggle moduleToggle,
                  Runnable onClose) {
        this.activity = activity;
        this.overlayView = overlayView;
        this.mods = mods;
        this.favoriteKeys = favoriteKeys;
        this.moduleToggle = moduleToggle;
        this.onClose = onClose;
    }

    /** Builds the Compose menu and adds it over the View menu's container. Idempotent. */
    void attach() {
        if (attached || overlayView == null) return;
        attached = true;
        try {
            state = ModMenuHost.state(
                    ModMenuStateBuilder.build(mods, favoriteKeys, ModMenuDestination.MODULES));

            composeView = new ComposeView(activity);
            composeView.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT));

            ModMenuHost.setContent(
                    composeView,
                    state,
                    destination -> update(s -> s.withDestination(destination)),
                    query -> update(s -> s.withSearch(query)),
                    (stableKey, enabled) -> {
                        moduleToggle.onToggle(stableKey, enabled);
                        refresh();
                    },
                    stableKey -> {
                        toggleFavorite(stableKey);
                        refresh();
                    },
                    () -> update(s -> s.withFavoritesOnly(!s.getFavoritesOnly())),
                    (key, enabled) -> {
                        InbuiltModManager.getInstance(activity).setModConfigBool(key, enabled);
                        refresh();
                    },
                    onClose::run);

            FrameLayout host = overlayView.findViewById(R.id.mod_menu_container);
            if (host != null) {
                host.addView(composeView);
            } else if (overlayView instanceof ViewGroup) {
                ((ViewGroup) overlayView).addView(composeView);
            }
        } catch (Throwable e) {
            // Compose failing must never take the menu down: the View menu is already inflated
            // underneath, so dropping the Compose layer leaves a working screen.
            AppLog.warnOnce("CaramelClient", "ModMenuScreen:attach",
                    "The Compose Mod Menu could not start; using the View menu", e);
            composeView = null;
            state = null;
            attached = false;
        }
    }

    /** Rebuilds the rows from the current mod list, preserving the destination and the filters. */
    void refresh() {
        if (state == null) return;
        try {
            final ModMenuState current = state.getValue();
            final ModMenuState rebuilt =
                    ModMenuStateBuilder.build(mods, favoriteKeys, current.getDestination());
            state.setValue(rebuilt
                    .withSearch(current.getSearch())
                    .withFavoritesOnly(current.getFavoritesOnly()));
        } catch (Throwable e) {
            AppLog.warnOnce("CaramelClient", "ModMenuScreen:refresh",
                    "Could not rebuild the Compose menu rows", e);
        }
    }

    private void update(UnaryOperator<ModMenuState> change) {
        if (state == null) return;
        state.setValue(change.apply(state.getValue()));
    }

    private void toggleFavorite(String stableKey) {
        if (stableKey == null) return;
        final boolean nowFavorite = !favoriteKeys.contains(stableKey);
        if (nowFavorite) {
            favoriteKeys.add(stableKey);
        } else {
            favoriteKeys.remove(stableKey);
        }
        InbuiltModManager.getInstance(activity).setModFavorite(stableKey, nowFavorite);
    }
}

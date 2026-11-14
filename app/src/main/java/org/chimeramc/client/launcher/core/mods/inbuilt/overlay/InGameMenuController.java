package org.chimeramc.client.launcher.core.mods.inbuilt.overlay;

import org.chimeramc.client.util.AppLog;

import android.app.Activity;
import android.view.MotionEvent;

import org.chimeramc.client.R;
import org.chimeramc.client.core.mods.inbuilt.ExternalModuleProvider;
import org.chimeramc.client.core.mods.inbuilt.InbuiltModuleProvider;
import org.chimeramc.client.core.mods.inbuilt.UnifiedMod;
import org.chimeramc.client.core.mods.inbuilt.manager.InbuiltModManager;
import org.chimeramc.client.core.mods.inbuilt.model.ModAvailability;
import org.chimeramc.client.core.mods.inbuilt.overlay.ModNotificationManager;
import org.chimeramc.client.preloader.PreloaderMenu;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Drives the native in-game Mod Menu from the launcher side.
 *
 * <p>The menu itself is drawn natively (Dear ImGui, on the game's GL context — see {@link
 * PreloaderMenu}); this class is the bridge. It builds the module catalogue the menu shows, forwards
 * the game view's touch events into it, and applies the interactions the menu raises back onto the
 * real modules. That keeps the native side free of Android types and keeps this side testable.
 *
 * <p>The catalogue and the drained events cross as JSON. {@link #buildCatalogueJson} and {@link
 * #applyEvent} are pure with respect to the native library, so the shape of both documents is
 * pinned by unit tests rather than discovered on a device.
 *
 * <p><b>Honesty.</b> A module that cannot run in this build is sent with its reason and the menu
 * badges it; the menu never shows a toggle that would do nothing. The module list is the same one
 * the launcher shows, from the same providers, so a module cannot exist in one and be missing from
 * the other.
 */
public final class InGameMenuController {

    private static final String TAG = "InGameMenuController";

    private static volatile InGameMenuController instance;

    private final Activity activity;
    private final ModNotificationManager notifications;
    private boolean installed;
    /** The catalogue revision last sent, so an unchanged list is not rebuilt every tick. */
    private int lastCatalogueHash;

    private InGameMenuController(Activity activity) {
        this.activity = activity;
        this.notifications = new ModNotificationManager(activity);
    }

    /** The controller for an activity, or null before one is installed. */
    public static InGameMenuController getInstance() {
        return instance;
    }

    /** Installs the controller for a game session. Idempotent per activity. */
    public static InGameMenuController install(Activity activity) {
        if (activity == null) return null;
        InGameMenuController existing = instance;
        if (existing != null && existing.activity == activity) {
            existing.ensureNativeInstalled();
            return existing;
        }
        InGameMenuController controller = new InGameMenuController(activity);
        instance = controller;
        controller.ensureNativeInstalled();
        controller.registerFonts();
        controller.pushCatalogue();
        return controller;
    }

    /** Drops the controller when the game session ends. */
    public static void release(Activity activity) {
        InGameMenuController current = instance;
        if (current == null) return;
        if (activity != null && current.activity != activity) return;
        PreloaderMenu.setOpen(false);
        instance = null;
    }

    private void ensureNativeInstalled() {
        if (installed) return;
        PreloaderMenu.install();
        installed = true;
    }

    /** True when the native menu's ImGui context is up in the game process. */
    public boolean isMenuLive() {
        return PreloaderMenu.isMenuLive();
    }

    /** Whether the native menu is currently open. */
    public boolean isMenuOpen() {
        return PreloaderMenu.isOpen();
    }

    /** Opens the native menu, refreshing the catalogue first. */
    public void show() {
        if (!PreloaderMenu.isMenuLive()) {
            // The present hook has not installed (or this build has none). The menu cannot draw, so
            // it must not open onto a black screen; say so once instead.
            AppLog.warnOnce(TAG, "show", "The in-game menu is not live on this build", null);
            return;
        }
        pushCatalogue();
        PreloaderMenu.setOpen(true);
    }

    /** Closes the native menu. */
    public void hide() {
        PreloaderMenu.setOpen(false);
    }

    /** Opens the menu when it is closed, closes it when it is open. */
    public void toggle() {
        if (isMenuOpen()) {
            hide();
        } else {
            show();
        }
    }

    /**
     * Forwards a touch event to the native menu.
     *
     * @return true when the menu is open and therefore consuming the touch, so the game must not
     *     also see it.
     */
    public boolean handleTouch(MotionEvent event) {
        if (event == null || !isMenuOpen()) return false;
        final float x = event.getX();
        final float y = event.getY();
        // The game view's size is the surface the menu draws into, so the coordinate maps 1:1. The
        // view size is passed so the native side can scale if it ever differs (a letterboxed surface).
        PreloaderMenu.pushTouch(event.getActionMasked(), x, y, 0f, 0f);
        return true;
    }

    /** Forwards a back/close request (the back button or a controller's B). */
    public boolean handleBack() {
        if (!isMenuOpen()) return false;
        PreloaderMenu.pushBack();
        return true;
    }

    /**
     * Drains the interactions the menu raised and applies them to the real modules.
     *
     * <p>Called from the per-frame tick, on the main thread, so the module changes land on the same
     * thread the launcher's own toggles use.
     */
    public void drainAndApply() {
        if (!installed) return;
        final String json = PreloaderMenu.drainEvents();
        if (json == null || json.isEmpty() || "[]".equals(json)) return;
        boolean changed = false;
        try {
            final JSONArray events = new JSONArray(json);
            for (int i = 0; i < events.length(); i++) {
                changed |= applyEvent(events.getJSONObject(i));
            }
        } catch (Exception e) {
            AppLog.warnOnce(TAG, "drain", "Ignored a malformed menu event batch", e);
            return;
        }
        if (changed) pushCatalogue();
    }

    /**
     * Applies one menu event. Returns true when the module list changed and must be re-sent.
     *
     * <p>Package-private and free of Android view code so the mapping from a menu event to a module
     * change is unit-tested.
     */
    boolean applyEvent(JSONObject event) {
        final String kind = event.optString("kind", "");
        final String key = event.optString("key", "");
        final String value = event.optString("value", "");
        if (key.isEmpty()) return false;
        final UnifiedMod mod = findMod(key);
        if (mod == null) return false;
        switch (kind) {
            case "toggle_module":
                if (!ModAvailability.isInteractive(mod.getId())) return false;
                mod.applyEnabled("true".equals(value));
                InbuiltModManager manager = InbuiltModManager.getInstance(activity);
                if (manager != null && mod.isEnabled() && manager.isNotificationsEnabled()) {
                    notifications.show(mod.getName(), mod.getStableKey());
                }
                return true;
            case "set_config":
                return applyConfig(mod, value);
            case "toggle_favorite":
                return toggleFavorite(mod);
            case "close":
                return false;
            default:
                return false;
        }
    }

    private boolean applyConfig(UnifiedMod mod, String keyValue) {
        final int sep = keyValue.indexOf('=');
        if (sep <= 0) return false;
        final String configKey = keyValue.substring(0, sep);
        final String configValue = keyValue.substring(sep + 1);
        mod.updateConfig(configKey, configValue);
        return false;
    }

    private boolean toggleFavorite(UnifiedMod mod) {
        InbuiltModManager manager = InbuiltModManager.getInstance(activity);
        if (manager == null) return false;
        final boolean nowFavorite = !manager.getFavoriteModKeys().contains(mod.getStableKey());
        manager.setModFavorite(mod.getStableKey(), nowFavorite);
        return true;
    }

    private UnifiedMod findMod(String stableKey) {
        for (UnifiedMod mod : loadMods()) {
            if (mod.getStableKey().equals(stableKey)) return mod;
        }
        return null;
    }

    private List<UnifiedMod> loadMods() {
        List<UnifiedMod> mods = new ArrayList<>(InbuiltModuleProvider.load(activity));
        mods.addAll(ExternalModuleProvider.load(activity));
        return mods;
    }

    /** Rebuilds and sends the catalogue, unless it is unchanged since the last send. */
    public void pushCatalogue() {
        if (!installed) return;
        final List<UnifiedMod> mods = loadMods();
        final Set<String> favorites = favoriteKeys();
        final String json = buildCatalogueJson(mods, favorites, emptyReason(), this::reasonMessage);
        final int hash = json.hashCode();
        if (hash == lastCatalogueHash) return;
        lastCatalogueHash = hash;
        PreloaderMenu.setCatalogue(json);
    }

    private Set<String> favoriteKeys() {
        InbuiltModManager manager = InbuiltModManager.getInstance(activity);
        return manager == null ? java.util.Collections.emptySet() : manager.getFavoriteModKeys();
    }

    private String emptyReason() {
        return activity.getString(R.string.mod_menu_no_mods);
    }

    /**
     * The wording for a module's unavailable reason.
     *
     * <p>Every reason has its own message: a module waiting for an engine hook, one waiting for a
     * game feed this build cannot read, one a server decides, and one waiting on a backend feature
     * are four different situations with four different answers. A generic "unavailable" tells the
     * player none of them.
     */
    private String reasonMessage(String reasonId) {
        int res = reasonStringRes(reasonId);
        return res == 0 ? "" : activity.getString(res);
    }

    /** The string resource that words a reason id, or 0 when the id is unknown. */
    static int reasonStringRes(String reasonId) {
        if (ModAvailability.REASON_SERVER_DECIDED.equals(reasonId)) {
            return R.string.mod_unavailable_reason_server;
        }
        if (ModAvailability.REASON_NO_BACKEND.equals(reasonId)) {
            return R.string.mod_unavailable_reason_backend;
        }
        if (ModAvailability.REASON_NO_HOOK.equals(reasonId)) {
            return R.string.mod_unavailable_reason_hook;
        }
        if (ModAvailability.REASON_NO_GAME_DATA.equals(reasonId)) {
            return R.string.mod_unavailable_reason;
        }
        return 0;
    }

    /**
     * Builds the catalogue document the native menu reads.
     *
     * <p>Static and free of Android types other than the strings it is handed, so the shape is
     * unit-tested against the exact fields the native parser reads. The reason is already worded by
     * {@code messageFor}, so the native menu never shows a raw reason id.
     */
    static String buildCatalogueJson(List<UnifiedMod> mods, Set<String> favorites,
                                     String emptyReason,
                                     java.util.function.Function<String, String> messageFor) {
        try {
            JSONArray array = new JSONArray();
            for (UnifiedMod mod : mods) {
                array.put(moduleJson(mod, favorites, messageFor));
            }
            JSONObject root = new JSONObject();
            root.put("modules", array);
            root.put("empty_reason", emptyReason == null ? "" : emptyReason);
            return root.toString();
        } catch (Exception e) {
            return "{\"modules\":[],\"empty_reason\":\"\"}";
        }
    }

    private static JSONObject moduleJson(UnifiedMod mod, Set<String> favorites,
                                         java.util.function.Function<String, String> messageFor)
            throws Exception {
        JSONObject obj = new JSONObject();
        obj.put("stable_key", mod.getStableKey());
        obj.put("name", mod.getName());
        obj.put("description", mod.getDescription());
        obj.put("group", mod.getGroupName());
        obj.put("enabled", mod.isEnabled());
        obj.put("favorite", favorites != null && favorites.contains(mod.getStableKey()));
        obj.put("has_config", mod.hasConfig());
        String reason = ModAvailability.unavailableReason(mod.getId());
        String message = reason == null ? "" : messageFor.apply(reason);
        obj.put("unavailable", message == null ? "" : message);
        obj.put("configs", configsJson(mod));
        return obj;
    }

    private static JSONArray configsJson(UnifiedMod mod) throws Exception {
        JSONArray configs = new JSONArray();
        for (UnifiedMod.ConfigEntry entry : mod.getConfigEntries()) {
            JSONObject cfg = new JSONObject();
            cfg.put("key", entry.key);
            cfg.put("label", entry.displayName);
            cfg.put("value", entry.currentValue == null ? "" : entry.currentValue);
            switch (entry.type) {
                case TOGGLE:
                    cfg.put("kind", "toggle");
                    cfg.put("float_value", "true".equalsIgnoreCase(entry.currentValue) ? 1.0 : 0.0);
                    break;
                case SLIDER_INT:
                case SLIDER_FLOAT:
                    cfg.put("kind", "slider");
                    cfg.put("float_value", parseFloat(entry.currentValue, 0f));
                    cfg.put("float_min", parseFloat(entry.minValue, 0f));
                    cfg.put("float_max", parseFloat(entry.maxValue, 1f));
                    break;
                default:
                    // A colour, a keybind or a text field has no in-game editor yet; it is shown
                    // read-only with its value rather than dropped from the panel.
                    cfg.put("kind", "readonly");
                    break;
            }
            configs.put(cfg);
        }
        return configs;
    }

    private static float parseFloat(String value, float fallback) {
        if (value == null || value.isEmpty()) return fallback;
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void registerFonts() {
        // The pack's roles. Inter for body text, Poppins for titles; the launcher already ships both.
        registerFont(0, R.font.inter);
        registerFont(1, R.font.inter);
        registerFont(2, R.font.inter);
        registerFont(3, R.font.poppins);
        registerFont(4, R.font.poppins);
    }

    private void registerFont(int role, int fontRes) {
        try {
            byte[] bytes = readResource(fontRes);
            if (bytes != null) PreloaderMenu.registerFont(role, bytes);
        } catch (Exception e) {
            AppLog.warnOnce(TAG, "font", "A menu font could not be read; the default is used", e);
        }
    }

    private byte[] readResource(int resId) {
        try (java.io.InputStream in = activity.getResources().openRawResource(resId)) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }
}

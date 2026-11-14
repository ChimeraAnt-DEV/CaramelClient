package org.chimeramc.client.preloader;

import org.chimeramc.client.util.AppLog;

/**
 * The in-game Mod Menu, drawn natively with Dear ImGui on the game's own GL context.
 *
 * <p>This is the client half of the menu the UI pack ships as {@code imgui/src/CaramelMenu.cpp}
 * (Lynxifyy/CaramelClient-UI-pack). The pack's desktop GLFW backend has no Android path, so the
 * menu is rendered from the sweetloader's present hook instead, where the game's GLES context is
 * current. The layout, glass widget kit and tokens are the pack's; the module data is this client's
 * real catalogue.
 *
 * <p><b>Fail-closed.</b> Every entry point is a no-op when the sweetloader is absent or its present
 * hook is not live, and {@link #isMenuLive()} then reports false, so the launcher can badge the menu
 * rather than open a menu that cannot draw.
 */
public final class PreloaderMenu {

    private static final String TAG = "PreloaderMenu";

    private PreloaderMenu() {
    }

    /** Asks the sweetloader to prepare the menu. Safe to call repeatedly. */
    public static void install() {
        try {
            nativeInstallMenu();
        } catch (UnsatisfiedLinkError e) {
            AppLog.warnOnce(TAG, "PreloaderMenu:install", "The sweetloader is not loaded", e);
        }
    }

    /** True once the menu's ImGui context and GLES backend are up in the game process. */
    public static boolean isMenuLive() {
        try {
            return nativeIsMenuLive();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    /** Opens or closes the menu. */
    public static void setOpen(boolean open) {
        try {
            nativeSetMenuOpen(open);
        } catch (UnsatisfiedLinkError e) {
            // The menu cannot exist without the library; a failure to open or close it is worth a
            // line in the log, not silence.
            AppLog.warnOnce(TAG, "PreloaderMenu:setOpen", "The sweetloader is not loaded", e);
        }
    }

    public static boolean isOpen() {
        try {
            return nativeIsMenuOpen();
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    /**
     * Forwards a touch event to the menu.
     *
     * @param action   the {@code MotionEvent} action mask (0 = down, 1 = up, 2 = move, 3 = cancel).
     * @param x        the touch x in the view's coordinate space.
     * @param y        the touch y in the view's coordinate space.
     * @param viewWidth  the view width the coordinate is in, or 0 to skip scaling.
     * @param viewHeight the view height the coordinate is in, or 0 to skip scaling.
     */
    public static void pushTouch(int action, float x, float y, float viewWidth, float viewHeight) {
        try {
            nativePushTouch(action, x, y, viewWidth, viewHeight);
        } catch (UnsatisfiedLinkError e) {
            AppLog.warnOnce(TAG, "PreloaderMenu:pushTouch", "The sweetloader is not loaded", e);
        }
    }

    /** Reports a back/close request (the back button or a controller's B). */
    public static void pushBack() {
        try {
            nativePushBack();
        } catch (UnsatisfiedLinkError e) {
            AppLog.warnOnce(TAG, "PreloaderMenu:pushBack", "The sweetloader is not loaded", e);
        }
    }

    /**
     * Replaces the module catalogue the Mods tab shows.
     *
     * @param json a {@code {"modules":[...],"empty_reason":"..."}} document, built by
     *             {@code InGameMenuCatalog}.
     * @return true when the catalogue was accepted; false when it was rejected as malformed.
     */
    public static boolean setCatalogue(String json) {
        try {
            return nativeSetCatalogue(json);
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    /**
     * Drains the interactions the menu raised since the last call, as a JSON array of
     * {@code {"kind":...,"key":...,"value":...}} objects.
     */
    public static String drainEvents() {
        try {
            return nativeDrainEvents();
        } catch (UnsatisfiedLinkError e) {
            return "[]";
        }
    }

    /** Registers a font face for a menu role (see {@code InGameMenuCatalog.ROLE_*}). */
    public static boolean registerFont(int role, byte[] data) {
        try {
            return nativeRegisterFont(role, data);
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    private static native void nativeInstallMenu();

    private static native boolean nativeIsMenuLive();

    private static native void nativeSetMenuOpen(boolean open);

    private static native boolean nativeIsMenuOpen();

    private static native void nativePushTouch(int action, float x, float y, float viewWidth,
                                               float viewHeight);

    private static native void nativePushBack();

    private static native boolean nativeSetCatalogue(String json);

    private static native String nativeDrainEvents();

    private static native boolean nativeRegisterFont(int role, byte[] data);
}

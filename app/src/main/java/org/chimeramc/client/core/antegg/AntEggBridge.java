package org.chimeramc.client.core.antegg;

import org.chimeramc.client.core.mods.ModManager;

/**
 * JNI surface for the native .AntEgg loader.
 *
 * <p>The launcher UI can import an .AntEgg while no game session is running, in which case the
 * pure-Java path in {@link AntEggLoader} handles it. This bridge exists for the two things the
 * native side must own: validating a package during a launch (before the JVM has finished coming
 * up) and extracting at a point where the process is already holding the preloader. Every method
 * degrades to the Java implementation when the preloader is not loaded, so an import never fails
 * merely because a game was not running.
 *
 * <p>Each native call returns an empty string on success and a message on failure, matching the
 * style of the rest of the preloader JNI bridge (see {@code ExternalModBridge}).
 */
public final class AntEggBridge {

    private AntEggBridge() {}

    private static native String nativeValidate(String filePath);

    /**
     * Extracts a package. {@code allowedRoot} is the app's private directory the sandbox root must
     * live under; the native side re-checks it, so a wrong root is refused on both sides.
     */
    private static native String nativeLoadMod(String filePath, String sandboxRoot,
                                               String allowedRoot);

    private static native boolean nativeLooksLikeAntEgg(String fileName);

    /** True when the native loader is reachable in this process. */
    public static boolean isNativeAvailable() {
        return ModManager.ensurePreloaderLoaded();
    }

    /**
     * Validates a package, preferring the native parser and falling back to Java.
     *
     * @return null when the package is valid, otherwise a reason to show the user
     */
    public static String validate(String filePath) {
        if (isNativeAvailable()) {
            try {
                String error = nativeValidate(filePath);
                if (error != null && !error.isEmpty()) {
                    return error;
                }
                return null;
            } catch (UnsatisfiedLinkError e) {
                // The library loaded but this symbol is missing (an older build); the Java
                // path is equivalent, so fall through rather than failing the import.
            }
        }
        AntEggPackage.Result result = AntEggPackage.inspect(new java.io.File(filePath));
        return result.isValid() ? null : result.error;
    }

    /**
     * Extracts a package natively when possible; the caller decides how to load the entry point.
     *
     * @return null when extraction succeeded, otherwise a reason
     */
    public static String extract(String filePath, String sandboxRoot) {
        java.io.File sandbox = new java.io.File(sandboxRoot);
        // The sandbox root is <private-storage>/AntEggs, so its parent is the app's private
        // directory — the root the extraction must stay inside. Derived rather than passed so this
        // bridge needs no Context; the native side re-checks the same containment before writing.
        java.io.File allowedRoot = sandbox.getParentFile();
        String localRefusal = AntEggSandbox.describeRefusal(sandbox, allowedRoot);
        if (localRefusal != null) {
            return localRefusal;
        }
        if (isNativeAvailable()) {
            try {
                String error = nativeLoadMod(filePath, sandboxRoot,
                        allowedRoot == null ? "" : allowedRoot.getAbsolutePath());
                if (error != null && !error.isEmpty()) {
                    return error;
                }
                return null;
            } catch (UnsatisfiedLinkError e) {
                // fall through to Java
            }
        }
        AntEggPackage.Result result = AntEggPackage.extract(new java.io.File(filePath), sandbox);
        return result.isValid() ? null : result.error;
    }

    /** Keeps the Java extension check as the authority when the native symbol is absent. */
    public static boolean looksLikeAntEgg(String fileName) {
        if (isNativeAvailable()) {
            try {
                return nativeLooksLikeAntEgg(fileName);
            } catch (UnsatisfiedLinkError e) {
                // fall through
            }
        }
        return AntEggPackage.looksLikeAntEgg(fileName);
    }
}

package org.chimeramc.client.preloader;

/**
 * Maps a native library into the process without running its {@code JNI_OnLoad}.
 *
 * <p>{@code System.load} is the only way Java can open a library, and it always runs
 * {@code JNI_OnLoad}. 1.26.60's {@code libminecraftpe.so} links {@code libpairipcore.so}, whose
 * {@code JNI_OnLoad} refuses to initialise outside the game's own process ("Bad JNI version"), so
 * that dependency has to be mapped by the linker instead of being opened as a library in its own
 * right. That is what {@link #dlopenGlobal(String)} does.
 */
public final class NativeMapper {

    private NativeMapper() {
    }

    /**
     * {@code dlopen(path, RTLD_NOW | RTLD_GLOBAL)} in the launcher's own native namespace, so a
     * later {@code System.load} in that namespace resolves the file as a dependency.
     *
     * @param absolutePath the library to map
     * @return null when it mapped, or the linker's error message
     */
    public static String dlopenGlobal(String absolutePath) {
        System.loadLibrary("sweetloader");
        return nativeDlopenGlobal(absolutePath);
    }

    private static native String nativeDlopenGlobal(String absolutePath);
}

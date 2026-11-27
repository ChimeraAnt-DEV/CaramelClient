package org.chimeramc.client.core.minecraft;

import android.util.Log;

import org.chimeramc.client.preloader.NativeMapper;

import java.io.File;

/**
 * Opens one extracted game library, mapping any dependency the launcher cannot open itself.
 *
 * <p>1.26.60's {@code libminecraftpe.so} names {@code libpairipcore.so} in {@code DT_NEEDED}. That
 * file cannot be opened as a library in its own right: its {@code JNI_OnLoad} aborts outside the
 * game's own process ("Bad JNI version"), and {@code System.load} always runs that hook. It is
 * therefore mapped with a raw {@code dlopen} first, so the game library finds it as a dependency.
 */
public final class GameLibraryOpener {

    private static final String TAG = "GamePackageManager";
    private static final String GAME_LIBRARY = "libminecraftpe.so";
    private static final String PAIRIP_LIBRARY = "libpairipcore.so";

    private GameLibraryOpener() {
    }

    /**
     * Loads {@code library} into the process.
     *
     * @param library     the extracted library file
     * @param nativeDir   the directory the game's libraries were extracted to
     * @param versionCode the instance's version code, for the dependency gate
     */
    public static void open(File library, File nativeDir, String versionCode) {
        if (GAME_LIBRARY.equals(library.getName())
                && NativeLibraryVersionGate.shouldLoadPairIp(versionCode)) {
            mapDependency(new File(nativeDir, PAIRIP_LIBRARY));
        }
        System.load(library.getAbsolutePath());
    }

    /**
     * Maps the PairIP dependency before any bundle library is opened.
     *
     * <p>Only the game library's own {@link #open} used to map it, which is too late: the bundle
     * loader opens the other libraries first (fmod, mcfix, ...), and those are PairIP-protected too.
     * Their {@code JNI_OnLoad} then returns a version the VM rejects ("Bad JNI version ... : 28")
     * and the launch fails on a fresh install, where the extracted directory has no cached mapping
     * yet. Idempotent: a second {@code dlopen} of the same file returns the same handle.
     *
     * @param nativeDir   the directory the game's libraries were extracted to
     * @param versionCode the instance's version code, for the dependency gate
     * @return null when nothing had to be mapped or it mapped, or the failure to report
     */
    public static String mapPairIpIfNeeded(File nativeDir, String versionCode) {
        if (nativeDir == null || !NativeLibraryVersionGate.shouldLoadPairIp(versionCode)) return null;
        File dependency = new File(nativeDir, PAIRIP_LIBRARY);
        if (!dependency.isFile() || dependency.length() <= 0L) return null;
        String error = NativeMapper.dlopenGlobal(dependency.getAbsolutePath());
        if (error != null) return "Failed to map " + PAIRIP_LIBRARY + ": " + error;
        Log.i(TAG, "Mapped " + PAIRIP_LIBRARY + " before the bundle");
        return null;
    }

    private static void mapDependency(File dependency) {
        if (!dependency.isFile() || dependency.length() <= 0L) {
            throw new IllegalStateException(dependency.getName() + " missing from "
                    + dependency.getParent());
        }
        String error = NativeMapper.dlopenGlobal(dependency.getAbsolutePath());
        if (error != null) {
            throw new IllegalStateException("Failed to map " + dependency.getName() + ": " + error);
        }
        Log.i(TAG, "Mapped " + dependency.getName() + " before the game library");
    }
}

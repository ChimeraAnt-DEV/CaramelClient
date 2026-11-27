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

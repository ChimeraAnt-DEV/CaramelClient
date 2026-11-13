package org.chimeramc.client.universe;

import org.chimeramc.client.util.AppLog;

import java.io.File;

/**
 * Java handle for the {@code .universe} loader implemented by {@code libcaramel_universe.so}, the
 * Rust cdylib built from the sweetloader's {@code rust/universe-jni} crate.
 *
 * <p>This class is a thin marshalling shell. Every decision — manifest parsing, Ed25519 signature
 * verification, Zip-Slip-safe extraction, load ordering, permission checks — lives in the Rust
 * {@code universe-core} crate, where it is unit-tested on the build host. Each fallible call returns
 * a message string and signals failure with a non-empty string; an empty string is success. That is
 * the same contract the C++ loaders use, so callers do not need a second error shape.
 *
 * <p>The library is loaded lazily and every entry point is guarded with {@link UnsatisfiedLinkError}
 * so a build that ships without the Rust cdylib keeps working — the loader then simply reports
 * "unavailable" rather than crashing the launcher.
 */
public final class UniverseLoader {

    private static final String TAG = "UniverseLoader";

    /** The loader log tag; also the Rust library's log tag. */
    public static final String LOG_TAG = "SweetLoader";

    private static boolean attempted;
    private static boolean loaded;

    private UniverseLoader() {
    }

    /** Loads {@code libcaramel_universe.so} once; returns whether it is available. */
    public static synchronized boolean ensureLoaded() {
        if (loaded) return true;
        if (attempted) return false;
        attempted = true;
        try {
            System.loadLibrary("caramel_universe");
            loaded = true;
        } catch (UnsatisfiedLinkError e) {
            AppLog.warn(TAG, "libcaramel_universe.so is not available", e);
        }
        return loaded;
    }

    /** True when the Rust loader is present and its JNI surface resolved. */
    public static boolean isAvailable() {
        return ensureLoaded();
    }

    /**
     * Hands the Rust loader the BerryAPI API table pointer the C++ bridge already resolved.
     *
     * <p>A {@code null} pointer is not an error: the loader still runs and only the BerryAPI-backed
     * features report unavailable.
     *
     * @return an empty string on success, or a human-readable refusal reason.
     */
    public static String init(long berryApiPointer) {
        if (!ensureLoaded()) return "the .universe loader library is not available";
        try {
            return nativeInit(berryApiPointer);
        } catch (UnsatisfiedLinkError e) {
            return "the .universe loader library is not available";
        }
    }

    /** Validates a package's manifest and its {@code min_client_version} gate. */
    public static String validate(File packageFile) {
        if (!ensureLoaded()) return "the .universe loader library is not available";
        if (packageFile == null) return "no package file";
        try {
            return nativeValidate(packageFile.getAbsolutePath());
        } catch (UnsatisfiedLinkError e) {
            return "the .universe loader library is not available";
        }
    }

    /**
     * Extracts a package into {@code sandboxRoot}, which must sit inside {@code allowedRoot}.
     *
     * <p>The Rust side refuses any entry whose path would escape the sandbox (Zip Slip) and refuses
     * a {@code sandboxRoot} that is not contained by {@code allowedRoot}.
     */
    public static String extract(File packageFile, File sandboxRoot, File allowedRoot) {
        if (!ensureLoaded()) return "the .universe loader library is not available";
        if (packageFile == null || sandboxRoot == null || allowedRoot == null) {
            return "no package file or sandbox root";
        }
        try {
            return nativeExtract(packageFile.getAbsolutePath(), sandboxRoot.getAbsolutePath(),
                    allowedRoot.getAbsolutePath());
        } catch (UnsatisfiedLinkError e) {
            return "the .universe loader library is not available";
        }
    }

    /** Whether {@code fileName} looks like a {@code .universe} package (by extension or manifest). */
    public static boolean looksLikeUniverse(String fileName) {
        if (!ensureLoaded()) return false;
        try {
            return nativeLooksLikeUniverse(fileName);
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    /** Puts the loader into (or clears) its cancelled state; mods poll it between frames. */
    public static void setCancelled(boolean cancelled) {
        if (!ensureLoaded()) return;
        try {
            nativeSetCancelled(cancelled);
        } catch (UnsatisfiedLinkError e) {
            AppLog.warn(TAG, "nativeSetCancelled is not available", e);
        }
    }

    /** Reads a package's manifest as JSON. */
    public static String readManifest(File packageFile) {
        if (!ensureLoaded()) return "the .universe loader library is not available";
        if (packageFile == null) return "no package file";
        try {
            return nativeReadManifest(packageFile.getAbsolutePath());
        } catch (UnsatisfiedLinkError e) {
            return "the .universe loader library is not available";
        }
    }

    /** Checks a manifest's required {@code min_client_version} against this build's own version. */
    public static String checkVersion(String minClientVersion) {
        if (!ensureLoaded()) return "the .universe loader library is not available";
        try {
            return nativeCheckVersion(minClientVersion == null ? "" : minClientVersion);
        } catch (UnsatisfiedLinkError e) {
            return "the .universe loader library is not available";
        }
    }

    /** The loader's own client version string, or an empty string when unavailable. */
    public static String clientVersion() {
        if (!ensureLoaded()) return "";
        try {
            return nativeClientVersion();
        } catch (UnsatisfiedLinkError e) {
            return "";
        }
    }

    private static native String nativeInit(long berryApiPointer);

    private static native String nativeValidate(String filePath);

    private static native String nativeExtract(String filePath, String sandboxRoot,
            String allowedRoot);

    private static native boolean nativeLooksLikeUniverse(String fileName);

    private static native void nativeSetCancelled(boolean cancelled);

    private static native String nativeReadManifest(String filePath);

    private static native String nativeCheckVersion(String minClientVersion);

    private static native String nativeClientVersion();
}

package org.chimeramc.client.singularity;

import org.chimeramc.client.util.AppLog;

import java.io.File;

/**
 * Java handle for the {@code .singularity} world-gen engine implemented by
 * {@code libcaramel_singularity.so}, the Rust cdylib built from the sweetloader's
 * {@code rust/singularity-jni} crate.
 *
 * <p>Same shape as {@link org.chimeramc.client.universe.UniverseLoader}: the decisions (manifest
 * parsing, signature verification, sandboxed extraction, version gating) live in the Rust cores,
 * and this class only marshals arguments and returns a message string per call. An empty string is
 * success, a non-empty string is the refusal reason. The engine reaches the game through BerryAPI,
 * whose C++ bridge owns the chunk hook and the render-context handoff.
 *
 * <p>Loaded lazily and guarded, so a build without the Rust cdylib reports "unavailable" instead of
 * failing.
 */
public final class SingularityEngine {

    private static final String TAG = "SingularityEngine";

    /** The engine log tag; also the Rust library's log tag. */
    public static final String LOG_TAG = "SweetLoader.Singularity";

    private static boolean attempted;
    private static boolean loaded;

    private SingularityEngine() {
    }

    /** Loads {@code libcaramel_singularity.so} once; returns whether it is available. */
    public static synchronized boolean ensureLoaded() {
        if (loaded) return true;
        if (attempted) return false;
        attempted = true;
        try {
            System.loadLibrary("caramel_singularity");
            loaded = true;
        } catch (UnsatisfiedLinkError e) {
            AppLog.warn(TAG, "libcaramel_singularity.so is not available", e);
        }
        return loaded;
    }

    /** True when the Rust engine is present and its JNI surface resolved. */
    public static boolean isAvailable() {
        return ensureLoaded();
    }

    /**
     * Hands the engine the BerryAPI API table pointer so it can generate chunks against the live
     * world. A {@code null} pointer is not an error; the engine then reports the BerryAPI-backed
     * features unavailable.
     *
     * @return an empty string on success, or a refusal reason.
     */
    public static String init(long berryApiPointer) {
        if (!ensureLoaded()) return "the .singularity engine library is not available";
        try {
            return nativeInit(berryApiPointer);
        } catch (UnsatisfiedLinkError e) {
            return "the .singularity engine library is not available";
        }
    }

    /** Validates a {@code .singularity} package's manifest and its {@code min_client_version}. */
    public static String validate(File packageFile) {
        if (!ensureLoaded()) return "the .singularity engine library is not available";
        if (packageFile == null) return "no package file";
        try {
            return nativeValidate(packageFile.getAbsolutePath());
        } catch (UnsatisfiedLinkError e) {
            return "the .singularity engine library is not available";
        }
    }

    /** Extracts a package into {@code sandboxRoot}, which must sit inside {@code allowedRoot}. */
    public static String extract(File packageFile, File sandboxRoot, File allowedRoot) {
        if (!ensureLoaded()) return "the .singularity engine library is not available";
        if (packageFile == null || sandboxRoot == null || allowedRoot == null) {
            return "no package file or sandbox root";
        }
        try {
            return nativeExtract(packageFile.getAbsolutePath(), sandboxRoot.getAbsolutePath(),
                    allowedRoot.getAbsolutePath());
        } catch (UnsatisfiedLinkError e) {
            return "the .singularity engine library is not available";
        }
    }

    /** Whether {@code fileName} looks like a {@code .singularity} package. */
    public static boolean looksLikeSingularity(String fileName) {
        if (!ensureLoaded()) return false;
        try {
            return nativeLooksLikeSingularity(fileName);
        } catch (UnsatisfiedLinkError e) {
            return false;
        }
    }

    /** Puts the engine into (or clears) its cancelled state; chunk generation polls it. */
    public static void setCancelled(boolean cancelled) {
        if (!ensureLoaded()) return;
        try {
            nativeSetCancelled(cancelled);
        } catch (UnsatisfiedLinkError e) {
            AppLog.warn(TAG, "nativeSetCancelled is not available", e);
        }
    }

    /** Parses a manifest JSON string; returns the normalised JSON or a refusal reason. */
    public static String parseManifest(String manifestJson) {
        if (!ensureLoaded()) return "the .singularity engine library is not available";
        try {
            return nativeParseManifest(manifestJson == null ? "" : manifestJson);
        } catch (UnsatisfiedLinkError e) {
            return "the .singularity engine library is not available";
        }
    }

    private static native String nativeInit(long berryApiPointer);

    private static native String nativeValidate(String filePath);

    private static native String nativeExtract(String filePath, String sandboxRoot,
            String allowedRoot);

    private static native boolean nativeLooksLikeSingularity(String fileName);

    private static native void nativeSetCancelled(boolean cancelled);

    private static native String nativeParseManifest(String manifestJson);
}

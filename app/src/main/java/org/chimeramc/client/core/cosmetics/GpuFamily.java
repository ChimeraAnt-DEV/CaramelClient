package org.chimeramc.client.core.cosmetics;

/**
 * The GPU vendor family the device reports, classified from the GL vendor/renderer strings.
 *
 * <p>A backend name is not enough to choose safe defaults: an Adreno, a Mali and a software
 * rasteriser all report {@code GLES}, and they differ in texture-size limits and precision quirks.
 * The native probe reads {@code glGetString(GL_VENDOR/GL_RENDERER)} once, classifies it (the pure
 * {@code pl::render::ClassifyGpuFamily}), and hands the family up through the diagnostics array.
 *
 * <p>This mirrors that enum so the report and the debug lines share one label, exactly as
 * {@link RenderBackend} does for the backend. It is pure so the wording is pinned without a device.
 */
public final class GpuFamily {

    /** The family, mirroring the native {@code pl::render::GpuFamily} enum. */
    public enum Kind {
        /** No vendor/renderer string was read, or one we cannot place. */
        UNKNOWN,
        /** Qualcomm Adreno. */
        ADRENO,
        /** Arm Mali. */
        MALI,
        /** Arm Immortalis (a Mali-tier part reported separately). */
        IMMORTALIS,
        /** Imagination PowerVR. */
        POWERVR,
        /** Samsung Xclipse (AMD RDNA-based). */
        XCLIPSE,
        /** NVIDIA (Tegra and desktop). */
        NVIDIA,
        /** Apple GPU (a macOS/CI or emulated host). */
        APPLE,
        /** SwiftShader / llvmpipe / a software rasteriser. */
        SOFTWARE,
        /** A real GPU we cannot name. */
        GENERIC,
    }

    private GpuFamily() {
    }

    /**
     * Maps the native {@code GpuFamily} reading to a {@link Kind}.
     *
     * @param nativeCode 0 unknown, 1 Adreno, 2 Mali, 3 Immortalis, 4 PowerVR, 5 Xclipse, 6 NVIDIA,
     *                   7 Apple, 8 Software, 9 Generic; anything else (including -1, "not read")
     *                   reads as {@link Kind#UNKNOWN}
     */
    public static Kind fromNativeCode(int nativeCode) {
        switch (nativeCode) {
            case 1: return Kind.ADRENO;
            case 2: return Kind.MALI;
            case 3: return Kind.IMMORTALIS;
            case 4: return Kind.POWERVR;
            case 5: return Kind.XCLIPSE;
            case 6: return Kind.NVIDIA;
            case 7: return Kind.APPLE;
            case 8: return Kind.SOFTWARE;
            case 9: return Kind.GENERIC;
            default: return Kind.UNKNOWN;
        }
    }

    /** The short label for the family, as shown in the diagnostics report. */
    public static String label(Kind kind) {
        switch (kind) {
            case ADRENO: return "Adreno";
            case MALI: return "Mali";
            case IMMORTALIS: return "Immortalis";
            case POWERVR: return "PowerVR";
            case XCLIPSE: return "Xclipse";
            case NVIDIA: return "NVIDIA";
            case APPLE: return "Apple";
            case SOFTWARE: return "software rasteriser";
            case GENERIC: return "generic GPU";
            default: return "unknown";
        }
    }

    /** True for a software rasteriser, where every draw is expensive and the cheapest path wins. */
    public static boolean isSoftware(Kind kind) {
        return kind == Kind.SOFTWARE;
    }
}

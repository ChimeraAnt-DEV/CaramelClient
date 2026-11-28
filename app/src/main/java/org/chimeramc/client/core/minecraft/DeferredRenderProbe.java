package org.chimeramc.client.core.minecraft;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import org.chimeramc.client.util.AppLog;

import java.lang.reflect.Method;

/**
 * Reads the device facts {@link DeferredRenderSupport} needs, on Android.
 *
 * <p>Kept apart from the pure rule so the decision stays unit-testable: this class only gathers the
 * readings (Vulkan presence, GLES version, whether the renderer is a software rasteriser) and hands
 * them to {@link DeferredRenderSupport#supported}. A reading that throws — an old ROM with no Vulkan
 * feature constant, a missing GLES version — reads as unsupported, which hides the Vibrant Visuals
 * option rather than advertising one the device cannot run.
 */
public final class DeferredRenderProbe {

    private static final String TAG = "DeferredRenderProbe";

    /** The feature string the platform declares when a Vulkan driver is present. */
    private static final String FEATURE_VULKAN_VERSION = "android.hardware.vulkan.version";

    private static volatile boolean exactVersionChecked;
    private static Method exactVersionMethod;

    private DeferredRenderProbe() {
    }

    /** The facts for this device. Never null. */
    public static DeferredRenderSupport.Facts facts(Context context) {
        DeferredRenderSupport.Facts facts = new DeferredRenderSupport.Facts();
        if (context == null) return facts;
        facts.vulkanVersion = vulkanVersion(context);
        facts.glEsVersion = glEsVersion(context);
        facts.softwareRenderer = isSoftwareRenderer();
        return facts;
    }

    /**
     * True when the only renderer the native probe could name is a software rasteriser
     * (SwiftShader, llvmpipe).
     *
     * <p>A Vulkan feature flag alone is not enough: an emulator or a device whose GPU is blocked
     * can advertise Vulkan while every draw runs on SwiftShader, where the deferred pipeline
     * cannot reach a usable rate. The native probe already classifies the vendor/renderer string
     * for the cosmetics quality path, so this reads the same reading rather than inventing a
     * second one. An unread family (or a missing native library) is not a software renderer.
     */
    private static boolean isSoftwareRenderer() {
        try {
            int code = org.chimeramc.client.preloader.PreloaderInput.gpuFamily();
            return org.chimeramc.client.core.cosmetics.GpuFamily
                    .isSoftware(org.chimeramc.client.core.cosmetics.GpuFamily.fromNativeCode(code));
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "gpu-family",
                    "GPU family could not be read; not treating as software", t);
            return false;
        }
    }

    /** True when the device can run the deferred pipeline. */
    public static boolean supported(Context context) {
        return DeferredRenderSupport.supported(facts(context));
    }

    /**
     * The Vulkan API version the platform reports, or 0 when there is no Vulkan driver.
     *
     * <p>{@code android.hardware.vulkan.version} is a {@code PackageManager} feature whose version
     * is the driver's {@code VK_MAKE_VERSION} (major in bits 22-31, minor in bits 12-21). The bare
     * presence of the feature is not enough — a 1.0 driver advertises the feature but cannot run the
     * deferred pipeline — so the API version is what the gate reads. The exact version comes from
     * the framework's {@code getSystemFeatureVersion} when it is reachable; otherwise the public
     * {@code hasSystemFeature(feature, version)} overload answers "at least 1.1" directly.
     */
    private static int vulkanVersion(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            if (pm == null || !pm.hasSystemFeature(FEATURE_VULKAN_VERSION)) return 0;
            int exact = systemFeatureVersion(pm, FEATURE_VULKAN_VERSION);
            if (exact > 0) return exact;
            int oneOne = (1 << 22) | (1 << 12);
            return pm.hasSystemFeature(FEATURE_VULKAN_VERSION, oneOne) ? oneOne : (1 << 22);
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "vulkan",
                    "Vulkan version could not be read; treating as absent", t);
            return 0;
        }
    }

    /** The feature's reported version via the hidden framework API, or 0 when it is not reachable. */
    private static int systemFeatureVersion(PackageManager pm, String feature) {
        try {
            Method method = getSystemFeatureVersionMethod();
            if (method == null) return 0;
            Object value = method.invoke(pm, feature);
            return value instanceof Integer ? (Integer) value : 0;
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "vulkan-exact", "Exact Vulkan version unreadable; using the "
                    + "minimum-version query", t);
            return 0;
        }
    }

    /**
     * The hidden accessor, resolved once per process, or null when this platform does not expose it.
     *
     * <p>{@code getSystemFeatureVersion} is not public API: it is present on some builds and absent
     * on others, so a lookup that fails is the ordinary case rather than a fault. Caching the
     * verdict keeps that one miss from costing a reflection lookup (and a stack trace) on every
     * launch, where it was the loudest thing in logcat.
     */
    private static Method getSystemFeatureVersionMethod() {
        if (exactVersionChecked) return exactVersionMethod;
        synchronized (DeferredRenderProbe.class) {
            if (exactVersionChecked) return exactVersionMethod;
            Method resolved = null;
            try {
                resolved = PackageManager.class.getMethod("getSystemFeatureVersion",
                        String.class);
            } catch (NoSuchMethodException absent) {
                AppLog.warnOnce(TAG, "vulkan-exact-method", "PackageManager has no "
                        + "getSystemFeatureVersion; Vulkan is gated at 1.0 or 1.1 only", absent);
            }
            exactVersionMethod = resolved;
            exactVersionChecked = true;
        }
        return exactVersionMethod;
    }

    private static int glEsVersion(Context context) {
        try {
            ActivityManager am =
                    (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) return 0;
            ConfigurationInfo info = am.getDeviceConfigurationInfo();
            return info == null ? 0 : info.reqGlEsVersion;
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "gles",
                    "GLES version could not be read; treating as absent", t);
            return 0;
        }
    }
}

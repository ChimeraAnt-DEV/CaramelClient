package org.chimeramc.client.core.cosmetics;

import org.chimeramc.client.util.AppLog;

import org.chimeramc.client.BuildConfig;
import org.chimeramc.client.core.crash.CrashReportStore;
import org.chimeramc.client.core.versions.GameVersion;
import org.chimeramc.client.core.versions.VersionManager;
import org.chimeramc.client.util.LauncherStorage;
import org.chimeramc.client.preloader.PreloaderInput;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * The in-app cosmetics diagnostics report.
 *
 * <p>The dev environment has no device and no adb, so the player has to be able to read — and share —
 * the state of the native render path from inside the app. Logcat is not available to the app without
 * {@code READ_LOGS}, so this class reads the native feed directly through {@link PreloaderInput} and
 * formats a plain-text report. Every field is either a real native reading or explicitly "unknown":
 * the report never invents a value.
 *
 * <p>The formatting half is pure and Android-free ({@link #format}) so it is unit-tested; only
 * {@link #readLive} touches Android.
 */
public final class CosmeticsDiagnosticsReport {

    private CosmeticsDiagnosticsReport() {
    }

    /** The raw inputs the report is formatted from. Plain fields so a test can set them directly. */
    public static final class Inputs {
        public String appVersion = "unknown";
        public String minecraftVersion = "unknown";
        /**
         * The device model and Android release, for a reproducible bug report. A report
         * from an unknown device cannot be compared with another, so this is part of the export
         * rather than something the reporter has to remember.
         */
        public String deviceModel = "unknown";
        public boolean nativeLoaded;
        /** The render slot the hook resolved to, or -1 when unknown. */
        public int renderSlot = -1;
        /** 1 = resolved by the render-diagnostic string scan, 0 = by configured index, -1 unknown. */
        public int resolvedBy = -1;
        /** {@code GlBackend}: 0 unknown, 1 GLES, 2 Vulkan. */
        public int glBackend = 0;
        /** The classified GPU family: see {@link GpuFamily#fromNativeCode}; -1 not read. */
        public int gpuFamily = -1;
        /**
         * The number of distinct targets currently carrying a detour, or -1 unknown.
         *
         * <p>The overlay hooks install through a {@code std::call_once}, so a count that rises on a
         * second init attempt, or falls to zero after a successful install, is a real defect the
         * report can show.
         */
        public int hookCount = -1;
        public boolean eglContextCurrent;
        public boolean hookInstalled;
        /** The exact gate that refused the render hook: 0 live, 1 version gate,
         * 2 slot unresolved, 3 not executable, 4 hook() failed; -1 unknown/absent. */
        public int renderHookFailureReason = -1;
        /**
         * True while the deferred hook install is still waiting for the game runtime to come up
         * (armed but not yet run). Distinguishes "waiting" from "failed" in the report.
         */
        public boolean hookInstallPending;
        /** Render ticks seen this session, or -1 when the renderer has not run. */
        public int framesSeen = -1;
        /** Whether the cape's engine texture route is live this session (the cape's live route). */
        public boolean capeEngineRouteLive;
        /** Whether the engine image-loader pipeline observed a texture upload this session. */
        public boolean imagePipelineLive;
        public int lastError;
        /** Bytes of published render geometry, or -1 when none. */
        public int geometryBytes = -1;
        /** bit0 cape, bit1 hat, bit2 pet; -1 when the native library is absent. */
        public int pieceAtlasMask = -1;
        public int capeOverrides;
        public int textureOverrides;
        public String localKeyId = NativeCosmeticsBridge.LOCAL_PLAYER_ID;
        /** Whether the local-player position feed produced a reading this frame. */
        public boolean localPlayerLive;
        /** The effective hook rule's resolved fields (which slots the running build used). */
        public String rulesSummary = "";
        /** The rules file's declared revision, or -1 when unknown. */
        public int rulesRevision = -1;
        /** Extra rules that matched this version (>0 is where first-match selection lost fields). */
        public int rulesMergeConflicts = -1;
        /** Whether the effective rule carries a playerRenderVtableIndex. */
        public boolean rulesHaveRenderIndex;
        /** Whether the rules were delivered to the native library at all this session. */
        public boolean rulesLoadAttempted;
        /** Whether a rules path was supplied to the native side. */
        public boolean rulesConfigured;
        /** Whether the rules file was read and parsed into a usable rule set. */
        public boolean rulesLoaded;
        /** How many copies of libsweetloader.so are mapped; >1 means a split, empty registry. */
        public int sweetloaderModuleMappings = -1;
        /** The deferred hook-install state: 0 not armed, 1 waiting, 2 resolved. */
        public int hookInstallState;
        /** Frame ticks the install gate observed before deciding. */
        public int hookInstallFrameTicks;
        /** Camera anchor source: 1 engine view-projection, 0 first-person fallback, -1 unknown. */
        public int cameraAnchorSource = -1;
        /** The rules source actually in use ("bundled" / "remote"), for the report. */
        public String rulesSource = "bundled";
        /** Local player feed: {x, y, z, yaw, pitch} when live, else null. */
        public float[] localPlayerPosition;
        /** True when the engine captured a base-skin texture this session (the real-skin route). */
        public boolean skinCaptured;
        /** True when a game-skin provider is installed (the engine-hook seam is active). */
        public boolean skinFeedInstalled;
        /** The skin-path seam stage: 0 join hook absent, 1 hook idle, 2 joins observed; -1 unknown. */
        public int skinSeamStage = -1;
        /** Join-packet calls the detour forwarded, or -1 when the hook is absent. */
        public int skinJoinCalls = -1;
        /** Texture-bind calls the detour forwarded, or -1 when the hook is absent. */
        public int skinTextureCalls = -1;
        /** The native one-line explanation for {@link #skinSeamStage}, or "". */
        public String skinSeamNote = "";
        /** Per-cosmetic asset audit lines (PNG size, geometry size, cube count), or null. */
        public java.util.List<CosmeticAssetAudit.Entry> cosmeticAssets;
        /** Faces the scene refused to build (a size mismatch), as one-line descriptions. */
        public java.util.List<String> skippedFaces;
        /** The most recent crash report text (from the crash_logs dir), or "" when none. */
        public String lastCrashReport = "";
        /** True when the crash report was truncated for display. */
        public boolean lastCrashTruncated;
        /** True when the report was generated while a world was loaded (not in a menu). */
        public boolean inWorld;
        /** The render thread's smoothed frame interval in microseconds, or -1 unknown. */
        public int framePacingAverageUs = -1;
        /** The worst frame interval in the recent window, or -1 unknown. */
        public int framePacingWorstUs = -1;
        /** A recent frame was late (a hitch). */
        public boolean framePacingJittering;
        /** The frame interval matched the display interval. */
        public boolean framePacingVsyncLocked;
        /** Whether a frame-pacing reading exists at all this session. */
        public boolean framePacingAvailable;
        /** The median frame interval in microseconds over the long window, or -1. */
        public int framePacingP50Us = -1;
        /** The 95th percentile frame interval, or -1. */
        public int framePacingP95Us = -1;
        /** The 99th percentile frame interval, or -1. */
        public int framePacingP99Us = -1;
        /** Frames above twice the median, per minute, or -1 unknown. */
        public int framePacingHitchesPerMinute = -1;
        /** This process's resident footprint in KiB, or -1 when no source was readable. */
        public int processResidentKb = -1;
        /** This process's proportional (Pss) footprint in KiB, or -1 unknown. */
        public int processProportionalKb = -1;
        /** True when a game session is running (the launcher's own session flag). */
        public boolean sessionActive;
        /** The HUD hook's "a HUD screen is open" reading, or false when the hook has not fired. */
        public boolean hudScreenOpen;
        public long timestampMs;
    }

    /** A one-shot reading of the live state, formatted into a plain-text report. */
    public static String readLive(Context context) {
        return format(readInputs(context));
    }

    /** Gathers the live inputs. Every native read is guarded so an absent library reads as unknown. */
    public static Inputs readInputs(Context context) {
        Inputs in = new Inputs();
        in.timestampMs = System.currentTimeMillis();
        in.appVersion = safeVersionName();
        in.minecraftVersion = selectedMinecraftVersion(context);
        in.deviceModel = deviceModelText();

        int[] diag = PreloaderInput.readRenderDiagnostics();
        in.nativeLoaded = diag != null;
        if (diag != null) {
            in.renderSlot = diag[0];
            in.resolvedBy = diag[1];
            in.glBackend = diag[2];
            in.gpuFamily = diag.length >= 18 ? diag[17] : -1;
            in.hookCount = diag.length >= 19 ? diag[18] : -1;
            in.eglContextCurrent = diag[3] == 1;
            in.hookInstalled = diag[4] == 1;
            in.renderHookFailureReason = PreloaderInput.renderHookFailureReason();
            in.framesSeen = diag[5];
            in.lastError = diag[6];
            in.geometryBytes = diag[7];
            in.pieceAtlasMask = diag[8];
            if (diag.length >= 17) {
                in.localPlayerLive = diag[10] == 1;
                in.rulesRevision = diag[11];
                in.rulesMergeConflicts = diag[12];
                in.rulesHaveRenderIndex = diag[13] == 1;
                in.rulesLoadAttempted = diag[14] == 1;
                in.rulesConfigured = diag.length >= 21 && diag[19] == 1;
                in.rulesLoaded = diag.length >= 21 && diag[20] == 1;
                in.sweetloaderModuleMappings = diag[15];
            }
        }
        in.cameraAnchorSource = PreloaderInput.cameraAnchorSource();

        // The render-thread frame-pacing reading, when the renderer has produced frames.
        try {
            int[] pacing = PreloaderInput.readFramePacing();
            if (pacing != null) {
                in.framePacingAvailable = true;
                in.framePacingAverageUs = pacing[1];
                in.framePacingWorstUs = pacing[2];
                in.framePacingJittering = (pacing[3] & 1) != 0;
                in.framePacingVsyncLocked = (pacing[3] & 2) != 0;
            }
        } catch (Throwable ignored) {
            AppLog.warnOnce("CaramelClient", "CosmeticsDiagnosticsReport:213", "Ignored a failure at CosmeticsDiagnosticsReport:213", ignored);
        }

        // The percentile + hitch reading. Separate from the per-frame one because it
        // sorts the long window; it is a summary the report shows beside the average.
        try {
            int[] percentiles = PreloaderInput.readFramePacingPercentiles();
            if (percentiles != null) {
                in.framePacingP50Us = percentiles[1];
                in.framePacingP95Us = percentiles[2];
                in.framePacingP99Us = percentiles[3];
                in.framePacingHitchesPerMinute = percentiles[4];
            }
        } catch (Throwable ignored) {
            AppLog.warnOnce("CaramelClient", "CosmeticsDiagnosticsReport:227", "Ignored a failure at CosmeticsDiagnosticsReport:227", ignored);
        }

        // The process footprint, so a session that grows is visible in the report.
        try {
            int[] memory = PreloaderInput.readProcessMemory();
            if (memory != null) {
                in.processResidentKb = memory[1];
                in.processProportionalKb = memory[2];
            }
        } catch (Throwable ignored) {
            AppLog.warnOnce("CaramelClient", "CosmeticsDiagnosticsReport:238", "Ignored a failure at CosmeticsDiagnosticsReport:238", ignored);
        }

        // The local-player feed, decoded from its raw float bits. Only present when live.
        int[] feed = PreloaderInput.readLocalPlayerFeed();
        if (feed != null && feed.length >= 6 && feed[0] == 1) {
            in.localPlayerPosition = new float[] {
                    Float.intBitsToFloat(feed[1]), Float.intBitsToFloat(feed[2]),
                    Float.intBitsToFloat(feed[3]), Float.intBitsToFloat(feed[4]),
                    Float.intBitsToFloat(feed[5])};
        }

        try {
            in.hookInstallState = org.chimeramc.client.core.mods.ModManager.gameHookInstallState();
            in.hookInstallFrameTicks =
                    org.chimeramc.client.core.mods.ModManager.gameHookInstallFrameTicks();
            in.rulesSummary = org.chimeramc.client.core.mods.ModManager.gameHookRulesSummary();
        } catch (Throwable ignored) {
            AppLog.warnOnce("CaramelClient", "CosmeticsDiagnosticsReport:256", "Ignored a failure at CosmeticsDiagnosticsReport:256", ignored);
        }
        try {
            in.rulesSource = org.chimeramc.client.preloader.SweetLoaderSignatureRulesManager
                    .getRulesSource(context);
        } catch (Throwable ignored) {
            AppLog.warnOnce("CaramelClient", "CosmeticsDiagnosticsReport:262", "Ignored a failure at CosmeticsDiagnosticsReport:262", ignored);
        }
        in.capeEngineRouteLive = NativeCosmeticsFeed.isCapeEngineRouteLive();
        in.imagePipelineLive = NativeCosmeticsBridge.isImagePipelineLive();
        try {
            in.hookInstallPending =
                    org.chimeramc.client.core.mods.ModManager.isGameHookInstallPending();
        } catch (Throwable ignored) {
            AppLog.warnOnce("CaramelClient", "CosmeticsDiagnosticsReport:270", "Ignored a failure at CosmeticsDiagnosticsReport:270", ignored);
        }
        try {
            in.sessionActive =
                    org.chimeramc.client.settings.LowLatencyNetworkManager.isGameSessionActive();
        } catch (Throwable ignored) {
            AppLog.warnOnce("CaramelClient", "CosmeticsDiagnosticsReport:276", "Ignored a failure at CosmeticsDiagnosticsReport:276", ignored);
        }
        try {
            in.hudScreenOpen = PreloaderInput.isHudScreenOpen();
        } catch (Throwable ignored) {
            AppLog.warnOnce("CaramelClient", "CosmeticsDiagnosticsReport:281", "Ignored a failure at CosmeticsDiagnosticsReport:281", ignored);
        }
        // "In a world" is the HUD hook having reported a world at least once and no HUD screen being
        // open right now. Before the hook fires there is no reading at all, so the report says so
        // rather than claiming a menu.
        in.inWorld = in.sessionActive && !in.hudScreenOpen;
        try {
            in.skinCaptured = PreloaderInput.nativeHasCapturedSkin();
        } catch (Throwable ignored) {
            AppLog.warnOnce("CaramelClient", "CosmeticsDiagnosticsReport:290", "Ignored a failure at CosmeticsDiagnosticsReport:290", ignored);
        }
        in.skinFeedInstalled = GameSkinSource.hasProvider();
        // Which stage the skin path reached, so a "skin not visible" report can be routed
        // (client never reached the join handler, versus reached it with the outcome unproven).
        try {
            int[] seam = PreloaderInput.readSkinSeam();
            if (seam != null) {
                in.skinSeamStage = seam[0];
                in.skinJoinCalls = seam[2];
                in.skinTextureCalls = seam[3];
                in.skinSeamNote = PreloaderInput.describeSkinSeam(seam[0]);
            }
        } catch (Throwable ignored) {
            AppLog.warnOnce("CaramelClient", "CosmeticsDiagnosticsReport:304", "Ignored a failure at CosmeticsDiagnosticsReport:304", ignored);
        }

        // The most recent crash report, so a device crash trace can be copied/shared from inside the
        // app (logcat is not app-readable without READ_LOGS).
        try {
            CrashReportStore.Report report =
                    CrashReportStore.readDir(LauncherStorage.getCrashLogsDir(context));
            if (report.isPresent()) {
                in.lastCrashReport = report.text;
                in.lastCrashTruncated = report.truncated;
            }
        } catch (Throwable ignored) {
            AppLog.warnOnce("CaramelClient", "CosmeticsDiagnosticsReport:317", "Ignored a failure at CosmeticsDiagnosticsReport:317", ignored);
        }
        // Mod-menu build failures the containment guard caught.

        // Per-cosmetic asset audit: the real PNG size, the geometry's declared size and the cube
        // count, so a size mismatch (which used to crash the preview) is visible without a device.
        try {
            if (context != null) {
                in.cosmeticAssets = CosmeticAssetAudit.scan(
                        path -> context.getAssets().open(path));
            }
        } catch (Throwable ignored) {
            AppLog.warnOnce("CaramelClient", "CosmeticsDiagnosticsReport:330", "Ignored a failure at CosmeticsDiagnosticsReport:330", ignored);
        }
        // Faces the scene refused to build, with the reason.
        java.util.List<PreviewSceneDiagnostics.Entry> skipped = PreviewSceneDiagnostics.snapshot();
        if (!skipped.isEmpty()) {
            in.skippedFaces = new java.util.ArrayList<>();
            for (PreviewSceneDiagnostics.Entry e : skipped) in.skippedFaces.add(e.describe());
        }

        int[] stats = NativeCosmeticsBridge.stats();
        if (stats != null && stats.length >= 4) {
            in.capeOverrides = stats[2];
            in.textureOverrides = stats[3];
        }
        return in;
    }

    private static String safeVersionName() {
        try {
            String name = BuildConfig.VERSION_NAME;
            return name == null || name.isEmpty() ? "unknown" : name;
        } catch (Throwable t) {
            return "unknown";
        }
    }

    private static String selectedMinecraftVersion(Context context) {
        try {
            if (context == null) return "none selected";
            VersionManager manager = VersionManager.get(context);
            if (manager == null) return "none selected";
            GameVersion version = manager.getSelectedVersion();
            if (version == null || version.versionCode == null || version.versionCode.isEmpty()) {
                return "none selected";
            }
            return version.versionCode;
        } catch (Throwable t) {
            return "none selected";
        }
    }

    /**
     * Formats the report. Pure: no Android types, so a JVM test pins the exact lines and their
     * truthful wording.
     */
    public static String format(Inputs in) {
        Inputs v = in == null ? new Inputs() : in;
        StringBuilder b = new StringBuilder(1024);
        b.append("CaramelClient — Cosmetics diagnostics\n");
        b.append("Generated: ").append(timestamp(v.timestampMs)).append('\n');
        b.append("Generated inside a world: ").append(yesNo(v.inWorld)).append('\n');
        b.append("App version: ").append(v.appVersion).append('\n');
        b.append("Minecraft version: ").append(v.minecraftVersion).append('\n');
        b.append("Device: ").append(v.deviceModel == null || v.deviceModel.isEmpty()
                ? "unknown" : v.deviceModel).append('\n');
        b.append('\n');

        b.append("FIRST FAILING STAGE: ").append(firstFailingStage(v)).append('\n');
        b.append('\n');

        b.append("Native library loaded: ").append(yesNo(v.nativeLoaded)).append('\n');
        b.append("Rules source: ").append(v.rulesSource == null ? "bundled" : v.rulesSource)
                .append(" (revision ").append(countText(v.rulesRevision)).append(")\n");
        b.append("Rules delivered to native: ").append(yesNo(v.rulesLoadAttempted)).append('\n');
        b.append("Rules parsed into effect: ").append(yesNo(v.rulesLoaded)).append('\n');
        b.append("Effective rule: ").append(ruleSummaryText(v)).append('\n');
        b.append("Rules matching this version (extra): ").append(countText(v.rulesMergeConflicts))
                .append('\n');
        b.append("Rule has render slot: ").append(yesNo(v.rulesHaveRenderIndex)).append('\n');
        b.append("Hook install state: ").append(installStateText(v)).append('\n');
        b.append("libsweetloader.so copies mapped: ").append(mappingText(v.sweetloaderModuleMappings))
                .append('\n');
        b.append("Player-render slot: ").append(slotText(v)).append('\n');
        b.append("Hook installed: ").append(hookText(v)).append('\n');
        b.append("Hook refusal reason: ").append(hookRefusalText(v)).append('\n');
        b.append("Frames seen: ").append(countText(v.framesSeen)).append('\n');
        b.append("Local player feed: ").append(localPlayerText(v)).append('\n');
        b.append("Camera anchor source: ").append(cameraText(v)).append('\n');
        b.append("Frame pacing: ").append(framePacingText(v)).append('\n');
        b.append("Frame percentiles: ").append(framePercentilesText(v)).append('\n');
        b.append("Process memory: ").append(processMemoryText(v)).append('\n');
        b.append("Cape engine texture route: ").append(yesNo(v.capeEngineRouteLive)).append('\n');
        b.append("Engine image pipeline live: ").append(yesNo(v.imagePipelineLive)).append('\n');
        b.append("GL backend: ").append(backendText(v.glBackend)).append('\n');
        b.append("GPU family: ").append(gpuFamilyText(v.gpuFamily)).append('\n');
        b.append("Hooks installed: ").append(hookCountText(v.hookCount)).append('\n');
        b.append("EGL context current: ").append(yesNo(v.eglContextCurrent)).append('\n');
        b.append("Last GL error: ").append(errorText(v.lastError)).append('\n');
        b.append('\n');

        b.append("Layers (item G9):\n");
        b.append("  render hook: ").append(layerRenderHook(v)).append('\n');
        b.append("  skin data: ").append(layerSkinData(v)).append('\n');
        b.append("  session: ").append(layerSession(v)).append('\n');
        b.append('\n');

        b.append("Published geometry bytes: ").append(countText(v.geometryBytes)).append('\n');
        b.append("Piece atlases: ").append(pieceMaskText(v.pieceAtlasMask)).append('\n');
        b.append("Cape overrides: ").append(v.capeOverrides).append('\n');
        b.append("Texture overrides: ").append(v.textureOverrides).append('\n');
        b.append("Local cosmetics published under key: \"").append(v.localKeyId).append("\"\n");
        b.append("Game-skin feed installed: ").append(yesNo(v.skinFeedInstalled)).append('\n');
        b.append("Base skin captured from game: ").append(yesNo(v.skinCaptured)).append('\n');
        b.append("Skin path seam: ").append(skinSeamText(v)).append('\n');
        b.append('\n');

        b.append("Cosmetic assets (PNG size / geometry size / cubes):\n");
        b.append(CosmeticAssetAudit.format(v.cosmeticAssets));
        b.append('\n');

        b.append("Faces skipped (atlas size mismatch): ");
        if (v.skippedFaces == null || v.skippedFaces.isEmpty()) {
            b.append("none\n");
        } else {
            b.append(v.skippedFaces.size()).append('\n');
            for (String s : v.skippedFaces) b.append("  ").append(s).append('\n');
        }
        b.append('\n');

        b.append("Status: ").append(statusText(v)).append('\n');
        b.append('\n');

        b.append("Last crash report: ");
        if (v.lastCrashReport == null || v.lastCrashReport.isEmpty()) {
            b.append("none\n");
        } else {
            b.append(v.lastCrashTruncated ? "(truncated)\n" : "\n");
            b.append(v.lastCrashReport);
            if (!v.lastCrashReport.endsWith("\n")) b.append('\n');
        }
        b.append('\n');
        b.append("Note: whether cosmetics actually appear in game is unverified on device. This "
                + "report only describes the state of the native render path.\n");
        return b.toString();
    }

    /**
     * The device model plus Android release. A report from an unknown device cannot be reproduced,
     * so this is read here (the only Android touch) and formatted by the pure path.
     */
    private static String deviceModelText() {
        try {
            String model = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
            // The ABI is one of the gates that can refuse a feature, so the report names the
            // process ABI next to the device. The sweetloader is arm64-only, so a report from a device
            // where the native library did not load needs the ABI to be diagnosable.
            String abi = android.os.Build.SUPPORTED_ABIS.length > 0
                    ? android.os.Build.SUPPORTED_ABIS[0] : "unknown";
            return model.trim() + " (" + abi + ", Android " + android.os.Build.VERSION.RELEASE + ")";
        } catch (Throwable t) {
            return "unknown";
        }
    }

    private static String timestamp(long ms) {
        long value = ms > 0 ? ms : System.currentTimeMillis();
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date(value));
    }

    private static String yesNo(boolean value) {
        return value ? "yes" : "no";
    }

    /**
     * The hook-install line. "waiting for the render context" is distinct from a plain "no": the
     * install is deferred to the game's frame tick, so an armed-but-not-yet-run state is expected
     * briefly and must not read as a failure.
     */
    private static String hookText(Inputs v) {
        if (v.hookInstalled) return "yes";
        return v.hookInstallPending ? "waiting for the render context" : "no";
    }

    /** Maps the render hook's refusal reason to the exact gate that refused it. */
    private static String hookRefusalText(Inputs v) {
        if (v.hookInstalled) return "n/a (hook live)";
        switch (v.renderHookFailureReason) {
            case 0:
                return "not refused (still pending or not yet resolved)";
            case 1:
                return "build outside the verified version range";
            case 2:
                return "LivePlayerRenderer render slot unresolved";
            case 3:
                return "resolved slot is not executable code";
            case 4:
                return "hook() install call failed";
            case 5:
                return "the matching rule's probe for the render seam did not resolve";
            default:
                return "unknown";
        }
    }

    private static String countText(int value) {
        return value < 0 ? "unknown" : Integer.toString(value);
    }

    private static String slotText(Inputs v) {
        if (v.renderSlot < 0) return "unknown (hook not installed)";
        String how;
        if (v.resolvedBy == 1) {
            how = "resolved by render-diagnostic string scan";
        } else if (v.resolvedBy == 0) {
            how = "resolved by configured/default index";
        } else {
            how = "resolution method unknown";
        }
        return v.renderSlot + " (" + how + ")";
    }

    private static String backendText(int backend) {
        return RenderBackend.label(RenderBackend.fromNativeCode(backend));
    }

    /**
     * The classified GPU family, or "unknown" when the native side has not read the driver strings.
     * A negative code is the "not read" sentinel, never a family, so it must not be labelled.
     */
    private static String gpuFamilyText(int nativeCode) {
        if (nativeCode < 0) return "unknown";
        return GpuFamily.label(GpuFamily.fromNativeCode(nativeCode));
    }

    /** The live hook count, or "unknown" when the native side did not report it. Never "0 hooks"
     * for an unread value: that reads as a real failure rather than a missing measurement. */
    private static String hookCountText(int count) {
        return count < 0 ? "unknown" : Integer.toString(count);
    }

    private static String errorText(int error) {
        if (error < 0) return "unknown";
        return error == 0 ? "none" : "0x" + Integer.toHexString(error);
    }

    private static String pieceMaskText(int mask) {
        if (mask < 0) return "unknown";
        StringBuilder b = new StringBuilder();
        if ((mask & 1) != 0) b.append("cape ");
        if ((mask & 2) != 0) b.append("hat ");
        if ((mask & 4) != 0) b.append("pet ");
        return b.length() == 0 ? "none published" : b.toString().trim();
    }

    private static String ruleSummaryText(Inputs v) {
        if (v.rulesSummary == null || v.rulesSummary.isEmpty()) return "unknown";
        return v.rulesSummary;
    }

    private static String installStateText(Inputs v) {
        switch (v.hookInstallState) {
            case 0: return "not armed";
            case 1: return "armed, waiting for the render context (" + v.hookInstallFrameTicks
                    + " frame tick(s))";
            case 2: return "resolved after " + v.hookInstallFrameTicks + " frame tick(s)";
            default: return "unknown";
        }
    }

    private static String mappingText(int count) {
        if (count < 0) return "unknown";
        if (count > 1) return count + " (SPLIT: more than one copy has an empty registry)";
        return Integer.toString(count);
    }

    private static String localPlayerText(Inputs v) {
        if (!v.localPlayerLive) return "no live local player";
        if (v.localPlayerPosition == null || v.localPlayerPosition.length < 5) return "live (values unavailable)";
        return String.format(Locale.US, "live x=%.2f y=%.2f z=%.2f yaw=%.2f pitch=%.2f",
                v.localPlayerPosition[0], v.localPlayerPosition[1], v.localPlayerPosition[2],
                v.localPlayerPosition[3], v.localPlayerPosition[4]);
    }

    /**
     * The skin-path seam line. Pure, so the wording is pinned by a JVM test. A missing
     * native library renders "unknown" rather than a confident "not installed".
     */
    private static String skinSeamText(Inputs v) {
        if (v.skinSeamStage < 0) return "unknown (native library absent)";
        StringBuilder b = new StringBuilder();
        b.append(v.skinSeamNote == null || v.skinSeamNote.isEmpty()
                ? "stage " + v.skinSeamStage : v.skinSeamNote);
        if (v.skinJoinCalls >= 0 || v.skinTextureCalls >= 0) {
            b.append(" [joins=").append(v.skinJoinCalls >= 0 ? v.skinJoinCalls : -1)
                    .append(" textures=").append(v.skinTextureCalls >= 0 ? v.skinTextureCalls : -1)
                    .append(']');
        }
        return b.toString();
    }

    private static String cameraText(Inputs v) {
        if (v.cameraAnchorSource < 0) return "unknown";
        return v.cameraAnchorSource == 1
                ? "engine view-projection"
                : "first-person fallback (engine camera did not resolve)";
    }

    /**
     * The render-thread frame-pacing line. "no reading yet" is distinct from a bad
     * interval: the renderer has to produce two frames before any interval exists. The verdict
     * never claims a lock without a reading, and an unknown display rate is never called locked.
     */
    private static String framePacingText(Inputs v) {
        if (!v.framePacingAvailable || v.framePacingAverageUs < 0) return "no reading yet";
        StringBuilder s = new StringBuilder();
        s.append(formatMicros(v.framePacingAverageUs)).append(" average");
        if (v.framePacingWorstUs >= 0) {
            s.append(", ").append(formatMicros(v.framePacingWorstUs)).append(" worst");
        }
        if (v.framePacingJittering) {
            s.append(" — a recent frame was late (hitching)");
        } else if (v.framePacingVsyncLocked) {
            s.append(" — vsync-locked");
        } else {
            s.append(" — steady, not vsync-locked");
        }
        return s.toString();
    }

    /**
     * The median and tail frame intervals plus the hitch rate. A percentile a device
     * cannot report reads as "no reading yet", never as 0 ms (which would look like a perfect
     * frame).
     */
    private static String framePercentilesText(Inputs v) {
        if (v.framePacingP50Us < 0 || v.framePacingP95Us < 0 || v.framePacingP99Us < 0) {
            return "no reading yet";
        }
        StringBuilder s = new StringBuilder();
        s.append("p50 ").append(formatMicros(v.framePacingP50Us));
        s.append(", p95 ").append(formatMicros(v.framePacingP95Us));
        s.append(", p99 ").append(formatMicros(v.framePacingP99Us));
        if (v.framePacingHitchesPerMinute >= 0) {
            s.append(" — ").append(v.framePacingHitchesPerMinute).append(" hitches/min");
        }
        return s.toString();
    }

    /**
     * The process footprint. The proportional figure is the honest one when the kernel
     * exposes it (it is this process's share of shared pages); the resident figure counts shared
     * pages in full. An unreadable source says so rather than printing "0 KB".
     */
    private static String processMemoryText(Inputs v) {
        if (v.processResidentKb < 0 && v.processProportionalKb < 0) {
            return "no reading yet";
        }
        StringBuilder s = new StringBuilder();
        if (v.processProportionalKb >= 0) {
            s.append(formatKb(v.processProportionalKb)).append(" (Pss)");
            if (v.processResidentKb >= 0) s.append(", ");
        }
        if (v.processResidentKb >= 0) {
            s.append(formatKb(v.processResidentKb)).append(" resident");
        }
        return s.toString();
    }

    /** Kibibytes to a short "182 MB" form. */
    private static String formatKb(int kb) {
        if (kb < 1024) return kb + " KB";
        return String.format(Locale.US, "%.1f MB", kb / 1024.0);
    }

    /** Microseconds to a short "16.7 ms" form; a sub-millisecond value is shown in microseconds. */
    private static String formatMicros(int micros) {
        if (micros < 1000) return micros + " us";
        return String.format(Locale.US, "%.1f ms", micros / 1000.0);
    }

    /** The honest status line, matching the rule the Cosmetics panel uses. */
    static String statusText(Inputs v) {
        if (!v.nativeLoaded) {
            return "native library not loaded — cosmetics are preview-only.";
        }
        if (v.capeEngineRouteLive) {
            return "cape active through the engine texture loader"
                    + (v.imagePipelineLive ? " (image pipeline live)" : "")
                    + (v.hookInstalled ? "; accessories and pets reach the game through the native "
                            + "player-render hook." : "; accessories and pets need the native "
                            + "player-render hook, which is not live this session.");
        }
        if (v.hookInstalled) {
            return "native player-render hook live; the cape's engine texture route has not "
                    + "observed a loaded cape texture yet this session.";
        }
        return "render hook not installed and no cape texture observed — cosmetics are preview-only.";
    }

    /**
     * The render-hook layer of the G9 breakdown: how cosmetics geometry reaches the game's own
     * player renderer. "waiting" is distinct from "not live" because the install is deferred to the
     * game's frame tick.
     */
    private static String layerRenderHook(Inputs v) {
        if (!v.nativeLoaded) return "native library not loaded";
        if (v.hookInstalled) return "live (renderer frames seen: " + countText(v.framesSeen) + ")";
        if (v.hookInstallPending) return "waiting for the render context";
        return "not live (" + hookRefusalText(v) + ")";
    }

    /**
     * The skin-data layer of the G9 breakdown: whether the game's own skin (the base skin the
     * engine loads) reached the launcher's feed, and how far the join/texture seam got.
     */
    private static String layerSkinData(Inputs v) {
        if (v.skinSeamStage < 0 && !v.skinFeedInstalled && !v.skinCaptured) {
            return "unknown (native skin seam absent)";
        }
        return (v.skinCaptured ? "captured" : "no capture yet")
                + (v.skinFeedInstalled ? " (game-skin feed installed)" : "")
                + (v.skinSeamStage >= 0 ? "; " + skinSeamText(v) : "");
    }

    /**
     * The session layer of the G9 breakdown: whether the report was generated in a live game
     * session or in the launcher, which decides how much of the pipeline could have run.
     */
    private static String layerSession(Inputs v) {
        if (!v.sessionActive) return "launcher (no game session this process)";
        return v.inWorld ? "in-world" : "game session, menu/loading screen";
    }

    /**
     * Names the first stage that did not complete, in the order the native path is built.
     *
     * <p>A bare "Status" line says the end result but not which link broke; with the install
     * deferred to the game's frame tick, "not installed" could mean the library never loaded, the
     * rules never arrived, no rule matched the version, or the runtime never came up. This walks the
     * same order the sweetloader does and stops at the first failure, so a device report points at one
     * step instead of leaving the reader to guess. Every stage is a real reading; nothing is
     * inferred.
     */
    static String firstFailingStage(Inputs v) {
        if (!v.nativeLoaded) {
            return "0. native library — libsweetloader.so is not loaded, so nothing below ran.";
        }
        if (v.rulesRevision <= 0) {
            return "1. rules file — the delivered rules declared revision 0 (or none was delivered);"
                    + " the file did not reach the native side or is unreadable.";
        }
        if (!v.rulesLoadAttempted) {
            return "2. rules load — the hook install never ran, so the rules file was never read"
                    + " (the install is deferred to the game's frame tick; see the install state).";
        }
        if (!v.rulesLoaded) {
            return "2. rules parse — the rules file was read but could not be parsed into a usable"
                    + " rule set: the file is missing, malformed, or carries no rule for Minecraft "
                    + v.minecraftVersion + ".";
        }
        if (!v.rulesHaveRenderIndex) {
            return "3. rule match — no matching rule for Minecraft " + v.minecraftVersion
                    + " carried a playerRenderVtableIndex, so the render slot could not resolve.";
        }
        if (v.hookInstallState == 0) {
            return "4. install armed — the deferred install was never armed; the game activity did"
                    + " not reach the pump.";
        }
        if (v.hookInstallState == 1) {
            return "4. install armed — waiting for the game runtime (" + v.hookInstallFrameTicks
                    + " frame tick(s)); if this persists the runtime never came up.";
        }
        if (!v.hookInstalled) {
            return "5. render slot — the install ran but the render hook is not live; the slot did"
                    + " not resolve to an executable address on this build.";
        }
        if (v.capeEngineRouteLive) {
            return "none — the cape reaches the game through the engine texture loader this session.";
        }
        return "none blocking — the render hook is live; the cape's engine texture route fires once"
                + " the engine has loaded your cape texture this session.";
    }
}

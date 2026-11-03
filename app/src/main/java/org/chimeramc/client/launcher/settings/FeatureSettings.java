package org.chimeramc.client.settings;

import android.content.Context;

public class FeatureSettings {
    private static volatile FeatureSettings INSTANCE;
    private static Context appContext;
    private boolean launcherManagedMcLoginEnabled = false;
    private boolean logcatOverlayEnabled = false;

    /**
     * Whether the launch screen shows the raw launch log instead of the logo + status ring.
     *
     * <p>Off by default: the launch screen is a calm "Preparing to launch" surface, and the raw log is
     * a diagnostic a user opts into from Settings. A file written before this switch existed loads it
     * off, which is the documented default.
     */
    private boolean launchLogEnabled = false;
    private boolean lowInputDelayEnabled = false;
    /**
     * Crash report upload. Off until the user opts in: a report carries a stack trace
     * and the app version, and sending it is a disclosure the user must make deliberately. The
     * Settings row shows exactly what would be sent before the switch is turned on.
     */
    private Boolean crashUploadEnabled = false;

    /**
     * The opt-in anonymous usage counter. Off by default: it is a disclosure, so it is
     * the user's decision, and the Settings row shows exactly which fields would be sent before the
     * switch can be turned on. The counter is inert in the shipped source anyway, because this
     * build has no collector configured ({@code BuildConfig.USAGE_STATS_ENDPOINT}).
     */
    private Boolean usageStatsEnabled = false;
    private boolean foregroundServiceEnabled = false;
    private boolean reduceNetworkLatencyEnabled = false;
    private boolean fpsOptimizerEnabled = false;

    /**
     * Guest mode: hide mods, servers and voice chat on a shared or borrowed device.
     *
     * <p>Off by default, like the other behaviour-changing switches, so an existing setup is never
     * altered silently. It hides surfaces only -- it never disables a feature or blocks a launch.
     */
    private boolean guestModeEnabled = false;

    /**
     * Streaming-safe mode: mask account names, coordinates and server addresses in
     * overlays and in any text that leaves the app.
     */
    private boolean streamingSafeModeEnabled = false;

    /**
     * Whether the daily mod-update check may run.
     *
     * <p>On by default, and deliberately so: the check is inert unless this build was given a feed
     * URL ({@code BuildConfig.MOD_UPDATE_FEED_URL}), so the shipped source makes no request at all
     * and a build that configures a feed gets the feature without every user having to find a
     * switch. It is one request a day at most and it only reads a public version list -- the
     * privacy-sensitive reporting is a separate opt-in. A user who wants no check turns
     * this off and the throttle state is left untouched.
     */
    private boolean modUpdateCheckEnabled = true;

    public boolean isModUpdateCheckEnabled() { return modUpdateCheckEnabled; }

    public void setModUpdateCheckEnabled(boolean enabled) {
        if (this.modUpdateCheckEnabled == enabled) return;
        this.modUpdateCheckEnabled = enabled;
        autoSave();
    }

    /**
     * The top-level destination the user was last on, by {@code LauncherTab} name.
     *
     * <p>Stored as the enum's name, not its view id: an id is a generated resource number that a
     * later build can renumber, so a stored id would silently point at a different tab after an
     * update. A name this build does not know reads as "no preference" and the launcher opens on
     * Play.
     */
    private String lastNavTab = null;

    /**
     * Whether the user collapsed the wide navigation rail to icon-only.
     *
     * <p>A tablet user who prefers the icons to a labelled rail can fold it, and the choice is
     * remembered across launches. It is a presentation preference only: it never changes which
     * destination exists, only whether the expanded rail shows its labels. Default false, so an
     * existing setup keeps the shape its window width already gives it.
     */
    private boolean navRailCollapsed = false;

    // --- Game Performance Mode -------------------------------------------------------------
    // The master switch plus one toggle per optimization, so a user can isolate an item that
    // misbehaves on their device. Tier-1 (host) items default on; Tier-2 (game hook) items
    // default off, because a hook that resolves wrong can change how the game renders.
    private boolean optifineModeEnabled = false;
    private boolean optifineAllocatorEnabled = true;
    private boolean optifineRenderPriorityEnabled = true;
    private boolean optifineCpuAffinityEnabled = true;
    private boolean optifineRefreshRateEnabled = true;
    private boolean optifineEntityCullingEnabled = false;
    private boolean optifineParticleCullingEnabled = false;
    private boolean optifineRenderDistanceEnabled = false;
    private boolean optifineCallbackTrimmingEnabled = false;
    private boolean optifineOreUiStrippingEnabled = false;
    /**
     * Consecutive launches that ended before a session started, keyed by optifine item id.
     * Managed by {@code OptifineModeManager}; an item is auto-disabled at three.
     */
    private java.util.Map<String, Integer> optifineCrashCounts = new java.util.HashMap<>();

    public enum StorageType {
        INTERNAL,
        EXTERNAL,
        VERSION_ISOLATION,
        VERSION_ISOLATION_INTERNAL,
        VERSION_ISOLATION_EXTERNAL
    }

    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static FeatureSettings getInstance() {
        if (INSTANCE == null) {
            synchronized (FeatureSettings.class) {
                if (INSTANCE == null) {
                    INSTANCE = SettingsStorage.load(appContext);
                    if (INSTANCE == null) {
                        INSTANCE = new FeatureSettings();
                    }
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Re-reads the persisted settings into the singleton.
     *
     * Restoring a backup writes the store directly, but the singleton survives for the life of
     * the process, so without this the current session would keep serving the pre-import values
     * until the next launch. Callers must hold no references to the old instance.
     */
    public static void reload(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
        }
        synchronized (FeatureSettings.class) {
            INSTANCE = SettingsStorage.load(appContext);
            if (INSTANCE == null) {
                INSTANCE = new FeatureSettings();
            }
        }
    }

    public boolean isLauncherManagedMcLoginEnabled() { return launcherManagedMcLoginEnabled; }
    public void setLauncherManagedMcLoginEnabled(boolean enabled) { this.launcherManagedMcLoginEnabled = enabled; autoSave(); }

    public boolean isLaunchLogEnabled() { return launchLogEnabled; }

    public void setLaunchLogEnabled(boolean enabled) {
        if (this.launchLogEnabled == enabled) return;
        this.launchLogEnabled = enabled;
        autoSave();
    }

    public boolean isLogcatOverlayEnabled() { return logcatOverlayEnabled; }
    public void setLogcatOverlayEnabled(boolean enabled) { this.logcatOverlayEnabled = enabled; autoSave(); }

    public boolean isLowInputDelayEnabled() { return lowInputDelayEnabled; }
    public void setLowInputDelayEnabled(boolean enabled) { this.lowInputDelayEnabled = enabled; autoSave(); }

    public boolean isCrashUploadEnabled() { return crashUploadEnabled != null && crashUploadEnabled; }
    public void setCrashUploadEnabled(boolean enabled) { this.crashUploadEnabled = enabled; autoSave(); }

    public boolean isUsageStatsEnabled() { return usageStatsEnabled != null && usageStatsEnabled; }
    public void setUsageStatsEnabled(boolean enabled) { this.usageStatsEnabled = enabled; autoSave(); }

    public boolean isForegroundServiceEnabled() { return foregroundServiceEnabled; }
    public void setForegroundServiceEnabled(boolean enabled) { this.foregroundServiceEnabled = enabled; autoSave(); }


    /**
     * Whether the FPS optimization module may quiet the host during a session.
     *
     * <p>Off by default, like anti stick drift: enabling it changes how the launcher behaves on
     * every launch, so an existing setup cannot be altered silently. It only ever reduces
     * launcher-side work during a session -- see {@code FpsOptimizer} for what that does and,
     * more importantly, what it deliberately does not do.
     */
    public boolean isFpsOptimizerEnabled() { return fpsOptimizerEnabled; }
    public void setFpsOptimizerEnabled(boolean enabled) { this.fpsOptimizerEnabled = enabled; autoSave(); }

    /**
     * The last top-level destination the user visited, or null when there is none.
     *
     * <p>Persisting the tab is what makes the launcher reopen where the user left off instead of
     * always resetting to Play, which matters most on a device where the launcher is used as the
     * game's own front end. The value is a {@code LauncherTab} name so it survives a renumbering
     * of resource ids, and an unknown name is treated as no preference rather than a failure.
     */
    public String getLastNavTab() { return lastNavTab; }

    public void setLastNavTab(String tabName) { this.lastNavTab = tabName; autoSave(); }

    /** Whether the wide rail is folded to icons only. */
    public boolean isNavRailCollapsed() { return navRailCollapsed; }

    public void setNavRailCollapsed(boolean collapsed) {
        this.navRailCollapsed = collapsed;
        autoSave();
    }

    // --- Game Performance Mode accessors ---------------------------------------------------

    public boolean isOptifineModeEnabled() { return optifineModeEnabled; }
    public void setOptifineModeEnabled(boolean enabled) {
        this.optifineModeEnabled = enabled;
        autoSave();
    }

    /**
     * Whether one optifine item's own toggle is on, by stable item id.
     *
     * <p>An unknown id reads as off, so a settings file written by a newer build cannot turn on
     * an item this build does not implement.
     */
    public boolean isOptifineItemEnabled(String itemId) {
        switch (itemId == null ? "" : itemId) {
            case "allocator": return optifineAllocatorEnabled;
            case "render_priority": return optifineRenderPriorityEnabled;
            case "cpu_affinity": return optifineCpuAffinityEnabled;
            case "refresh_rate": return optifineRefreshRateEnabled;
            case "entity_culling": return optifineEntityCullingEnabled;
            case "particle_culling": return optifineParticleCullingEnabled;
            case "dynamic_render_distance": return optifineRenderDistanceEnabled;
            case "callback_trimming": return optifineCallbackTrimmingEnabled;
            case "oreui_stripping": return optifineOreUiStrippingEnabled;
            default: return false;
        }
    }

    public void setOptifineItemEnabled(String itemId, boolean enabled) {
        switch (itemId == null ? "" : itemId) {
            case "allocator": optifineAllocatorEnabled = enabled; break;
            case "render_priority": optifineRenderPriorityEnabled = enabled; break;
            case "cpu_affinity": optifineCpuAffinityEnabled = enabled; break;
            case "refresh_rate": optifineRefreshRateEnabled = enabled; break;
            case "entity_culling": optifineEntityCullingEnabled = enabled; break;
            case "particle_culling": optifineParticleCullingEnabled = enabled; break;
            case "dynamic_render_distance": optifineRenderDistanceEnabled = enabled; break;
            case "callback_trimming": optifineCallbackTrimmingEnabled = enabled; break;
            case "oreui_stripping": optifineOreUiStrippingEnabled = enabled; break;
            default: return;
        }
        autoSave();
    }

    /** Consecutive-crash count for an optifine item, 0 when it has not crashed. */
    public int getOptifineCrashCount(String itemId) {
        if (itemId == null || optifineCrashCounts == null) return 0;
        Integer value = optifineCrashCounts.get(itemId);
        return value == null ? 0 : value;
    }

    public void setOptifineCrashCount(String itemId, int count) {
        if (itemId == null) return;
        if (optifineCrashCounts == null) optifineCrashCounts = new java.util.HashMap<>();
        if (count <= 0) {
            optifineCrashCounts.remove(itemId);
        } else {
            optifineCrashCounts.put(itemId, count);
        }
        autoSave();
    }

    /** Clears every optifine crash counter, e.g. after the user re-enables an item by hand. */
    public void clearOptifineCrashCounts() {
        if (optifineCrashCounts == null || optifineCrashCounts.isEmpty()) return;
        optifineCrashCounts.clear();
        autoSave();
    }

    public boolean isGuestModeEnabled() { return guestModeEnabled; }
    public void setGuestModeEnabled(boolean enabled) { this.guestModeEnabled = enabled; autoSave(); }

    public boolean isStreamingSafeModeEnabled() { return streamingSafeModeEnabled; }
    public void setStreamingSafeModeEnabled(boolean enabled) { this.streamingSafeModeEnabled = enabled; autoSave(); }

    /**
     * How many instance backups to keep per instance. {@code 0} means "use the default"
     * ({@link org.chimeramc.client.util.BackupRetention#DEFAULT_KEEP}); the rule itself clamps the
     * value, so a bad stored number cannot leave thousands of archives on disk.
     */
    private int backupRetentionKeep = 0;

    public int getBackupRetentionKeep() {
        return org.chimeramc.client.util.BackupRetention.clampKeep(backupRetentionKeep);
    }

    public void setBackupRetentionKeep(int keep) {
        int clamped = org.chimeramc.client.util.BackupRetention.clampKeep(keep);
        if (this.backupRetentionKeep == clamped) return;
        this.backupRetentionKeep = clamped;
        autoSave();
    }

    public boolean isReduceNetworkLatencyEnabled() { return reduceNetworkLatencyEnabled; }
    public void setReduceNetworkLatencyEnabled(boolean enabled) {
        if (this.reduceNetworkLatencyEnabled == enabled) return;
        this.reduceNetworkLatencyEnabled = enabled;
        autoSave();
        if (enabled) {
            LowLatencyNetworkManager.prefetchDnsOnBackground();
        }
    }


    private void autoSave() {
        if (appContext != null) {
            SettingsStorage.save(appContext, this);
        }
    }
}

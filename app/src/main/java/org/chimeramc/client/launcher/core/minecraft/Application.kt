package org.chimeramc.client.core.minecraft

import org.chimeramc.client.util.AppLog

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import org.chimeramc.client.core.crash.CrashReporter
import org.chimeramc.client.settings.FeatureSettings
import org.chimeramc.client.settings.LowLatencyNetworkManager
import org.chimeramc.client.settings.ThermalGovernor
import org.chimeramc.client.ui.dialogs.LogcatOverlayManager

class LauncherApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        // Point the failure reporter at the application context before anything else can fail, so
        // the first failure is durable rather than only reaching logcat.
        org.chimeramc.client.util.AppLog.install(applicationContext)
        // Catch main-thread disk/network work in debug builds. A no-op in release.
        org.chimeramc.client.util.StrictModeSetup.install(org.chimeramc.client.BuildConfig.DEBUG)
        // The critical path: only what a screen drawn on the first frame reads. Everything else is
        // deferred below, because running it here blocked the first frame (the startup-time report).
        FeatureSettings.init(applicationContext)
        CrashReporter.init(this)
        // One table a script or a module can ask before it draws, so a feature whose
        // sweetloader hook did not resolve is skipped rather than looking like a broken feature. Each
        // probe is a cached native read, so this is cheap enough to leave installed for the process.
        org.chimeramc.client.core.backend.HookAvailability.install { hook ->
            when (hook) {
                org.chimeramc.client.core.backend.HookAvailability.Hook.PLAYER_RENDER ->
                    org.chimeramc.client.preloader.PreloaderInput.isPlayerRenderHookLive()
                org.chimeramc.client.core.backend.HookAvailability.Hook.MCE_IMAGE ->
                    org.chimeramc.client.preloader.PreloaderInput.isMceImageHookLive()
                org.chimeramc.client.core.backend.HookAvailability.Hook.COSMETICS_RENDER ->
                    org.chimeramc.client.preloader.PreloaderInput.isCosmeticsHookLive()
                org.chimeramc.client.core.backend.HookAvailability.Hook.LOCAL_PLAYER ->
                    org.chimeramc.client.preloader.PreloaderInput.isLocalPlayerAvailable()
                org.chimeramc.client.core.backend.HookAvailability.Hook.TEXTURE_FLUSH ->
                    org.chimeramc.client.preloader.PreloaderInput.isTextureCacheFlushAvailable()
                org.chimeramc.client.core.backend.HookAvailability.Hook.PRESENT ->
                    org.chimeramc.client.preloader.PreloaderInput.isPresentHookLive()
            }
        }
        // Send any crash report captured by a previous run, but only when the user's own
        // "Crash report upload" switch is on and this build has a collector configured. Off the
        // main thread: it reads files and performs a network request, and a launcher that stalls
        // on startup to report an old crash has the priorities backwards.
        Thread({
            try {
                org.chimeramc.client.core.crash.CrashReportUploader.uploadPending(applicationContext)
            } catch (t: Throwable) {
                AppLog.warnOnce("CaramelClient", "Application:59", "Ignored a failure at Application:59", t)
            }
        }, "crash-report-upload").apply { isDaemon = true }.start()
        // The opt-in anonymous usage counter. Same shape as the crash report above --
        // off the main thread, gated on the user's switch and a configured collector, and inert in
        // the shipped source because no endpoint is set. At most one report a day.
        Thread({
            try {
                org.chimeramc.client.core.telemetry.UsageStatsReporter
                    .reportIfDue(applicationContext)
            } catch (t: Throwable) {
                AppLog.warnOnce("CaramelClient", "Application:70", "Ignored a failure at Application:70", t)
            }
        }, "usage-stats").apply { isDaemon = true }.start()
        // Mirror the persisted haptics preference into the static feedback layer so the
        // very first interaction honours it, before Settings is ever opened.
        org.chimeramc.client.ui.animation.UiTouchFeedback.setEnabled(
            org.chimeramc.client.util.PersonalizationManager(applicationContext)
                .isHapticFeedbackEnabled()
        )
        // A migrated instance whose storage-migration foreground service was killed mid-run leaves
        // the running flag set; a fresh process is proof the run died, so record the interruption
        // and clear the flag before the launcher can blind-resume it on the next launch.
        org.chimeramc.client.util.StorageMigrationService.reconcileOnProcessStart(applicationContext)

        val processName = Application.getProcessName()
        if (processName.endsWith(":crash")) return

        LogcatOverlayManager.init(this)
        PlaytimeManager.init(applicationContext)
        // The in-game pack changer applies a change by relaunching the running instance; install
        // the relaunch hook once here so it is present whenever a session is live, independent of
        // whether any particular overlay has been opened.
        MinecraftSessionRestarter.install()
        // The Replay recorder's state lives in a process singleton so a screen opened mid-capture
        // sees the running state; init here so it exists before any Mod Menu screen asks for it.
        org.chimeramc.client.core.replay.ReplayManager.init(applicationContext)

        // Boot the native cosmetics system once: publish the equipped set into the native registry
        // and install the sink that renders a peer's advertised cosmetics. There is no resource pack
        // path any more, so this is the single initialisation point.
        org.chimeramc.client.core.cosmetics.NativeCosmeticsRuntime.init(applicationContext)

        preferences = PreferenceManager.getDefaultSharedPreferences(this)

        // Everything the first frame does not read, off the main thread. Each of these is read only
        // when its own feature is used, so running it late is invisible; running it on the main
        // thread before the first frame is what made the start slow. Posted after the main thread has
        // had a chance to draw, so the launcher's first screen is not competing with it.
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            org.chimeramc.client.util.AppExecutors.run(
                org.chimeramc.client.util.AppExecutors.Kind.DISK, "startup-deferred",
            ) {
                runDeferredStartup()
            }
        }
    }

    /**
     * The startup work that does not have to finish before the first frame.
     *
     * <p>Runs on the shared disk executor after the main thread has drawn. Each step is wrapped so one
     * failure cannot stop the rest: a feature whose init fails is degraded, not a reason for the app
     * to be missing every other feature. The classification is {@code StartupPhases}, which is
     * unit-tested, so a new init step has to say which phase it belongs to.
     */
    private fun runDeferredStartup() {
        fun step(name: String, work: () -> Unit) {
            try {
                work()
            } catch (t: Throwable) {
                org.chimeramc.client.util.AppLog.warnOnce(
                    "Application", "deferred:$name", "Deferred startup step failed", t,
                )
            }
        }
        step("low_latency_network") {
            org.chimeramc.client.settings.LowLatencyNetworkManager
                .init(applicationContext)
        }
        step("thermal_governor") {
            org.chimeramc.client.settings.ThermalGovernor.init(applicationContext)
        }
        step("fps_optimizer") {
            org.chimeramc.client.core.minecraft.FpsOptimizationService.init(applicationContext)
        }
        step("fps_modules") {
            org.chimeramc.client.core.mods.fps.FpsModulesService.init(applicationContext)
        }
        step("optifine_reconcile") {
            org.chimeramc.client.core.minecraft.OptifineModeManager
                .reconcileOnStartup(applicationContext)
        }
        // Keep the account skin in sync: if the cache is missing, for another account, or older than
        // the trust window, re-fetch it in the background. Off the main thread because it reads the
        // cache, and off the critical path because the first frame does not need it.
        step("account_skin_sync") {
            org.chimeramc.client.core.cosmetics.AccountSkinService
                .refreshIfStale(applicationContext)
        }
    }

    /**
     * Feeds the OS memory-pressure signal to the FPS optimizer.
     *
     * <p>{@code onTrimMemory} is the only place the platform tells the host it is being squeezed,
     * and the optimizer treats it as one of the reasons to shed background work during a session.
     * The constant is {@code ComponentCallbacks2.TRIM_MEMORY_RUNNING_*} — referencing the int is
     * safe on minSdk 28 because it is a compile-time constant, not a type.
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        val critical = level >= TRIM_MEMORY_RUNNING_CRITICAL
        org.chimeramc.client.core.minecraft.FpsOptimizationService.setLowMemory(critical)
        if (critical) {
            org.chimeramc.client.core.minecraft.FpsOptimizationService.apply()
            // Bound the sweetloader's cached module handles: a hot-reloading or heavily-modded session
            // grows that cache without bound, and trim-memory is exactly when to reclaim it. No-op
            // when the library is absent.
            org.chimeramc.client.preloader.PreloaderInput.closeUnusedModuleHandles()
        }
    }

    companion object {
        @JvmStatic
        lateinit var context: Context
            private set

        @JvmStatic
        lateinit var preferences: SharedPreferences
            private set
    }
}

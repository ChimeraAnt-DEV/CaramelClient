package org.chimeramc.client.core.minecraft

import org.chimeramc.client.util.AppLog


import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import org.chimeramc.client.R
import org.chimeramc.client.core.minecraft.LaunchLogBuffer
import org.chimeramc.client.launcher.core.minecraft.LastLaunchFailure
import org.chimeramc.client.launcher.core.minecraft.LaunchErrorDiagnosis
import org.chimeramc.client.ui.animation.DynamicAnim
import org.chimeramc.client.ui.activities.BaseActivity
import org.chimeramc.client.ui.dialogs.CustomAlertDialog
import org.chimeramc.client.util.PersonalizationManager
import org.chimeramc.client.util.TextContrast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MinecraftLoadingActivity : BaseActivity(), MinecraftRuntimePreparer.ProgressListener {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    private lateinit var binding: org.chimeramc.client.databinding.ActivityMinecraftLoadingBinding
    private lateinit var ring: org.chimeramc.client.launcher.ui.views.LaunchRingView
    private lateinit var logContainer: android.view.View
    private var launchFailed = false
    private var launchReady = false
    private lateinit var statusView: TextView
    private lateinit var detailView: TextView
    private lateinit var logView: TextView
    private lateinit var logScroll: ScrollView
    private lateinit var returnButton: Button
    private lateinit var trace: LaunchTrace

    @Volatile
    private var returningToLauncher = false
    private var preparingStarted = false
    private var firstFrameFallback: Runnable? = null
    private var enteringGame = false
    // The launch watchdog. It tracks the last sign of progress and, when a launch goes
    // silent for too long, offers the two actions that actually help instead of an endless spinner.
    private var launchStartedMs = 0L
    private var lastProgressMs = 0L
    private var watchdogScheduled = false
    private var watchdogDialog: CustomAlertDialog? = null
    private var progressAnimator: ValueAnimator? = null
    private var currentProgress = 0
    private var lastLogMessage: String? = null
    private var logRenderScheduled = false
    private var statusRenderTask: Runnable? = null
    private var gameStartDispatched = false
    private var gameStartPreDrawListener: ViewTreeObserver.OnPreDrawListener? = null
    // Time to the loading screen's first drawn frame, or NO_FIRST_FRAME when it was never observed
    // (the fallback path fired before a draw). Fed into the local statistics.
    private var firstFrameMs = org.chimeramc.client.core.stats.LaunchStats.NO_FIRST_FRAME
    private var lastRenderedStatus: String? = null
    private var lastRenderedDetail: String? = null
    // The cap and the render live in a pure, tested class; the view is assigned its whole output
    // so the two can never disagree about how many lines exist.
    private val launchLog = LaunchLogBuffer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        trace = LaunchTrace.ensure(intent)
        trace.mark("MinecraftLoadingActivity onCreate")
        launchStartedMs = android.os.SystemClock.elapsedRealtime()
        lastProgressMs = launchStartedMs
        applyLaunchOrientation()
        showSystemUI()
        window.decorView.setOnSystemUiVisibilityChangeListener {
            if (!isFinishing && !isDestroyed && !returningToLauncher) {
                window.decorView.post { showSystemUI() }
            }
        }
        binding = org.chimeramc.client.databinding.ActivityMinecraftLoadingBinding
                .inflate(layoutInflater)
        setContentView(binding.root)
        showSystemUI()

        // View binding, not findViewById: the hygiene gate holds the per-file findViewById count,
        // and the binding is also type-safe, so a renamed id is a compile error rather than a null.
        ring = binding.minecraftLaunchRing
        logContainer = binding.minecraftLaunchLogContainer
        // The raw log is opt-in; the default launch screen is the logo, the status and the ring.
        logContainer.visibility =
                if (org.chimeramc.client.settings.FeatureSettings.getInstance()
                        .isLaunchLogEnabled()) android.view.View.VISIBLE else android.view.View.GONE
        statusView = binding.minecraftLaunchStatus
        detailView = binding.minecraftLaunchDetail
        renderChangelogLine(binding.minecraftLaunchChangelog)
        logView = binding.minecraftLaunchLog
        logScroll = binding.minecraftLaunchLogScroll
        returnButton = binding.minecraftLaunchReturn

        returnButton.setOnClickListener {
            appendLog("Returning to launcher")
            returnToLauncher()
        }

        applyLaunchTheme()

        MinecraftLaunchSession.clear()
        // Count the launch the moment the launch screen is reached. This is the widest
        // point a launch can be observed from -- before it can fail in runtime preparation -- so a
        // failure later still leaves the launch counted.
        org.chimeramc.client.core.stats.LaunchStatsRecorder.recordLaunch(this)
        trace.milestone("Launch screen ready")
        appendLog("Preparing launch")
        appendLog("Preparing game files")
        startPreparingAfterFirstDraw()
    }

    override fun shouldSkipNavBar(): Boolean = false

    override fun onResume() {
        super.onResume()
        showSystemUI()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            showSystemUI()
        }
    }

    private fun startPreparingAfterFirstDraw() {
        val content = window.decorView
        content.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (content.viewTreeObserver.isAlive) {
                    content.viewTreeObserver.removeOnPreDrawListener(this)
                }
                // The loading screen's first draw is the only "first frame" the launcher can time
                // for a launch; the game's own first frame is not observable from here.
                firstFrameMs = trace.elapsedMs()
                content.post {
                    showSystemUI()
                    startPreparingOnce("loading first frame drawn")
                }
                return true
            }
        })
        // Held so onDestroy can cancel it. View.postDelayed goes through the ViewRootImpl handler,
        // not the one this activity owns, so mainHandler.removeCallbacksAndMessages in onDestroy
        // does not reach it - a screen destroyed inside the fallback window would still fire it and
        // submit to the executor after it was shut down.
        firstFrameFallback = Runnable {
            showSystemUI()
            startPreparingOnce("loading first frame fallback")
        }
        content.postDelayed(firstFrameFallback!!, FIRST_FRAME_FALLBACK_MS)
    }

    private fun startPreparingOnce(reason: String) {
        // A late first-frame callback (or the fallback) can arrive after the screen is gone; the
        // preparation must not start then, and must never submit to a shut-down executor.
        if (preparingStarted || isFinishing || isDestroyed) return
        preparingStarted = true
        trace.milestone("Runtime preparation continuing")
        appendLog("Preparing game")
        scheduleWatchdog()
        startPreparing()
    }

    private fun applyLaunchOrientation() {
        val version = MinecraftRuntimePreparer.resolveGameVersion(intent)
        val launchVertically = version?.launchVertically
            ?: intent.getBooleanExtra("LAUNCH_VERTICALLY", false)
        if (launchVertically) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    private fun startPreparing() {
        if (executor.isShutdown || executor.isTerminated) return
        try {
            executor.execute { prepareAndEnterGame() }
        } catch (rejected: java.util.concurrent.RejectedExecutionException) {
            // The screen was destroyed between the check and the submit; there is nobody left to
            // report to and nothing to launch.
            trace.error("Runtime preparation could not be scheduled", rejected.message)
        }
    }

    private fun prepareAndEnterGame() {
        try {
            val gameIntent = Intent(intent).apply {
                setClass(this@MinecraftLoadingActivity, MinecraftActivity::class.java)
            }
            val preparedRuntime = MinecraftRuntimePreparer.prepare(this, gameIntent, this)
            MinecraftLaunchSession.setPreparedRuntime(preparedRuntime)

            mainHandler.post {
                if (returningToLauncher || isFinishing || isDestroyed) return@post
                launchReady = true
                updateProgress(
                    100,
                    getString(R.string.minecraft_loading_complete),
                    getString(R.string.minecraft_loading_entering_game)
                )
                trace.milestone("Entering game")
                // The launch reached the game, so it counts as a completed launch and its first-frame
                // time joins the local mean.
                org.chimeramc.client.core.stats.LaunchStatsRecorder.recordCompletedLaunch(
                    this, firstFrameMs)
                appendLog("Entering Minecraft")
                // Starting the game activity is the last thing that can fail, and it failed
                // outside any guard: a throw from startActivity (a missing target, a
                // TransactionTooLarge, an OOM while the game activity inflated) unwound through
                // the main thread and killed the process, so the user saw the app vanish with
                // no message at all. It is contained here and reported through the same
                // cause-naming path every other launch failure uses.
                try {
                    showSkippedIncompatibleModsIfNeeded(preparedRuntime, gameIntent)
                } catch (throwable: Throwable) {
                    showFailure(throwable)
                }
            }
        } catch (cancelled: org.chimeramc.client.core.minecraft.ExtractionControl.ExtractionCancelledException) {
            // The user gave up on the launch while it was extracting. That is not a failure to
            // report: the loading screen is already on its way back to the launcher, and showing
            // a "launch failed" dialog on top of it would read as a bug.
            appendLog("Extraction cancelled")
        } catch (throwable: Throwable) {
            mainHandler.post {
                showFailure(throwable)
            }
        }
    }

    override fun onProgress(progress: Int, status: String, detail: String?) {
        mainHandler.post {
            lastProgressMs = android.os.SystemClock.elapsedRealtime()
            updateProgress(progress, status, detail)
        }
    }

    override fun onLog(message: String) {
        mainHandler.post {
            lastProgressMs = android.os.SystemClock.elapsedRealtime()
            appendLog(message)
        }
    }

    /**
     * Arms the launch watchdog.
     *
     * <p>One wake-up is scheduled at the next threshold the pure rule reports, rather than polling
     * every frame, so a launch that is progressing costs a single timer. Each call reschedules from
     * the new anchor, so progress pushes the deadline out instead of firing on a fixed wall clock.
     */
    private fun scheduleWatchdog() {
        if (watchdogScheduled || isFinishing || isDestroyed) return
        watchdogScheduled = true
        val now = android.os.SystemClock.elapsedRealtime()
        val at = org.chimeramc.client.core.minecraft.LaunchWatchdog.nextCheckAt(
            launchStartedMs, lastProgressMs, enteringGame, now)
        if (at == Long.MAX_VALUE) {
            watchdogScheduled = false
            return
        }
        val delay = (at - now).coerceAtLeast(0L)
        mainHandler.postDelayed({
            watchdogScheduled = false
            onWatchdogTick()
        }, delay)
    }

    private fun onWatchdogTick() {
        if (isFinishing || isDestroyed || returningToLauncher || enteringGame) return
        val now = android.os.SystemClock.elapsedRealtime()
        val state = org.chimeramc.client.core.minecraft.LaunchWatchdog.evaluate(
            launchStartedMs, lastProgressMs, enteringGame, now)
        if (org.chimeramc.client.core.minecraft.LaunchWatchdog.isActionable(state)) {
            showWatchdogDialog(state)
        } else {
            scheduleWatchdog()
        }
    }

    private fun showWatchdogDialog(state: org.chimeramc.client.core.minecraft.LaunchWatchdog.State) {
        if (watchdogDialog != null || isFinishing || isDestroyed) return
        val dead = state == org.chimeramc.client.core.minecraft.LaunchWatchdog.State.DEAD
        val dialog = CustomAlertDialog(this)
            .setTitleText(getString(R.string.minecraft))
            .setMessage(getString(if (dead) R.string.launch_watchdog_dead else R.string.launch_watchdog_stalled))
            .setBlurBackground(true)
            .setPositiveButton(getString(R.string.launch_watchdog_open_logs)) {
                watchdogDialog = null
                showLogcat()
            }
            .setNegativeButton(getString(if (dead) R.string.launch_watchdog_restart_without_mods else R.string.dialog_negative_cancel)) {
                watchdogDialog = null
                if (dead) restartWithoutMods() else scheduleWatchdog()
            }
        dialog.setCancelable(false)
        watchdogDialog = dialog
        dialog.show()
        appendLog("Launch watchdog: " + state.name)
    }

    /** Opens the in-app log viewer so the user can see what the launch is stuck on. */
    private fun showLogcat() {
        try {
            startActivity(Intent(this, org.chimeramc.client.launcher.ui.activities.DiagnosticsActivity::class.java))
        } catch (ignored: Throwable) {
            AppLog.warnOnce("CaramelClient", "MinecraftLoadingActivity:showLogcat", "Ignored a failure in showLogcat", ignored)
        }
        scheduleWatchdog()
    }

    /** Returns to the launcher with the current instance's mods disabled, to rule a mod out. */
    private fun restartWithoutMods() {
        try {
            org.chimeramc.client.core.mods.ModSafeMode.suppressModsForNextLaunch(this)
        } catch (ignored: Throwable) {
            AppLog.warnOnce("CaramelClient", "MinecraftLoadingActivity:restartWithoutMods", "Ignored a failure in restartWithoutMods", ignored)
        }
        appendLog("Restarting without mods")
        returnToLauncher()
    }

    /**
     * Shows the newest and upcoming changelog headlines on the launch screen.
     *
     * <p>The launch screen is the one place every player passes through on every launch, so it is
     * where "what changed" and "what is next" actually get read. The line is the two headlines
     * joined; the full changelog screen is still reachable from Settings. The read is off the UI
     * thread, and an absent feed simply leaves the line hidden.
     */
    private fun renderChangelogLine(view: android.widget.TextView?) {
        if (view == null) return
        Thread({
            val line = try {
                assets.open("launcher/changelog_feed.json").use { input ->
                    val text = input.readBytes().toString(Charsets.UTF_8)
                    org.chimeramc.client.core.changelog.ChangelogFeed.parse(text).headlineLine()
                }
            } catch (t: Throwable) {
                ""
            }
            if (line.isEmpty()) return@Thread
            mainHandler.post {
                if (isFinishing || isDestroyed) return@post
                view.text = line
                view.visibility = android.view.View.VISIBLE
            }
        }, "changelog-line").apply { isDaemon = true }.start()
    }

    private fun updateProgress(progress: Int, status: String, detail: String?) {
        if (isFinishing || isDestroyed) return
        val clamped = progress.coerceIn(0, 100)
        if (::ring.isInitialized) ring.setState(launchFailed, launchReady, clamped)
        // The final update is painted synchronously. Debouncing it meant the game activity took the
        // main thread 100 ms before it could render, so the last text the user ever saw was the
        // previous one ("Loading native mods") while the screen actually froze in the game.
        if (clamped >= 100) {
            renderStatusNow(status, detail)
        } else {
            scheduleStatusRender(status, detail)
        }
    }

    private fun scheduleStatusRender(status: String, detail: String?) {
        if (isFinishing || isDestroyed) return
        if (status == lastRenderedStatus && detail == lastRenderedDetail) return
        lastRenderedStatus = status
        lastRenderedDetail = detail
        // Cancel the pending render so a later status cannot be overwritten by an older one that
        // was still queued.
        statusRenderTask?.let { mainHandler.removeCallbacks(it) }
        val task = Runnable {
            statusRenderTask = null
            if (isFinishing || isDestroyed) return@Runnable
            statusView.text = status
            detailView.text = detail.orEmpty()
        }
        statusRenderTask = task
        mainHandler.postDelayed(task, STATUS_RENDER_DEBOUNCE_MS)
    }

    /** Paints the status immediately, cancelling any debounced render still queued. */
    private fun renderStatusNow(status: String, detail: String?) {
        if (isFinishing || isDestroyed) return
        statusRenderTask?.let { mainHandler.removeCallbacks(it) }
        statusRenderTask = null
        lastRenderedStatus = status
        lastRenderedDetail = detail
        statusView.text = status
        detailView.text = detail.orEmpty()
    }

    private fun appendLog(message: String) {
        if (isFinishing || isDestroyed) return
        if (message == lastLogMessage) return
        val timestamp = timeFormat.format(Date())
        lastLogMessage = message
        launchLog.add("[$timestamp] ${trace.formatForUi(message)}")
        scheduleLogRender()
    }

    private fun scheduleLogRender() {
        if (logRenderScheduled) return
        logRenderScheduled = true
        mainHandler.postDelayed({
            logRenderScheduled = false
            if (isFinishing || isDestroyed) return@postDelayed
            // The buffer mirrors what should be on screen, so it is re-rendered wholesale. Appending
            // only the newly arrived lines treated it as a delta queue, which left the line cap
            // bounding the *buffer* and not the view: the TextView accumulated every line of the
            // whole launch with no ceiling, so a slow extraction grew it into a long-layout,
            // high-memory scroll that also duplicated lines when a render drained the queue.
            logView.text = launchLog.render()
            logScroll.post { logScroll.fullScroll(ScrollView.FOCUS_DOWN) }
        }, LOG_RENDER_DEBOUNCE_MS)
    }

    private fun showFailure(throwable: Throwable) {
        if (isFinishing || isDestroyed) return
        val message = throwable.message ?: throwable.javaClass.simpleName
        // Name the cause, then the raw text. This is the screen a launch actually dies on, so it is
        // where "dlopen failed: library \"libminecraftpe.so\" not found" has to become "this is a
        // Google Play version or a version missing a file needed to launch the game".
        val cause = LaunchErrorDiagnosis.classify(throwable)
        val causeLine = getString(LaunchErrorDiagnosis.messageRes(cause))
        launchFailed = true
        updateProgress(100, getString(LaunchErrorDiagnosis.titleRes(cause)), null)
        detailView.visibility = View.GONE
        trace.error("Launch failed", message)
        appendLog("Launch failed")
        appendLog(getString(R.string.launch_error_cause_label) + " " + LaunchErrorDiagnosis.label(cause))
        appendLog(causeLine)
        appendLog(message)
        // The user is looking at the cause on this screen, so the launcher must not repeat it after
        // the restart. The report is stored as announced and any earlier pending report is dropped:
        // this screen is authoritative whenever it can be shown.
        LastLaunchFailure.write(this, throwable, true)
        // A launch that died here never reached a session, so it is a failed launch in the local
        // statistics.
        org.chimeramc.client.core.stats.LaunchStatsRecorder.recordCrash(this)
        showReturnButton()
    }

    /**
     * Reveals the "Back" button with a pop-up entrance.
     *
     * <p>The button carries no text in the layout and was shown with a bare VISIBLE, so on a failed
     * launch the user saw an empty pill with no way back. It now takes its label from the shared
     * {@code back} string and scales in, so it reads as a control arriving rather than a control
     * that was always there. {@code overshootScaleIn} honours reduced motion, so the pop-up is
     * skipped when animations are off.
     */
    private fun showReturnButton() {
        if (isFinishing || isDestroyed) return
        returnButton.setText(R.string.back)
        returnButton.visibility = View.VISIBLE
        DynamicAnim.overshootScaleIn(returnButton, 0L)
    }

    private fun animateProgressTo(targetProgress: Int) {
        if (currentProgress == targetProgress) return
        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofInt(currentProgress, targetProgress).apply {
            duration = PROGRESS_ANIMATION_MS
            addUpdateListener { animator ->
                currentProgress = animator.animatedValue as Int
                if (::ring.isInitialized) ring.setState(launchFailed, launchReady, currentProgress)
            }
            start()
        }
    }

    private fun applyLaunchTheme() {
        val configuredAccent = PersonalizationManager(this).getAccentColor()
        val accent = if (configuredAccent != 0) configuredAccent else ContextCompat.getColor(this, R.color.primary)
        val accentTint = ColorStateList.valueOf(accent)
        val secondaryText = ContextCompat.getColor(this, R.color.text_secondary)

        // The ring owns its own colour from the launch phase; only the log surface is themed here.

        logScroll.background = GradientDrawable().apply {
            cornerRadius = dp(8).toFloat()
            setColor(if (isDarkMode()) Color.argb(238, 16, 20, 18) else ContextCompat.getColor(this@MinecraftLoadingActivity, R.color.surface))
            setStroke(dp(1), withAlpha(accent, if (isDarkMode()) 88 else 120))
        }
        logView.setTextColor(blendColors(ContextCompat.getColor(this, R.color.on_background), accent, if (isDarkMode()) 0.10f else 0.18f))
        detailView.setTextColor(blendColors(secondaryText, accent, 0.18f))
        returnButton.backgroundTintList = accentTint
        // The label is chosen from the fill: the theme's fixed colour vanishes into a light accent
        // (white on white), which is what left the button looking like it had no text.
        returnButton.setTextColor(TextContrast.on(accent))
    }

    private fun isDarkMode(): Boolean {
        val mode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return mode == Configuration.UI_MODE_NIGHT_YES
    }

    private fun withAlpha(color: Int, alpha: Int): Int {
        return Color.argb(
            alpha.coerceIn(0, 255),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }

    private fun blendColors(from: Int, to: Int, ratio: Float): Int {
        val boundedRatio = ratio.coerceIn(0f, 1f)
        val inverse = 1f - boundedRatio
        return Color.rgb(
            (Color.red(from) * inverse + Color.red(to) * boundedRatio).toInt(),
            (Color.green(from) * inverse + Color.green(to) * boundedRatio).toInt(),
            (Color.blue(from) * inverse + Color.blue(to) * boundedRatio).toInt()
        )
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density + 0.5f).toInt()
    }

    private fun transitionToGame(gameIntent: Intent) {
        if (enteringGame) return
        // The guards run before the latch is set. Setting it first meant a call that arrived while
        // the screen was finishing latched `enteringGame` and returned without starting anything,
        // so the retry from the incompatible-mods dialog could never transition: the game simply
        // never opened.
        if (returningToLauncher || isFinishing || isDestroyed) return
        enteringGame = true
        // The final status was just written; hand the main thread to the game activity only after
        // that text has been drawn. Calling startActivity() in the same message meant the game
        // activity's onCreate ran before the frame, so the screen froze showing the previous line
        // and screenshots went black. The pre-draw callback is the earliest point the new text is
        // on screen. The timeout is a safety net for a window that never draws (paused, detached).
        val start = Runnable {
            if (gameStartDispatched) return@Runnable
            gameStartDispatched = true
            startGameActivity(gameIntent)
        }
        val decor = window?.decorView
        if (decor != null && decor.isAttachedToWindow) {
            // One-shot pre-draw: fires once, just before the frame that contains the final status.
            val observer = decor.viewTreeObserver
            val listener = object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    observer.removeOnPreDrawListener(this)
                    gameStartPreDrawListener = null
                    start.run()
                    return false
                }
            }
            gameStartPreDrawListener = listener
            observer.addOnPreDrawListener(listener)
        }
        mainHandler.postDelayed(start, GAME_START_DRAW_FALLBACK_MS)
    }

    private fun startGameActivity(gameIntent: Intent) {
        if (returningToLauncher || isFinishing || isDestroyed) {
            enteringGame = false
            return
        }
        // Starting the game is the last step and the only one that was unguarded on this path: the
        // dialog button calls straight into here with no enclosing try, so a throw from
        // startActivity (a missing target, TransactionTooLarge, an OOM while the game activity
        // inflates) unwound through the main thread and killed the process. Contained and named
        // through the same path every other launch failure uses.
        try {
            startActivity(gameIntent)
        } catch (throwable: Throwable) {
            enteringGame = false
            showFailure(throwable)
            return
        }
        finish()
        overridePendingTransition(0, 0)
    }

    private fun showSkippedIncompatibleModsIfNeeded(
        preparedRuntime: MinecraftRuntimePreparer.PreparedRuntime,
        gameIntent: Intent
    ) {
        val skippedMods = preparedRuntime.skippedIncompatibleMods
        if (skippedMods.isEmpty()) {
            transitionToGame(gameIntent)
            return
        }

        val minecraftVersion = preparedRuntime.version?.versionCode
            ?.takeIf { it.isNotBlank() }
            ?: getString(R.string.minecraft)
        val modList = skippedMods.joinToString(separator = "\n") { "- $it" }
        val dialog = CustomAlertDialog(this)
            .setTitleText(getString(R.string.dialog_title_incompatible_mods))
            .setMessage(getString(R.string.dialog_message_incompatible_mods, minecraftVersion, modList))
            .setBlurBackground(true)
            .setPositiveButton(getString(R.string.dialog_positive_ok)) {
                transitionToGame(gameIntent)
            }
        dialog.setCancelable(false)
        dialog.show()
    }

    override fun onBackPressed() {
        // A launch that is still extracting can be given up on. The signal is checked between
        // files, so the copy stops at the next safe boundary instead of running to completion for
        // a user who has already left. The instance is cleared inside returnToLauncher,
        // so a cancel that lands after the extraction finished is a no-op.
        try {
            org.chimeramc.client.core.minecraft.GamePackageManager.cancelExtraction()
        } catch (ignored: Throwable) {
            AppLog.warnOnce("CaramelClient", "MinecraftLoadingActivity:onBackPressed", "Ignored a failure in onBackPressed", ignored)
        }
        appendLog("Returning to launcher")
        returnToLauncher()
    }

    private fun returnToLauncher() {
        if (returningToLauncher) return
        returningToLauncher = true

        MinecraftLaunchSession.clear()
        MinecraftProcessRestarter.restartLauncherAfterMinecraftExit(this)
        finish()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        // The first-frame fallback is posted through the decor view's handler, so it has to be
        // cancelled explicitly or it fires into a destroyed screen.
        firstFrameFallback?.let { window.decorView.removeCallbacks(it) }
        firstFrameFallback = null
        gameStartPreDrawListener?.let {
            window?.decorView?.viewTreeObserver?.removeOnPreDrawListener(it)
        }
        gameStartPreDrawListener = null
        progressAnimator?.cancel()
        watchdogDialog?.dismiss()
        watchdogDialog = null
        executor.shutdownNow()
        super.onDestroy()
    }

    private companion object {
        private const val FIRST_FRAME_FALLBACK_MS = 240L
        private const val PROGRESS_ANIMATION_MS = 140L
        private const val LOG_RENDER_DEBOUNCE_MS = 200L
        private const val STATUS_RENDER_DEBOUNCE_MS = 100L
        /** Safety net when no pre-draw arrives (window paused or detached) before starting the game. */
        private const val GAME_START_DRAW_FALLBACK_MS = 300L
        private const val TRACK_ALPHA = 42
    }
}

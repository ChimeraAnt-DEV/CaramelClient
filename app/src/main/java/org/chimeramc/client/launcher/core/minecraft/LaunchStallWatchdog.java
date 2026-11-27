package org.chimeramc.client.launcher.core.minecraft;

import android.content.Context;

import org.chimeramc.client.core.minecraft.MinecraftProcessRestarter;
import org.chimeramc.client.util.AppLog;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Recovers from a launch that hangs on the game activity's main thread.
 *
 * <p>The loading screen's own watchdog ({@link LaunchWatchdog}) runs on the main thread, so it
 * cannot fire while that thread is blocked — which is exactly the failure the user reported: the
 * loading screen finished, the game activity opened, and the whole app froze with the nav bar gone
 * and no way out but a power cycle. This watchdog runs on its own daemon thread, so it survives a
 * blocked main thread, and its recovery action (kill the frozen process, reopen the launcher) is
 * performed by another process ({@code :crash}), so it does not depend on the frozen one either.
 *
 * <p>It is armed at the start of {@code MinecraftActivity.onCreate} — the window that opens the
 * moment the game process is in play — and settled on the game's first frame tick, which is proof
 * the engine's loop is running rather than blocked. A launch that never ticks within
 * {@link #BUDGET_MS} is treated as hung: the next launch is started with mods disabled (the
 * one-shot flag {@link org.chimeramc.client.core.mods.ModSafeMode} already provides), the cause is
 * recorded as {@link LaunchErrorDiagnosis.Cause#LAUNCH_STALLED} so the relaunched launcher can name
 * it, and the frozen process is torn down.
 *
 * <p>Deliberately armed and settled by explicit calls rather than by an activity lifecycle
 * callback: {@code onCreate} is the earliest point the freeze can begin, and a frame tick is the
 * strongest "not frozen" signal available without a device.
 */
public final class LaunchStallWatchdog {

    /**
     * How long the game may go without a frame tick before it is treated as hung.
     *
     * <p>Forty-five seconds, not minutes: a launch that has not drawn a single frame by then is
     * hung, and every extra second is a second the user stares at a black screen with no way out.
     * A working cold start on a slow device is well inside this; the signature scan and the
     * engine's own start-up are the long poles and they still land under it.
     */
    public static final long BUDGET_MS = 45_000L;

    /** How often the watchdog thread re-checks. Small enough to react promptly, cheap either way. */
    private static final long POLL_MS = 1_000L;

    private static final String TAG = "LaunchStallWatchdog";

    private static final AtomicBoolean armed = new AtomicBoolean(false);
    private static volatile boolean settled;
    private static volatile long armedAtMs;
    private static volatile Thread watchdogThread;

    private LaunchStallWatchdog() {
    }

    /**
     * Arms the watchdog for the launch that is beginning.
     *
     * <p>Idempotent per process: a second arm while one is pending is ignored, so an activity
     * recreated mid-launch cannot start a second watchdog. The timer resets only after the first
     * one settles or fires, via {@link #disarm}.
     */
    public static void arm(Context context) {
        if (context == null) return;
        if (!armed.compareAndSet(false, true)) return;
        settled = false;
        armedAtMs = android.os.SystemClock.elapsedRealtime();
        final Context appContext = context.getApplicationContext();
        watchdogThread = startWatchdogThread(() -> run(appContext));
    }

    /**
     * Creates and <em>starts</em> the watchdog thread.
     *
     * <p>{@link org.chimeramc.client.util.AppExecutors#thread(String, Runnable)} only builds an
     * unstarted daemon {@code Thread}; every caller has to start it. This method existed because
     * the watchdog did not: the recovery thread was created and never started, so the safety net
     * for exactly the frozen launch it was written for was dead code. Keeping the creation and the
     * start in one place makes that mistake impossible to repeat, and package-visible so a JVM test
     * can prove the returned thread is actually running.
     */
    static Thread startWatchdogThread(Runnable work) {
        Thread thread = org.chimeramc.client.util.AppExecutors.thread("launch-stall-watchdog", work);
        thread.start();
        return thread;
    }

    /**
     * True while the watchdog thread is alive. For diagnostics and tests.
     *
     * <p>False before the first {@link #arm} and after the thread has returned (settled, fired, or
     * disarmed).
     */
    public static boolean isRunning() {
        Thread thread = watchdogThread;
        return thread != null && thread.isAlive();
    }

    /**
     * Marks the launch as alive; called from the game's frame tick.
     *
     * <p>A tick means the engine's own loop is running, which is the strongest "not frozen" signal
     * available without a device. Note the tick is delivered on the <em>game</em> thread, not the UI
     * thread: it proves the engine loop runs, not that the activity's main thread is unblocked. It
     * does not disarm the slot — that happens on {@link #disarm} — so a later stall in the same
     * session is not silently unwatched.
     */
    public static void settle() {
        settled = true;
    }

    /**
     * Releases the watchdog so a future launch can arm it again.
     *
     * <p>Called when the game activity is destroyed. After this, {@link #arm} may start a fresh
     * watchdog for the next launch.
     */
    public static void disarm() {
        settled = true;
        armed.set(false);
        watchdogThread = null;
    }

    /** True while the watchdog is armed and has not settled. For diagnostics and tests. */
    public static boolean isArmed() {
        return armed.get() && !settled;
    }

    /**
     * The pure decision: has the budget elapsed without the launch settling?
     *
     * <p>Kept separate from the thread so the boundary is unit-tested without a device or a real
     * clock. A negative elapsed (the clock moved backwards) is never a stall.
     */
    public static boolean shouldRecover(long armedAtMs, boolean settled, long nowMs,
                                        long budgetMs) {
        if (settled) return false;
        long elapsed = nowMs - armedAtMs;
        return elapsed >= budgetMs;
    }

    private static void run(Context appContext) {
        while (!settled) {
            if (shouldRecover(armedAtMs, settled, android.os.SystemClock.elapsedRealtime(),
                    BUDGET_MS)) {
                recover(appContext);
                return;
            }
            try {
                Thread.sleep(POLL_MS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * Tears down the frozen launch and reopens the launcher without mods.
     *
     * <p>Every step is best-effort and individually guarded: the whole point is to escape a stuck
     * process, so a failure in one step must not stop the rest. The process teardown goes through
     * {@link MinecraftProcessRestarter}, whose restart screen runs in its own {@code :crash}
     * process, so it works even though this process's main thread is blocked.
     */
    private static void recover(Context appContext) {
        if (settled) return;
        settled = true;
        AppLog.warn(TAG,
                "No frame tick within " + (BUDGET_MS / 1000) + "s; recovering from the frozen launch");
        final String reason = describeStall();
        AppLog.warn(TAG, reason);
        try {
            org.chimeramc.client.core.mods.ModSafeMode.suppressModsForNextLaunch(appContext);
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "recover:suppress", "Could not suppress mods for the recovery launch", t);
        }
        try {
            LastLaunchFailure.write(appContext, new IllegalStateException(reason), false);
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "recover:report", "Could not record the launch-hang report", t);
        }
        try {
            MinecraftProcessRestarter.INSTANCE.restartLauncherAfterMinecraftExit(appContext);
        } catch (Throwable t) {
            AppLog.warnOnce(TAG, "recover:restart", "Could not restart the launcher after a hang", t);
        }
    }

    /**
     * Builds the one-line stall reason stored with the report.
     *
     * <p>The user is asked to send this line, so it names the phase the launch was last seen in and
     * the deferred hook-install state. Both reads are best-effort: the process is already wedged, and
     * a failure to describe the stall must not stop the recovery.
     */
    static String describeStall() {
        String lastStage;
        try {
            lastStage = org.chimeramc.client.core.minecraft.LaunchTrace.lastLaunchStages().describeLast();
        } catch (Throwable t) {
            lastStage = "(unavailable)";
        }
        String hookState;
        try {
            hookState = describeHookState(
                    org.chimeramc.client.core.mods.ModManager.gameHookInstallState());
        } catch (Throwable t) {
            hookState = "unknown";
        }
        return "Launch hung: the game window did not draw a frame within " + (BUDGET_MS / 1000)
                + " seconds. Last launch stage: " + lastStage
                + "; hook install state: " + hookState + ".";
    }

    /** Names the native deferred-install state (0 not armed, 1 waiting, 2 resolved). */
    static String describeHookState(int state) {
        switch (state) {
            case 0:
                return "not armed (0)";
            case 1:
                return "waiting (1)";
            case 2:
                return "resolved (2)";
            default:
                return "unknown (" + state + ")";
        }
    }
}

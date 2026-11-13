package org.chimeramc.client.core.stats;

import org.chimeramc.client.util.AppLog;

import android.content.Context;

/**
 * The single place a launch/crash statistic is recorded.
 *
 * <p>Every method here is best-effort and never throws: these counters are diagnostic, and a
 * statistic must never be the reason a launch path fails. The rules themselves live in
 * {@link LaunchStats}, so this class is only the load-mutate-save step with the Android context.
 */
public final class LaunchStatsRecorder {

    private LaunchStatsRecorder() {
    }

    /** A launch was started (the loading screen was reached). */
    public static void recordLaunch(Context context) {
        try {
            LaunchStatsStore.update(context, LaunchStats::recordLaunch);
        } catch (Throwable ignored) {
            AppLog.warnOnce("CaramelClient", "LaunchStatsRecorder:24", "Ignored a failure at LaunchStatsRecorder:24", ignored);
        }
    }

    /**
     * A launch reached the game.
     *
     * @param firstFrameMs the time from launch start to the first drawn frame, or a negative value
     *                     when it was never observed. The negative sentinel is what keeps a missing
     *                     sample out of the mean instead of counting as a zero.
     */
    public static void recordCompletedLaunch(Context context, long firstFrameMs) {
        try {
            LaunchStatsStore.update(context, stats -> {
                stats.recordCompletedLaunch();
                stats.withFirstFrame(firstFrameMs);
            });
        } catch (Throwable ignored) {
            AppLog.warnOnce("CaramelClient", "LaunchStatsRecorder:42", "Ignored a failure at LaunchStatsRecorder:42", ignored);
        }
    }

    /** A launch failed before a session was recorded. */
    public static void recordCrash(Context context) {
        try {
            LaunchStatsStore.update(context, LaunchStats::recordCrash);
        } catch (Throwable ignored) {
            AppLog.warnOnce("CaramelClient", "LaunchStatsRecorder:51", "Ignored a failure at LaunchStatsRecorder:51", ignored);
        }
    }

    /** The statistics as stored, or an empty record. Never null. */
    public static LaunchStats load(Context context) {
        return LaunchStatsStore.load(context);
    }

    /** Clears every stored counter. */
    public static void clear(Context context) {
        LaunchStatsStore.clear(context);
    }
}

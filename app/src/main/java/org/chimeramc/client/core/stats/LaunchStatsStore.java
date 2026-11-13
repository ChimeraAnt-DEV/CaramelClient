package org.chimeramc.client.core.stats;

import org.chimeramc.client.util.AppLog;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Persists {@link LaunchStats} locally.
 *
 * <p>Kept deliberately separate from {@link LaunchStats}: the counters and their arithmetic are
 * pure and unit-tested, while this class is the small amount of Android code that reads and writes
 * them. The store keeps raw counts and a running sum, so a future version can recompute the mean
 * rather than inherit a rounded one.
 *
 * <p>Every write is {@code apply()}, never {@code commit()}: these counters are best-effort local
 * statistics, and blocking the caller (often the main thread, on a launch) to guarantee a durable
 * count is the wrong trade. A lost update costs one launch in a statistic, not a launch.
 */
public final class LaunchStatsStore {

    private static final String PREFS = "launch_stats";
    private static final String KEY_LAUNCHES = "launches";
    private static final String KEY_COMPLETED = "completed";
    private static final String KEY_CRASHES = "crashes";
    private static final String KEY_FIRST_FRAME_SAMPLES = "first_frame_samples";
    private static final String KEY_FIRST_FRAME_TOTAL = "first_frame_total_ms";

    private LaunchStatsStore() {
    }

    /** Reads the stored statistics, or an empty record when nothing has been written. */
    public static LaunchStats load(Context context) {
        if (context == null) return new LaunchStats();
        try {
            SharedPreferences prefs = context.getApplicationContext()
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            return new LaunchStats(
                    prefs.getInt(KEY_LAUNCHES, 0),
                    prefs.getInt(KEY_COMPLETED, 0),
                    prefs.getInt(KEY_CRASHES, 0),
                    prefs.getInt(KEY_FIRST_FRAME_SAMPLES, 0),
                    prefs.getLong(KEY_FIRST_FRAME_TOTAL, 0L));
        } catch (Throwable t) {
            // An unreadable store reads as "no statistics"; a stats screen must never be the reason
            // a launch path throws.
            return new LaunchStats();
        }
    }

    /** Writes the statistics back. Never throws. */
    public static void save(Context context, LaunchStats stats) {
        if (context == null || stats == null) return;
        try {
            context.getApplicationContext()
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putInt(KEY_LAUNCHES, stats.launches())
                    .putInt(KEY_COMPLETED, stats.completedLaunches())
                    .putInt(KEY_CRASHES, stats.crashes())
                    .putInt(KEY_FIRST_FRAME_SAMPLES, stats.firstFrameSamples())
                    .putLong(KEY_FIRST_FRAME_TOTAL, stats.firstFrameTotalMs())
                    .apply();
        } catch (Throwable ignored) {
            AppLog.warnOnce("CaramelClient", "LaunchStatsStore:65", "Ignored a failure at LaunchStatsStore:65", ignored);
        }
    }

    /** Loads, mutates and saves in one step, so every call site cannot forget the write. */
    public static void update(Context context, Mutation mutation) {
        if (context == null || mutation == null) return;
        LaunchStats stats = load(context);
        mutation.apply(stats);
        save(context, stats);
    }

    /** Clears every stored counter, for a "reset statistics" action. */
    public static void clear(Context context) {
        if (context == null) return;
        try {
            context.getApplicationContext()
                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().clear().apply();
        } catch (Throwable ignored) {
            AppLog.warnOnce("CaramelClient", "LaunchStatsStore:85", "Ignored a failure at LaunchStatsStore:85", ignored);
        }
    }

    /** A mutation of the statistics; the shape that keeps load/save out of every call site. */
    public interface Mutation {
        void apply(LaunchStats stats);
    }
}

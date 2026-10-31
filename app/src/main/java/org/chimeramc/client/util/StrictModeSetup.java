package org.chimeramc.client.util;

import android.os.StrictMode;

/**
 * Turns on {@link StrictMode} in debug builds so main-thread disk and network work is caught at the
 * point it is written rather than in a profiling session (item 296).
 *
 * <p>StrictMode is a development tool, so it is a no-op in a release build: the checks add overhead
 * to every file and socket access, and a violation policy that logs is only useful to a developer
 * holding a logcat. {@code BuildConfig.DEBUG} is a compile-time constant, so the whole body folds
 * away in release and no check ever runs there.
 *
 * <p>Penalties are log-only on purpose. A {@code penaltyDeath()} would make a debug build crash on
 * the first accidental main-thread read, which is worse than useless when the point is to find the
 * call sites. The reports go to the system log, where {@code adb logcat} shows them with the stack
 * of the offending call.
 */
public final class StrictModeSetup {

    private StrictModeSetup() {
    }

    /**
     * Installs the debug policies. Safe to call from {@code Application.onCreate}; a no-op in
     * release.
     */
    public static void install(boolean debug) {
        if (!debug) return;

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                // A main-thread file or socket access is the bug this exists to find.
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                // A custom SlowCode policy catches the leak of a stream, cursor or SQLite object,
                // which is the failure a disk-read check alone cannot see.
                .detectCustomSlowCalls()
                .penaltyLog()
                .build());

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build());
    }

    /**
     * Reports a slow main-thread operation. A no-op unless the caller passes the result of
     * {@code BuildConfig.DEBUG}, so it compiles into nothing in release.
     */
    public static void noteSlowCall(boolean debug, String name) {
        if (!debug) return;
        StrictMode.noteSlowCall(name);
    }
}

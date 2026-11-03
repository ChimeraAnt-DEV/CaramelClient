package org.chimeramc.client.core.minecraft;

import java.io.File;

/**
 * The disk an instance occupies, split by what it holds.
 *
 * <p>A single total does not tell a user what they can safely clear, so the figure is broken into
 * game data (worlds, resource packs, settings), mods, extracted runtime libraries and the scratch
 * cache. The cache is the only part the launcher can regenerate, so it is the one the UI offers to
 * clear.
 *
 * <p>Pure: it takes already-resolved directories and only measures them, so the split is unit-tested
 * against real temp directories with no Context.
 */
public final class InstanceDiskUsage {

    /** Bytes of game data (worlds, packs, settings). */
    public final long gameDataBytes;
    /** Bytes of installed mods. */
    public final long modsBytes;
    /** Bytes of extracted runtime libraries; regenerated on the next launch. */
    public final long runtimeBytes;
    /** Bytes of scratch cache; safe to clear at any time. */
    public final long cacheBytes;

    public InstanceDiskUsage(long gameDataBytes, long modsBytes, long runtimeBytes, long cacheBytes) {
        this.gameDataBytes = Math.max(0L, gameDataBytes);
        this.modsBytes = Math.max(0L, modsBytes);
        this.runtimeBytes = Math.max(0L, runtimeBytes);
        this.cacheBytes = Math.max(0L, cacheBytes);
    }

    /** Measures the four directories; any may be null or missing and counts as zero. */
    public static InstanceDiskUsage measure(File gameDataDir, File modsDir, File runtimeDir, File cacheDir) {
        return new InstanceDiskUsage(
                org.chimeramc.client.launcher.util.StorageUsage.sizeOf(gameDataDir),
                org.chimeramc.client.launcher.util.StorageUsage.sizeOf(modsDir),
                org.chimeramc.client.launcher.util.StorageUsage.sizeOf(runtimeDir),
                org.chimeramc.client.launcher.util.StorageUsage.sizeOf(cacheDir));
    }

    /** The sum of every part. */
    public long totalBytes() {
        return gameDataBytes + modsBytes + runtimeBytes + cacheBytes;
    }

    /**
     * Whether there is anything the Clear Cache action would remove.
     *
     * <p>Drives the button's enabled state, so a user is never offered an action that does nothing.
     */
    public boolean hasClearableCache() {
        return cacheBytes > 0L;
    }
}

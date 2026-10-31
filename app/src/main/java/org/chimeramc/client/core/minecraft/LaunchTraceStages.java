package org.chimeramc.client.core.minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The pure half of the launch trace: a bounded, ordered record of the milestones a launch passes
 * through, each with the elapsed time since the launch started (item 307).
 *
 * <p>{@link LaunchTrace} writes into this as the launch runs and can ask it for the slowest stages.
 * Keeping the arithmetic and the trimming here — with no Android type in sight — is what lets a JVM
 * test pin them: "the slowest three" is exactly the kind of rule that quietly reports the wrong stage
 * when it is written inline at the call site.
 *
 * <p>The record is bounded. A launch has a fixed set of stages today, but the trace is also written
 * from loops in the extraction path, and an unbounded list there would grow with the file count. When
 * the cap is reached the oldest entry is dropped, so the list always describes the most recent part
 * of the launch — which is where a stall is.
 */
public final class LaunchTraceStages {

    /** Enough for every milestone plus the per-file extraction marks, without growing unbounded. */
    public static final int MAX_STAGES = 128;

    private final String[] names = new String[MAX_STAGES];
    private final long[] atMs = new long[MAX_STAGES];
    private int size;

    /** Records a stage at {@code elapsedMs}. A null or blank name is ignored. */
    public synchronized void record(String name, long elapsedMs) {
        if (name == null || name.trim().isEmpty()) return;
        if (size == MAX_STAGES) {
            System.arraycopy(names, 1, names, 0, MAX_STAGES - 1);
            System.arraycopy(atMs, 1, atMs, 0, MAX_STAGES - 1);
            size--;
        }
        names[size] = name;
        atMs[size] = elapsedMs;
        size++;
    }

    public synchronized int size() {
        return size;
    }

    public synchronized long elapsedFor(int index) {
        return atMs[index];
    }

    public synchronized String nameFor(int index) {
        return names[index];
    }

    /**
     * The time each stage added over the one before it, in order.
     *
     * <p>A stage's own duration is the gap to the previous stage, not its elapsed time: the elapsed
     * time of the last stage is the whole launch, and reporting that as "extraction took 4 s" would
     * name the wrong culprit.
     */
    public synchronized long[] deltasMs() {
        long[] deltas = new long[size];
        long previous = 0L;
        for (int i = 0; i < size; i++) {
            deltas[i] = Math.max(0L, atMs[i] - previous);
            previous = atMs[i];
        }
        return deltas;
    }

    /** One stage with the time it added. */
    public static final class Stage implements Comparable<Stage> {
        public final String name;
        public final long deltaMs;

        Stage(String name, long deltaMs) {
            this.name = name;
            this.deltaMs = deltaMs;
        }

        @Override
        public int compareTo(Stage other) {
            // Slowest first; ties keep insertion order so the report is deterministic.
            return Long.compare(other.deltaMs, this.deltaMs);
        }

        @Override
        public String toString() {
            return String.format(Locale.US, "%s: %d ms", name, deltaMs);
        }
    }

    /** The {@code count} stages that added the most time, slowest first. */
    public synchronized List<Stage> slowest(int count) {
        if (count <= 0 || size == 0) return Collections.emptyList();
        List<Stage> all = new ArrayList<>(size);
        long previous = 0L;
        for (int i = 0; i < size; i++) {
            all.add(new Stage(names[i], Math.max(0L, atMs[i] - previous)));
            previous = atMs[i];
        }
        Collections.sort(all);
        return new ArrayList<>(all.subList(0, Math.min(count, all.size())));
    }

    /** A multi-line summary of the slowest {@code count} stages, for the diagnostics report. */
    public String describeSlowest(int count) {
        List<Stage> slowest = slowest(count);
        if (slowest.isEmpty()) return "(no launch stages recorded)";
        StringBuilder sb = new StringBuilder();
        for (Stage stage : slowest) {
            if (sb.length() > 0) sb.append('\n');
            sb.append("  ").append(stage);
        }
        return sb.toString();
    }

    /** Drops every recorded stage. */
    public synchronized void clear() {
        for (int i = 0; i < size; i++) {
            names[i] = null;
            atMs[i] = 0L;
        }
        size = 0;
    }
}

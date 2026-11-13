package org.chimeramc.client.core.replay;

import org.chimeramc.client.util.AppLog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The recorded positions of the other players over a clip, keyed by entity id.
 *
 * <p>The recorder captured only the local player, so a replay had nothing to show beside the camera.
 * The shipped build cannot read other <em>game</em> entities — the game classes are stripped of
 * symbols, so there is no reachable entity list (see the native-feed section of {@code AGENTS.md}).
 * But a proximity voice peer advertises its own position and facing over the voice protocol, and the
 * client already reads that for the nametag icons. Those advertisements are the one honest entity
 * feed the build has, so they are what this records: each known peer's track over the clip, so the
 * replay can draw the other players where they actually were.
 *
 * <p>Honest scope: a peer is only present while the voice link hears it, so the entity set is
 * "players who were on voice", not "every entity in the world". That is stated rather than implied —
 * a replay with no voice peers has no entity track, and that is correct.
 *
 * <p>Pure and clock-free: the caller supplies the elapsed time, so the cadence, the per-entity
 * interpolation, the serialisation and the bounds are all unit-tested without a device.
 */
public final class ReplayEntityTrack {

    /** The same sampling cadence the local-player track uses (10 Hz). */
    public static final long MIN_SAMPLE_INTERVAL_MS = ReplayTrack.MIN_SAMPLE_INTERVAL_MS;

    /** Upper bound on tracked entities, so a busy server cannot grow the track without limit. */
    public static final int MAX_ENTITIES = 64;

    /** Upper bound on frames per entity, matching the local-player track's twenty minutes. */
    public static final int MAX_FRAMES_PER_ENTITY = 12_000;

    /** One sampled pose. */
    public static final class Frame {
        public final long tMs;
        public final float x;
        public final float y;
        public final float z;
        public final float yawDeg;
        public final float pitchDeg;

        public Frame(long tMs, float x, float y, float z, float yawDeg, float pitchDeg) {
            this.tMs = Math.max(0L, tMs);
            this.x = x;
            this.y = y;
            this.z = z;
            this.yawDeg = yawDeg;
            this.pitchDeg = pitchDeg;
        }
    }

    /** One entity's frames, in ascending time order. */
    private static final class Track {
        final List<Frame> frames = new ArrayList<>();
    }

    private final Map<String, Track> tracks = new LinkedHashMap<>();

    public static ReplayEntityTrack empty() {
        return new ReplayEntityTrack();
    }

    public boolean isEmpty() {
        return tracks.isEmpty();
    }

    /** The number of tracked entities. */
    public int entityCount() {
        return tracks.size();
    }

    /** The tracked entity ids, in first-seen order. */
    public List<String> entityIds() {
        return new ArrayList<>(tracks.keySet());
    }

    /**
     * Stores one pose for an entity when the cadence allows.
     *
     * @return true when the frame was stored
     */
    public boolean sample(long tMs, String entityId, float x, float y, float z, float yawDeg,
                          float pitchDeg) {
        if (entityId == null || entityId.isEmpty()) return false;
        if (Float.isNaN(x) || Float.isNaN(y) || Float.isNaN(z)) return false;
        Track track = tracks.get(entityId);
        if (track == null) {
            if (tracks.size() >= MAX_ENTITIES) return false;
            track = new Track();
            tracks.put(entityId, track);
        }
        long time = Math.max(0L, tMs);
        if (!track.frames.isEmpty()) {
            long last = track.frames.get(track.frames.size() - 1).tMs;
            if (time >= last && time - last < MIN_SAMPLE_INTERVAL_MS) return false;
        }
        track.frames.add(new Frame(time, x, y, z, yawDeg, pitchDeg));
        while (track.frames.size() > MAX_FRAMES_PER_ENTITY) {
            track.frames.remove(0);
        }
        return true;
    }

    /** The interpolated pose of an entity at {@code tMs}, or null when it is not tracked. */
    public Frame at(long tMs, String entityId) {
        Track track = tracks.get(entityId);
        if (track == null || track.frames.isEmpty()) return null;
        List<Frame> frames = track.frames;
        Frame first = frames.get(0);
        if (tMs <= first.tMs) return first;
        Frame last = frames.get(frames.size() - 1);
        if (tMs >= last.tMs) return last;

        int high = 1;
        while (high < frames.size() && frames.get(high).tMs <= tMs) high++;
        Frame a = frames.get(high - 1);
        Frame b = frames.get(high);
        long span = b.tMs - a.tMs;
        float f = span <= 0L ? 0f : (float) (tMs - a.tMs) / (float) span;
        return new Frame(tMs,
                lerp(a.x, b.x, f), lerp(a.y, b.y, f), lerp(a.z, b.z, f),
                ReplayTrack.lerpAngle(a.yawDeg, b.yawDeg, f), lerp(a.pitchDeg, b.pitchDeg, f));
    }

    private static float lerp(float a, float b, float f) {
        return a + (b - a) * f;
    }

    /**
     * Serialises to {@code id|t,x,y,z,yaw,pitch;t,x,y,z,yaw,pitch} entries joined with {@code &}.
     *
     * <p>One line so it fits the sidecar and the JSON container; the separators ({@code &}, {@code |},
     * {@code ;}, {@code ,}) can never appear in a sanitised entity id.
     */
    public String serialize() {
        StringBuilder out = new StringBuilder();
        boolean firstEntity = true;
        for (Map.Entry<String, Track> entry : tracks.entrySet()) {
            if (entry.getValue().frames.isEmpty()) continue;
            if (!firstEntity) out.append('&');
            firstEntity = false;
            out.append(sanitizeId(entry.getKey())).append('|');
            boolean firstFrame = true;
            for (Frame f : entry.getValue().frames) {
                if (!firstFrame) out.append(';');
                firstFrame = false;
                out.append(f.tMs).append(',').append(f.x).append(',').append(f.y).append(',')
                        .append(f.z).append(',').append(f.yawDeg).append(',').append(f.pitchDeg);
            }
        }
        return out.toString();
    }

    /**
     * Parses a serialised entity track. A malformed entity or frame is skipped rather than failing
     * the whole track, so one bad write cannot make a replay's entities unusable.
     */
    public static ReplayEntityTrack parse(String text) {
        ReplayEntityTrack out = new ReplayEntityTrack();
        if (text == null || text.isEmpty()) return out;
        for (String entityEntry : text.split("&")) {
            int bar = entityEntry.indexOf('|');
            if (bar <= 0) continue;
            String id = entityEntry.substring(0, bar).trim();
            if (id.isEmpty()) continue;
            for (String rawFrame : entityEntry.substring(bar + 1).split(";")) {
                String line = rawFrame.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length != 6) continue;
                try {
                    out.sample(Long.parseLong(parts[0].trim()),
                            id,
                            Float.parseFloat(parts[1].trim()),
                            Float.parseFloat(parts[2].trim()),
                            Float.parseFloat(parts[3].trim()),
                            Float.parseFloat(parts[4].trim()),
                            Float.parseFloat(parts[5].trim()));
                } catch (NumberFormatException ignored) {
                    AppLog.warnOnce("CaramelClient", "ReplayEntityTrack:parse",
                            "Ignored a malformed entity frame", ignored);
                }
            }
        }
        return out;
    }

    /** Strips the serialisation separators from an id, so the line stays parseable. */
    private static String sanitizeId(String id) {
        StringBuilder out = new StringBuilder(id.length());
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (c != '&' && c != '|' && c != ';' && c != ',' && c != '\n' && c != '\r') {
                out.append(c);
            }
        }
        return out.toString();
    }
}

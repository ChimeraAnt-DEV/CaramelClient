package org.chimeramc.client.core.replay;

import org.chimeramc.client.util.AppLog;

import java.util.ArrayList;
import java.util.List;

/**
 * The recorded gameplay track: the player's own state over the clip, sampled on a fixed cadence.
 *
 * <p>This is what makes a Replay a replay rather than a video file. The recorder captures the
 * screen, but the screen alone cannot answer "where was I standing, and which way was I looking, at
 * 1:23" — so the free camera had no pose to detach from and the timeline could only scrub a video.
 * The track is that missing half: a bounded, append-only series of {@link Frame}s (time, position,
 * facing, health) sampled from the same native local-player feed the HUD reads.
 *
 * <p><b>Honest scope.</b> The shipped builds expose the local player's own position, facing and
 * health through the local-player feed and nothing else: game classes export no symbols, so block
 * changes, mobs and other players cannot be read (see the native-feed section of {@code AGENTS.md}).
 * The track therefore records exactly what the build can see and says so — it is the local player's
 * trajectory, not a world snapshot. A value the feed did not supply is stored as
 * {@link Frame#UNKNOWN} and interpolated as absent rather than as a plausible {@code 0}.
 *
 * <p>Pure and Android-free, so sampling, interpolation and the serialisation round-trip are
 * unit-tested without a device.
 */
public final class ReplayTrack {

    /**
     * The minimum gap between two stored frames, in milliseconds (10 Hz).
     *
     * <p>Ten hertz is enough for a smooth camera: a free camera interpolates between frames, and the
     * player's own movement over 100 ms is small. Sampling faster would grow the sidecar without
     * making the path visibly smoother, and the local-player feed is a JNI read, so the cadence is
     * deliberately modest.
     */
    public static final long MIN_SAMPLE_INTERVAL_MS = 100L;

    /**
     * The hard cap on stored frames, so a long session cannot grow the track without bound.
     *
     * <p>At the 10 Hz cadence this is twenty minutes of continuous play. When it is reached the
     * oldest frames are dropped, which keeps the recent part of a very long session — the part a
     * player actually wants to review — rather than refusing to record.
     */
    public static final int MAX_FRAMES = 12_000;

    /** A value the feed did not supply, for a field that has no reading this frame. */
    public static final float UNKNOWN = Float.NaN;

    /** One sampled state. */
    public static final class Frame {
        public final long tMs;
        public final float x;
        public final float y;
        public final float z;
        public final float yawDeg;
        public final float pitchDeg;
        public final float health;

        public Frame(long tMs, float x, float y, float z, float yawDeg, float pitchDeg, float health) {
            this.tMs = Math.max(0L, tMs);
            this.x = x;
            this.y = y;
            this.z = z;
            this.yawDeg = yawDeg;
            this.pitchDeg = pitchDeg;
            this.health = health;
        }

        /** True when this frame carries a usable position. */
        public boolean hasPosition() {
            return isFinite(x) && isFinite(y) && isFinite(z);
        }

        /** True when this frame carries a usable facing. */
        public boolean hasRotation() {
            return isFinite(yawDeg) && isFinite(pitchDeg);
        }
    }

    private final List<Frame> frames = new ArrayList<>();

    public static ReplayTrack empty() {
        return new ReplayTrack();
    }

    /**
     * Adds a frame when it is far enough past the last one, dropping the oldest at the cap.
     *
     * <p>A sample arriving inside {@link #MIN_SAMPLE_INTERVAL_MS} of the previous one is ignored
     * rather than replacing it, so a burst of feed reads (the recorder ticks faster than the
     * cadence, or the feed is polled twice in a frame) cannot crowd out the evenly-spaced path. A
     * frame with no position and no facing carries nothing, so it is not stored at all.
     *
     * @return true when the frame was stored
     */
    public boolean sample(long tMs, float x, float y, float z, float yawDeg, float pitchDeg,
                          float health) {
        Frame frame = new Frame(tMs, x, y, z, yawDeg, pitchDeg, health);
        if (!frame.hasPosition() && !frame.hasRotation() && !isFinite(health)) {
            return false;
        }
        if (!frames.isEmpty()) {
            long last = frames.get(frames.size() - 1).tMs;
            // A clock that went backwards (uptime wrap) starts a fresh cadence rather than
            // rejecting every later frame.
            if (frame.tMs >= last && frame.tMs - last < MIN_SAMPLE_INTERVAL_MS) {
                return false;
            }
        }
        frames.add(frame);
        while (frames.size() > MAX_FRAMES) {
            frames.remove(0);
        }
        return true;
    }

    public boolean isEmpty() {
        return frames.isEmpty();
    }

    public int size() {
        return frames.size();
    }

    public Frame frameAt(int index) {
        if (index < 0 || index >= frames.size()) return null;
        return frames.get(index);
    }

    /** The elapsed time the track spans, or 0 for a track with fewer than two frames. */
    public long durationMs() {
        if (frames.size() < 2) return 0L;
        return frames.get(frames.size() - 1).tMs - frames.get(0).tMs;
    }

    /** The first frame, or null when the track is empty. */
    public Frame first() {
        return frames.isEmpty() ? null : frames.get(0);
    }

    /**
     * The interpolated state at {@code tMs}, clamped to the track's range.
     *
     * <p>Position interpolates linearly and yaw takes the shortest way round (so a turn through
     * 179° to -179° moves two degrees, not 358). A field either bracketing frame lacks is carried
     * from the nearer frame that has it, so one dropped reading does not blank the pose. Returns
     * null for an empty track — never a synthesised origin, which would read as a real position.
     */
    public Frame at(long tMs) {
        if (frames.isEmpty()) return null;
        Frame firstFrame = frames.get(0);
        if (tMs <= firstFrame.tMs) return firstFrame;
        Frame lastFrame = frames.get(frames.size() - 1);
        if (tMs >= lastFrame.tMs) return lastFrame;

        int high = upperBound(tMs);
        Frame a = frames.get(high - 1);
        Frame b = frames.get(high);
        long span = b.tMs - a.tMs;
        float f = span <= 0L ? 0f : (float) (tMs - a.tMs) / (float) span;
        return new Frame(tMs,
                lerp(a.x, b.x, f), lerp(a.y, b.y, f), lerp(a.z, b.z, f),
                lerpAngle(a.yawDeg, b.yawDeg, f), lerp(a.pitchDeg, b.pitchDeg, f),
                lerp(a.health, b.health, f));
    }

    /** The index of the first frame with {@code tMs} at or after the given time. */
    private int upperBound(long tMs) {
        int low = 0;
        int high = frames.size();
        while (low < high) {
            int mid = (low + high) >>> 1;
            if (frames.get(mid).tMs <= tMs) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return Math.min(low, frames.size() - 1);
    }

    private static float lerp(float a, float b, float f) {
        if (!isFinite(a)) return b;
        if (!isFinite(b)) return a;
        return a + (b - a) * f;
    }

    /** Interpolates an angle in degrees along the shorter arc. */
    static float lerpAngle(float a, float b, float f) {
        if (!isFinite(a)) return b;
        if (!isFinite(b)) return a;
        float delta = wrapDeg(b - a);
        return wrapDeg(a + delta * f);
    }

    private static float wrapDeg(float degrees) {
        float d = degrees % 360f;
        if (d > 180f) d -= 360f;
        if (d < -180f) d += 360f;
        return d;
    }

    private static boolean isFinite(float v) {
        return !Float.isNaN(v) && !Float.isInfinite(v);
    }

    /** Serialises the track to one frame per line, for readability and tests. */
    public String serialize() {
        StringBuilder builder = new StringBuilder(frames.size() * 32);
        for (Frame frame : frames) {
            builder.append(frameLine(frame)).append('\n');
        }
        return builder.toString();
    }

    /**
     * Serialises the track to a single line, for the clip's sidecar.
     *
     * <p>The sidecar is a line-oriented {@code key=value} file, so a multi-line value would break
     * every key after it. Frames are joined with {@code ;} instead — the frame separator can never
     * collide with the {@code ,} inside a frame or the {@code =} between a key and its value.
     */
    public String serializeLine() {
        StringBuilder builder = new StringBuilder(frames.size() * 32);
        for (int i = 0; i < frames.size(); i++) {
            if (i > 0) builder.append(';');
            builder.append(frameLine(frames.get(i)));
        }
        return builder.toString();
    }

    private static String frameLine(Frame frame) {
        return frame.tMs + "," + frame.x + "," + frame.y + "," + frame.z + ","
                + frame.yawDeg + "," + frame.pitchDeg + "," + frame.health;
    }

    /**
     * Parses a serialised track. A malformed or truncated frame is skipped rather than failing the
     * whole track, so one bad write cannot make a clip's replay unusable. Accepts both the
     * newline-separated ({@link #serialize}) and the single-line ({@link #serializeLine}) forms.
     */
    public static ReplayTrack parse(String text) {
        ReplayTrack track = new ReplayTrack();
        if (text == null || text.isEmpty()) return track;
        for (String rawFrame : text.split("[;\n]")) {
            String line = rawFrame.trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split(",");
            if (parts.length < 7) continue;
            try {
                track.frames.add(new Frame(
                        Long.parseLong(parts[0].trim()),
                        Float.parseFloat(parts[1].trim()),
                        Float.parseFloat(parts[2].trim()),
                        Float.parseFloat(parts[3].trim()),
                        Float.parseFloat(parts[4].trim()),
                        Float.parseFloat(parts[5].trim()),
                        Float.parseFloat(parts[6].trim())));
            } catch (NumberFormatException ignored) {
                AppLog.warnOnce("GlowberryClient", "ReplayTrack:262", "Ignored a failure at ReplayTrack:262", ignored);
            }
        }
        return track;
    }

    /** Seeds a spectator camera at the track's start, or null when there is no usable pose. */
    public ReplayCamera toCamera() {
        Frame start = firstUsablePose();
        if (start == null) return null;
        ReplayCamera camera = new ReplayCamera();
        camera.set(start.x, start.y, start.z,
                start.hasRotation() ? start.yawDeg : 0f,
                start.hasRotation() ? start.pitchDeg : 0f);
        return camera;
    }

    private Frame firstUsablePose() {
        for (Frame frame : frames) {
            if (frame.hasPosition()) return frame;
        }
        return null;
    }
}

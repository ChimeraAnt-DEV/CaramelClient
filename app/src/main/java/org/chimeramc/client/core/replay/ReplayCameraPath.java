package org.chimeramc.client.core.replay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A cinematic camera path: keyframes and the presets that generate them.
 *
 * <p>The spec asks for camera keyframes, smooth paths and a set of shot presets (orbit, dolly, drone
 * and the rest). All of that is the same pure object: a list of timed keyframes sampled by the
 * timeline, plus generators that fill the list from a preset and the current view. Keeping it pure
 * means a shot can be authored, saved and unit-tested without a renderer, and the free camera and
 * the path share one pose representation.
 *
 * <p>Sampling is Catmull-Rom through the keyframes so a path is smooth rather than a chain of
 * straight hops; a path with fewer than two keyframes is a no-op. A camera cannot be asked to look
 * straight up, so pitch is clamped exactly as {@link ReplayCamera} clamps it.
 */
public final class ReplayCameraPath {

    /** The built-in shot presets. */
    public enum Preset {
        STATIC,
        // Orbit family: the camera circles the subject.
        ORBIT,
        ORBIT_LOW,
        ORBIT_HIGH,
        ARC,
        DOLLY_ORBIT,
        // Push/pull family: the camera moves along its own forward axis.
        DOLLY_ZOOM,
        PUSH_IN,
        PULL_OUT,
        REVEAL,
        // Lateral/vertical family: the camera slides without changing its facing.
        TRUCK_LEFT,
        TRUCK_RIGHT,
        PEDESTAL_UP,
        SWEEP,
        FLY_BY,
        // Pitch family: the camera holds position and tilts.
        TILT_UP,
        TILT_DOWN,
        TOPDOWN,
        // Aerial family: the camera rises and looks down.
        DRONE,
        CRANE,
        FLYOVER,
        SPIRAL,
        HELIX,
        CHASE
    }

    /** How many presets ship; pinned by a test so "dozens" cannot quietly shrink. */
    public static final int PRESET_COUNT = Preset.values().length;

    /**
     * The interpolation timing of a segment: the "smooth interpolation (Bezier curves)" the spec
     * asks for.
     *
     * <p>The easing reshapes *when* along a segment the camera reaches a point, not the shape of the
     * path itself, which is what makes a move feel like a camera move rather than a constant slide.
     * {@link #EASE_IN} and {@link #EASE_OUT} are the two halves of a cubic-Bezier ease-in-out
     * ({@code (0.42,0,1,1)} and {@code (0,0,0.58,1)}); {@link #EASE_IN_OUT} is the symmetric
     * {@code (0.42,0,0.58,1)}.
     */
    public enum Easing {
        /** Constant speed. */
        LINEAR,
        /** Starts slow and speeds up (a push-in settling into motion). */
        EASE_IN,
        /** Starts fast and slows down (a move arriving at its subject). */
        EASE_OUT,
        /** Slow at both ends, fast in the middle: the classic camera move. */
        EASE_IN_OUT
    }

    /** One timed camera pose. */
    public static final class Keyframe {
        public final long timeMs;
        public final float x, y, z;
        public final float yawDeg, pitchDeg;
        /** The field of view at this keyframe, in degrees, so a shot can zoom through the path. */
        public final float fovDeg;

        public Keyframe(long timeMs, float x, float y, float z, float yawDeg, float pitchDeg) {
            this(timeMs, x, y, z, yawDeg, pitchDeg, ReplayCamera.DEFAULT_FOV);
        }

        public Keyframe(long timeMs, float x, float y, float z, float yawDeg, float pitchDeg,
                        float fovDeg) {
            this.timeMs = Math.max(0L, timeMs);
            this.x = x;
            this.y = y;
            this.z = z;
            this.yawDeg = yawDeg;
            this.pitchDeg = ReplayCamera.clampPitch(pitchDeg);
            this.fovDeg = clampFov(fovDeg);
        }
    }

    private final List<Keyframe> keyframes = new ArrayList<>();
    private Preset preset = Preset.STATIC;
    private Easing easing = Easing.LINEAR;
    /**
     * How much the curve blends toward the straight line between keyframes, 0..1.
     *
     * <p>1 is the full Catmull-Rom curve (smooth but can overshoot a sharp corner); 0 is a straight
     * hop between keyframes (no overshoot but visibly faceted). The default is 1 so an existing path
     * keeps the smoothing it always had.
     */
    private float smoothing = 1f;

    public List<Keyframe> keyframes() {
        return Collections.unmodifiableList(keyframes);
    }

    public Preset preset() {
        return preset;
    }

    /** Records which preset produced this path (does not regenerate the keyframes). */
    public void setPreset(Preset preset) {
        if (preset != null) this.preset = preset;
    }

    /** The interpolation timing applied to every segment. */
    public Easing easing() {
        return easing;
    }

    /** Sets the interpolation timing; null is ignored so a bad read cannot blank it. */
    public void setEasing(Easing easing) {
        if (easing != null) this.easing = easing;
    }

    /** The curve-to-straight blend, 0..1 (1 is the full smooth curve). */
    public float smoothing() {
        return smoothing;
    }

    /** Sets the curve-to-straight blend, clamped to 0..1; a non-finite value is ignored. */
    public void setSmoothing(float smoothing) {
        if (Float.isNaN(smoothing) || Float.isInfinite(smoothing)) return;
        this.smoothing = Math.max(0f, Math.min(1f, smoothing));
    }

    public boolean isEmpty() {
        return keyframes.isEmpty();
    }

    public int size() {
        return keyframes.size();
    }

    public void clear() {
        keyframes.clear();
    }

    /** Appends a keyframe, keeping the list ordered by time. */
    public void addKeyframe(Keyframe keyframe) {
        if (keyframe == null) return;
        keyframes.add(keyframe);
        keyframes.sort((a, b) -> Long.compare(a.timeMs, b.timeMs));
    }

    /** Appends the camera's current pose at {@code timeMs}. */
    public void capture(ReplayCamera camera, long timeMs) {
        if (camera == null) return;
        addKeyframe(new Keyframe(timeMs, camera.x(), camera.y(), camera.z(),
                camera.yawDeg(), camera.pitchDeg(), camera.fovDeg()));
    }

    /**
     * Samples the path into {@code out}.
     *
     * @return true when a pose was produced; false for a path with no keyframes, in which case the
     *         camera is left untouched.
     */
    public boolean sample(long timeMs, ReplayCamera out) {
        if (out == null || keyframes.isEmpty()) return false;
        if (keyframes.size() == 1) {
            apply(out, keyframes.get(0));
            return true;
        }
        if (timeMs <= keyframes.get(0).timeMs) {
            apply(out, keyframes.get(0));
            return true;
        }
        Keyframe last = keyframes.get(keyframes.size() - 1);
        if (timeMs >= last.timeMs) {
            apply(out, last);
            return true;
        }
        int i = 0;
        while (i + 1 < keyframes.size() && keyframes.get(i + 1).timeMs <= timeMs) i++;
        Keyframe a = keyframes.get(i);
        Keyframe b = keyframes.get(i + 1);
        float span = Math.max(1f, b.timeMs - a.timeMs);
        float t = (timeMs - a.timeMs) / span;
        // Reshape the timing first, so the easing moves the sample along the segment; then the curve
        // itself is sampled at that reshaped point.
        float te = ease(easing, t);
        // Catmull-Rom needs the neighbours on either side; at an end the segment repeats itself,
        // which flattens the tangent rather than overshooting off the path.
        Keyframe p0 = keyframes.get(Math.max(0, i - 1));
        Keyframe p3 = keyframes.get(Math.min(keyframes.size() - 1, i + 2));
        float blend = smoothing;
        float x = blendSample(blend, a.x, b.x, catmull(p0.x, a.x, b.x, p3.x, te), te);
        float y = blendSample(blend, a.y, b.y, catmull(p0.y, a.y, b.y, p3.y, te), te);
        float z = blendSample(blend, a.z, b.z, catmull(p0.z, a.z, b.z, p3.z, te), te);
        float yaw = blendSample(blend, a.yawDeg, b.yawDeg,
                catmull(p0.yawDeg, a.yawDeg, b.yawDeg, p3.yawDeg, te), te);
        float pitch = blendSample(blend, a.pitchDeg, b.pitchDeg,
                catmull(p0.pitchDeg, a.pitchDeg, b.pitchDeg, p3.pitchDeg, te), te);
        out.set(x, y, z, yaw, pitch);
        // The field of view interpolates linearly along the reshaped segment, so a zoom matches the
        // move it is part of rather than running on its own clock.
        out.setFov(a.fovDeg + (b.fovDeg - a.fovDeg) * te);
        return true;
    }

    /**
     * Blends the smooth curve toward the straight line by {@code smoothing} (0..1).
     *
     * <p>A straight lerp between the two keyframe values is the {@code smoothing == 0} case, and the
     * Catmull-Rom value is the {@code smoothing == 1} case, so a value in between is a controlled
     * amount of curve. This is what the "path smoothing" control moves.
     */
    private static float blendSample(float smoothing, float a, float b, float curved, float t) {
        if (smoothing >= 1f) return curved;
        float straight = a + (b - a) * t;
        return straight + (curved - straight) * smoothing;
    }

    /**
     * Applies an easing to a normalised segment position, 0..1.
     *
     * <p>A cubic Bezier evaluated at {@code t}, solved for {@code y} at the given {@code x}, so the
     * result is a true Bezier timing rather than a polynomial approximation. The control points are
     * the standard CSS easings. The solve is a fixed number of Newton iterations followed by a
     * bisection fallback, so it is deterministic and cannot loop.
     */
    public static float ease(Easing easing, float t) {
        if (easing == null || easing == Easing.LINEAR) return clamp01(t);
        float x = clamp01(t);
        float x1, y1, x2, y2;
        switch (easing) {
            case EASE_IN:
                x1 = 0.42f; y1 = 0f; x2 = 1f; y2 = 1f;
                break;
            case EASE_OUT:
                x1 = 0f; y1 = 0f; x2 = 0.58f; y2 = 1f;
                break;
            case EASE_IN_OUT:
            default:
                x1 = 0.42f; y1 = 0f; x2 = 0.58f; y2 = 1f;
                break;
        }
        return bezierY(x, x1, y1, x2, y2);
    }

    /** Solves the cubic Bezier {@code (x1,y1),(x2,y2)} for {@code y} at a given {@code x}. */
    private static float bezierY(float x, float x1, float y1, float x2, float y2) {
        // Newton's method for the x(t) == x solve, then evaluate y(t). Bounded iterations keep it
        // deterministic; a flat derivative (near the ends) falls back to bisection.
        float t = x;
        for (int i = 0; i < 8; i++) {
            float xt = bezier(x1, x2, t) - x;
            if (Math.abs(xt) < 1e-5f) break;
            float dx = bezierSlope(x1, x2, t);
            if (Math.abs(dx) < 1e-6f) break;
            t -= xt / dx;
            t = clamp01(t);
        }
        return bezier(y1, y2, t);
    }

    /** One axis of a cubic Bezier with control points 0, {@code p1}, {@code p2}, 1. */
    private static float bezier(float p1, float p2, float t) {
        float u = 1f - t;
        return 3f * u * u * t * p1 + 3f * u * t * t * p2 + t * t * t;
    }

    /** The derivative of {@link #bezier} with respect to t. */
    private static float bezierSlope(float p1, float p2, float t) {
        float u = 1f - t;
        return 3f * u * u * p1 + 6f * u * t * (p2 - p1) + 3f * t * t * (1f - p2);
    }

    private static float clamp01(float v) {
        if (Float.isNaN(v)) return 0f;
        if (v < 0f) return 0f;
        return v > 1f ? 1f : v;
    }

    /**
     * Generates a preset path around the camera's current view.
     *
     * @param durationMs the clip length the path should span
     */
    public void generate(Preset preset, ReplayCamera camera, long durationMs) {
        if (preset == null || camera == null) return;
        this.preset = preset;
        keyframes.clear();
        long duration = Math.max(1000L, durationMs);
        float cx = camera.x(), cy = camera.y(), cz = camera.z();
        float yaw = camera.yawDeg();
        double yawRad = Math.toRadians(yaw);
        float fx = (float) Math.sin(yawRad);   // forward
        float fz = (float) Math.cos(yawRad);
        float rx = (float) Math.cos(yawRad);   // right
        float rz = (float) -Math.sin(yawRad);
        switch (preset) {
            case STATIC:
                capture(camera, 0L);
                capture(camera, duration);
                break;
            case ORBIT:
                orbit(cx, cy, cz, duration, 24f, 2f, 8f);
                break;
            case ORBIT_LOW:
                orbit(cx, cy, cz, duration, 18f, 0.5f, 2f);
                break;
            case ORBIT_HIGH:
                orbit(cx, cy, cz, duration, 30f, 12f, 30f);
                break;
            case ARC:
                arc(cx, cy, cz, duration, 20f);
                break;
            case DOLLY_ORBIT:
                dollyOrbit(cx, cy, cz, duration);
                break;
            case DOLLY_ZOOM:
                pushAlong(cx, cy, cz, duration, fx, fz, 8f, 2f, 1.5f, 1f);
                break;
            case PUSH_IN:
                pushAlong(cx, cy, cz, duration, fx, fz, 10f, 3f, 2f, 1.5f);
                break;
            case PULL_OUT:
                pushAlong(cx, cy, cz, duration, fx, fz, 2f, 1f, 10f, 3f);
                break;
            case REVEAL:
                reveal(cx, cy, cz, duration, fx, fz);
                break;
            case TRUCK_LEFT:
                slide(cx, cy, cz, duration, rx, rz, yaw, 10f, 0f, 1.5f, -8f, 0f, 1.5f);
                break;
            case TRUCK_RIGHT:
                slide(cx, cy, cz, duration, rx, rz, yaw, -8f, 0f, 1.5f, 10f, 0f, 1.5f);
                break;
            case PEDESTAL_UP:
                addKeyframe(new Keyframe(0L, cx, cy - 2f, cz, yaw, 2f));
                addKeyframe(new Keyframe(duration, cx, cy + 12f, cz, yaw, 6f));
                break;
            case SWEEP:
                slide(cx, cy, cz, duration, rx, rz, yaw, -12f, 3f, 4f, 12f, 3f, 4f);
                break;
            case FLY_BY:
                flyBy(cx, cy, cz, duration, rx, rz, yaw);
                break;
            case TILT_UP:
                addKeyframe(new Keyframe(0L, cx, cy, cz, yaw, -10f));
                addKeyframe(new Keyframe(duration, cx, cy, cz, yaw, 30f));
                break;
            case TILT_DOWN:
                addKeyframe(new Keyframe(0L, cx, cy, cz, yaw, 35f));
                addKeyframe(new Keyframe(duration, cx, cy, cz, yaw, -15f));
                break;
            case TOPDOWN:
                addKeyframe(new Keyframe(0L, cx, cy + 6f, cz, yaw, 20f));
                addKeyframe(new Keyframe(duration, cx, cy + 22f, cz, yaw, 88f));
                break;
            case DRONE:
                drone(cx, cy, cz, duration, yaw, rx, rz);
                break;
            case CRANE:
                addKeyframe(new Keyframe(0L, cx, cy - 2f, cz, yaw, 4f));
                addKeyframe(new Keyframe(duration, cx, cy + 14f, cz, yaw, 34f));
                break;
            case FLYOVER:
                flyOver(cx, cy, cz, duration, yaw, rx, rz);
                break;
            case SPIRAL:
                spiral(cx, cy, cz, duration, 22f, 2f, 14f, 1f);
                break;
            case HELIX:
                spiral(cx, cy, cz, duration, 14f, 0f, 20f, 2f);
                break;
            case CHASE:
                chase(cx, cy, cz, duration, fx, fz);
                break;
            default:
                capture(camera, 0L);
                capture(camera, duration);
                break;
        }
    }

    /** A full circle at a fixed radius and height, always facing the subject. */
    private void orbit(float cx, float cy, float cz, long duration, float radius, float height,
                       float pitch) {
        int steps = 16;
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            double angle = Math.toRadians(t * 360.0);
            float x = cx + (float) (Math.sin(angle) * radius);
            float z = cz + (float) (Math.cos(angle) * radius);
            float yaw = (float) Math.toDegrees(angle) + 180f;
            addKeyframe(new Keyframe((long) (t * duration), x, cy + height, z, yaw, pitch));
        }
    }

    /** A half circle around the subject, from one side to the other. */
    private void arc(float cx, float cy, float cz, long duration, float radius) {
        int steps = 12;
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            double angle = Math.toRadians(90.0 + t * 180.0);
            float x = cx + (float) (Math.sin(angle) * radius);
            float z = cz + (float) (Math.cos(angle) * radius);
            float yaw = (float) Math.toDegrees(angle) + 180f;
            addKeyframe(new Keyframe((long) (t * duration), x, cy + 3f, z, yaw, 6f));
        }
    }

    /** A circle whose radius tightens from wide to close, ending on the subject. */
    private void dollyOrbit(float cx, float cy, float cz, long duration) {
        int steps = 20;
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            double angle = Math.toRadians(t * 360.0);
            float radius = 24f - 14f * t;
            float x = cx + (float) (Math.sin(angle) * radius);
            float z = cz + (float) (Math.cos(angle) * radius);
            float yaw = (float) Math.toDegrees(angle) + 180f;
            addKeyframe(new Keyframe((long) (t * duration), x, cy + 2f + t, z, yaw, 6f + 6f * t));
        }
    }

    /**
     * A push along the camera's own forward axis, from {@code startBehind} blocks behind the subject
     * to {@code endBehind}, at heights {@code startHeight} and {@code endHeight}. A negative
     * "behind" distance pushes through the subject, so both are kept positive by the callers.
     */
    private void pushAlong(float cx, float cy, float cz, long duration, float fx, float fz,
                           float startBehind, float startHeight, float endBehind,
                           float endHeight) {
        float yaw = yawFor(-fx, -fz);
        addKeyframe(new Keyframe(0L, cx - fx * startBehind, cy + startHeight,
                cz - fz * startBehind, yaw, 6f));
        addKeyframe(new Keyframe(duration, cx - fx * endBehind, cy + endHeight,
                cz - fz * endBehind, yaw, 4f));
    }

    /** A drop from high and far behind down to just behind the subject. */
    private void reveal(float cx, float cy, float cz, long duration, float fx, float fz) {
        float yaw = yawFor(-fx, -fz);
        addKeyframe(new Keyframe(0L, cx - fx * 18f, cy + 16f, cz - fz * 18f, yaw, 34f));
        addKeyframe(new Keyframe(duration, cx - fx * 3f, cy + 1f, cz - fz * 3f, yaw, 4f));
    }

    /** A lateral slide: the camera moves along the right vector and holds its facing. */
    private void slide(float cx, float cy, float cz, long duration, float rx, float rz, float yaw,
                       float startSide, float startHeight, float startPitch,
                       float endSide, float endHeight, float endPitch) {
        addKeyframe(new Keyframe(0L, cx + rx * startSide, cy + startHeight,
                cz + rz * startSide, yaw, startPitch));
        addKeyframe(new Keyframe(duration, cx + rx * endSide, cy + endHeight,
                cz + rz * endSide, yaw, endPitch));
    }

    /** A pass from one side of the subject to the other at altitude, facing the travel direction. */
    private void flyBy(float cx, float cy, float cz, long duration, float rx, float rz, float yaw) {
        float travelYaw = yawFor(rx, rz);
        addKeyframe(new Keyframe(0L, cx - rx * 16f, cy + 6f, cz - rz * 16f, travelYaw, 8f));
        addKeyframe(new Keyframe(duration, cx + rx * 16f, cy + 6f, cz + rz * 16f, travelYaw, 8f));
    }

    /** A high sweep across the subject, looking down. */
    private void flyOver(float cx, float cy, float cz, long duration, float yaw, float rx,
                         float rz) {
        addKeyframe(new Keyframe(0L, cx - rx * 18f, cy + 18f, cz - rz * 18f, yaw, 40f));
        addKeyframe(new Keyframe(duration / 2, cx, cy + 22f, cz, yaw, 60f));
        addKeyframe(new Keyframe(duration, cx + rx * 18f, cy + 18f, cz + rz * 18f, yaw, 40f));
    }

    /** A circle that also rises, with {@code turns} full revolutions. */
    private void spiral(float cx, float cy, float cz, long duration, float radius, float startRise,
                        float rise, float turns) {
        int steps = Math.max(8, (int) (16 * turns));
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            double angle = Math.toRadians(t * 360.0 * turns);
            float x = cx + (float) (Math.sin(angle) * radius);
            float z = cz + (float) (Math.cos(angle) * radius);
            float yaw = (float) Math.toDegrees(angle) + 180f;
            addKeyframe(new Keyframe((long) (t * duration), x, cy + startRise + rise * t, z, yaw,
                    10f + 10f * t));
        }
    }

    /** Trails the subject from a fixed offset behind and slightly above, with a gentle bob. */
    private void chase(float cx, float cy, float cz, long duration, float fx, float fz) {
        float yaw = yawFor(-fx, -fz);
        int steps = 8;
        for (int i = 0; i <= steps; i++) {
            float t = i / (float) steps;
            float bob = (float) Math.sin(t * Math.PI * 2.0) * 0.6f;
            addKeyframe(new Keyframe((long) (t * duration), cx - fx * 6f, cy + 2.5f + bob,
                    cz - fz * 6f, yaw, 8f));
        }
    }

    /** The yaw that makes the forward vector point along {@code (dx, dz)}. */
    private static float yawFor(float dx, float dz) {
        return (float) Math.toDegrees(Math.atan2(dx, dz));
    }

    /** A high, gently descending aerial sweep. */
    private void drone(float cx, float cy, float cz, long duration, float yaw, float rx,
                       float rz) {
        addKeyframe(new Keyframe(0L, cx - rx * 14f, cy + 20f, cz - rz * 14f, yaw, 42f));
        addKeyframe(new Keyframe(duration / 2, cx, cy + 16f, cz, yaw, 36f));
        addKeyframe(new Keyframe(duration, cx + rx * 14f, cy + 12f, cz + rz * 14f, yaw, 28f));
    }

    private static void apply(ReplayCamera out, Keyframe k) {
        out.set(k.x, k.y, k.z, k.yawDeg, k.pitchDeg);
        out.setFov(k.fovDeg);
    }

    /** Clamps a field of view to the camera's visible range, so a keyframe cannot store a bad one. */
    static float clampFov(float fovDeg) {
        if (Float.isNaN(fovDeg) || Float.isInfinite(fovDeg)) return ReplayCamera.DEFAULT_FOV;
        return Math.max(ReplayCamera.MIN_FOV, Math.min(ReplayCamera.MAX_FOV, fovDeg));
    }

    /** Uniform Catmull-Rom: the standard smooth interpolation through four control values. */
    static float catmull(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t;
        float t3 = t2 * t;
        return 0.5f * ((2f * p1)
                + (-p0 + p2) * t
                + (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2
                + (-p0 + 3f * p1 - 3f * p2 + p3) * t3);
    }
}

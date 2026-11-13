package org.chimeramc.client.core.replay;

import org.chimeramc.client.util.AppLog;

/**
 * Encodes a {@link ReplayCameraPath} to a single sidecar value and back.
 *
 * <p>A camera path is a small list of timed poses, so it is stored as one compact, human-readable
 * line rather than a nested JSON document the sidecar has no parser for. The format is
 * {@code preset;meta;time,x,y,z,yaw,pitch,fov;...} where a {@code meta} segment is
 * {@code e=<Easing>} or {@code s=<smoothing>}. A metadata segment carries an {@code =} and a
 * keyframe carries commas, so the two can never be confused, and a path written before FOV or
 * easing existed (a bare {@code preset;time,x,y,z,yaw,pitch;...} with six fields) still parses —
 * the missing FOV falls back to the camera default and the easing to {@code LINEAR}.
 *
 * <p>The rules are strict enough that a corrupt line degrades to "no path" rather than a
 * half-parsed shot: a segment with the wrong field count, a non-numeric field or an unknown preset
 * is skipped, and an empty result is an empty path.
 *
 * <p>Pure and Android-free, so the round-trip is unit-tested.
 */
public final class ReplayCameraPathCodec {

    private ReplayCameraPathCodec() {
    }

    /** Serialises a path; a null or empty path yields an empty string. */
    public static String serialize(ReplayCameraPath path) {
        if (path == null || path.isEmpty()) return "";
        StringBuilder builder = new StringBuilder(path.preset().name());
        // The path-level controls, written before the keyframes. Both are always written so the
        // value round-trips even when it equals the default.
        builder.append(";e=").append(path.easing().name());
        builder.append(";s=").append(fmt(path.smoothing()));
        for (ReplayCameraPath.Keyframe k : path.keyframes()) {
            builder.append(';').append(k.timeMs).append(',').append(fmt(k.x)).append(',')
                    .append(fmt(k.y)).append(',').append(fmt(k.z)).append(',')
                    .append(fmt(k.yawDeg)).append(',').append(fmt(k.pitchDeg)).append(',')
                    .append(fmt(k.fovDeg));
        }
        return builder.toString();
    }

    /** Parses a stored path; malformed input yields an empty path, never a partial throw. */
    public static ReplayCameraPath parse(String encoded) {
        ReplayCameraPath path = new ReplayCameraPath();
        if (encoded == null || encoded.isEmpty()) return path;
        String[] segments = encoded.split(";");
        if (segments.length == 0) return path;
        ReplayCameraPath.Preset preset = parsePreset(segments[0]);
        for (int i = 1; i < segments.length; i++) {
            String segment = segments[i].trim();
            if (segment.isEmpty()) continue;
            if (segment.indexOf('=') >= 0) {
                applyMeta(path, segment);
                continue;
            }
            String[] fields = segment.split(",");
            // Six fields is the pre-FOV form; seven carries the field of view.
            if (fields.length != 6 && fields.length != 7) continue;
            try {
                float fov = fields.length == 7
                        ? Float.parseFloat(fields[6].trim())
                        : ReplayCamera.DEFAULT_FOV;
                path.addKeyframe(new ReplayCameraPath.Keyframe(
                        Long.parseLong(fields[0].trim()),
                        Float.parseFloat(fields[1].trim()),
                        Float.parseFloat(fields[2].trim()),
                        Float.parseFloat(fields[3].trim()),
                        Float.parseFloat(fields[4].trim()),
                        Float.parseFloat(fields[5].trim()),
                        fov));
            } catch (NumberFormatException ignored) {
                AppLog.warnOnce("CaramelClient", "ReplayCameraPathCodec:53", "Ignored a failure at ReplayCameraPathCodec:53", ignored);
            }
        }
        path.setPreset(preset);
        return path;
    }

    /** Applies an {@code e=} or {@code s=} metadata segment, ignoring an unknown key or bad value. */
    private static void applyMeta(ReplayCameraPath path, String segment) {
        int eq = segment.indexOf('=');
        String key = segment.substring(0, eq).trim();
        String value = segment.substring(eq + 1).trim();
        if ("e".equals(key)) {
            try {
                path.setEasing(ReplayCameraPath.Easing.valueOf(value));
            } catch (IllegalArgumentException e) {
                // An unknown easing name leaves the default rather than blanking the path.
                AppLog.warnOnce("CaramelClient", "ReplayCameraPathCodec:easing",
                        "Ignored an unknown easing name", e);
            }
        } else if ("s".equals(key)) {
            try {
                path.setSmoothing(Float.parseFloat(value));
            } catch (NumberFormatException e) {
                AppLog.warnOnce("CaramelClient", "ReplayCameraPathCodec:smoothing",
                        "Ignored a non-numeric smoothing value", e);
            }
        }
    }

    private static ReplayCameraPath.Preset parsePreset(String name) {
        if (name == null) return ReplayCameraPath.Preset.STATIC;
        try {
            return ReplayCameraPath.Preset.valueOf(name.trim());
        } catch (IllegalArgumentException e) {
            return ReplayCameraPath.Preset.STATIC;
        }
    }

    private static String fmt(float value) {
        if (value == (long) value) return String.valueOf((long) value);
        return String.valueOf(value);
    }
}

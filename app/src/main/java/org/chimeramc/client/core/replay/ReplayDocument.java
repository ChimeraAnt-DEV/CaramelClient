package org.chimeramc.client.core.replay;

import org.chimeramc.client.util.AppLog;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * The {@code .glowreplay} container: one file that carries a whole recording.
 *
 * <p>A replay used to be scattered across a sidecar's flat {@code key=value} keys and the native
 * capture file, so it was not a thing you could move, share or version — it was an install's
 * internal state. This is the container the spec names: a self-describing document that holds the
 * metadata a replay needs to make sense on its own (player skin, world seed, time of day, server
 * address), plus the gameplay track, the markers and the authored camera path.
 *
 * <p><b>Why JSON, not binary.</b> The recording is small (a 10 Hz track of a few minutes is tens of
 * kilobytes) and the rest of the replay system already serialises its pieces to text
 * ({@link ReplayTrack}, {@link ReplayMarkers}, {@link ReplayCameraPathCodec}). A text container is
 * human-inspectable, diffable and needs no bespoke reader, and it reuses those three codecs rather
 * than inventing a fourth. A future binary payload (the world geometry the recorder cannot yet
 * capture) can be added as a base64 field without changing the envelope.
 *
 * <p><b>Self-describing.</b> The {@code format} tag and the {@code version} are on the wire, so a
 * reader handed an arbitrary file can tell whether it is a Caramel replay and which revision it
 * was written by, rather than inferring it from the extension.
 *
 * <p><b>Fail-closed.</b> Any malformed, truncated or foreign file parses to an empty recording
 * rather than throwing: an unreadable replay is "no replay", never a crash. A missing field reads as
 * empty (or {@code -1} for the numeric time of day), never a plausible {@code 0}, matching the
 * repo-wide rule.
 */
public final class ReplayDocument {

    /** The format tag written into every document, so a reader can identify it. */
    public static final String FORMAT = "glowreplay";
    /** The container revision. Bump when the envelope changes in a way a reader must know about. */
    public static final int VERSION = 1;
    /** The file extension the container is written with. */
    public static final String EXTENSION = ".glowreplay";

    /** The value for a numeric field that was never recorded, distinct from a real 0. */
    public static final long UNKNOWN_TIME = -1L;

    private final JsonObject root;

    private ReplayDocument(JsonObject root) {
        this.root = root == null ? new JsonObject() : root;
    }

    /** The format tag this document carries (not necessarily {@link #FORMAT}). */
    public String format() {
        return optString("format", "");
    }

    /** The container revision this document carries. */
    public int version() {
        return (int) optLong("version", 0L);
    }

    public long recordedAtMs() {
        return optLong("recordedAtMs", 0L);
    }

    public String world() {
        return optString("world", "");
    }

    public String gameVersion() {
        return optString("gameVersion", "");
    }

    public String gameMode() {
        return optString("gameMode", "");
    }

    /** The player's skin texture id, or empty when the recording did not capture one. */
    public String playerSkin() {
        return optString("playerSkin", "");
    }

    /** The world seed as text (a seed can exceed a long, so it is not parsed as a number). */
    public String worldSeed() {
        return optString("worldSeed", "");
    }

    /** The recorded time of day in ticks, or {@link #UNKNOWN_TIME} when it was not captured. */
    public long timeOfDay() {
        return optLong("timeOfDay", UNKNOWN_TIME);
    }

    /** The server address the session was on, or empty for a single-player world. */
    public String serverAddress() {
        return optString("serverAddress", "");
    }

    /** The recorded gameplay track, or an empty track. */
    public ReplayTrack track() {
        return ReplayTrack.parse(optString("track", ""));
    }

    /** The recorded markers, or an empty set. */
    public ReplayMarkers markers() {
        return ReplayMarkers.parse(optString("markers", ""));
    }

    /** The authored cinematic camera path, or an empty path. */
    public ReplayCameraPath cameraPath() {
        return ReplayCameraPathCodec.parse(optString("cameraPath", ""));
    }

    /**
     * The other players' recorded tracks, or an empty track.
     *
     * <p>The build cannot read other game entities, but a voice peer advertises its position, so
     * these are the entities a replay can honestly show.
     */
    public ReplayEntityTrack entities() {
        return ReplayEntityTrack.parse(optString("entities", ""));
    }

    /** A fresh document with the required identity fields. */
    public static ReplayDocument of(long recordedAtMs, String world, String gameVersion,
                                    String gameMode) {
        JsonObject root = new JsonObject();
        root.addProperty("format", FORMAT);
        root.addProperty("version", VERSION);
        root.addProperty("recordedAtMs", Math.max(0L, recordedAtMs));
        root.addProperty("world", nullToEmpty(world));
        root.addProperty("gameVersion", nullToEmpty(gameVersion));
        root.addProperty("gameMode", nullToEmpty(gameMode));
        return new ReplayDocument(root);
    }

    public ReplayDocument setPlayerSkin(String skinId) {
        root.addProperty("playerSkin", nullToEmpty(skinId));
        return this;
    }

    public ReplayDocument setWorldSeed(String seed) {
        root.addProperty("worldSeed", nullToEmpty(seed));
        return this;
    }

    public ReplayDocument setTimeOfDay(long ticks) {
        root.addProperty("timeOfDay", Math.max(UNKNOWN_TIME, ticks));
        return this;
    }

    public ReplayDocument setServerAddress(String address) {
        root.addProperty("serverAddress", nullToEmpty(address));
        return this;
    }

    public ReplayDocument setTrack(ReplayTrack track) {
        root.addProperty("track", track == null ? "" : track.serializeLine());
        return this;
    }

    public ReplayDocument setMarkers(ReplayMarkers markers) {
        root.addProperty("markers", markers == null ? "" : markers.serialize());
        return this;
    }

    public ReplayDocument setCameraPath(ReplayCameraPath path) {
        root.addProperty("cameraPath", ReplayCameraPathCodec.serialize(path));
        return this;
    }

    /** Stores the other players' tracks; an empty track clears the key. */
    public ReplayDocument setEntities(ReplayEntityTrack entities) {
        root.addProperty("entities", entities == null ? "" : entities.serialize());
        return this;
    }

    /** The document as JSON text. */
    public String serialize() {
        return root.toString();
    }

    /**
     * Parses a document. A null, empty, malformed or foreign body yields an empty document rather
     * than throwing, so a bad file reads as "no recording".
     */
    public static ReplayDocument parse(String text) {
        if (text == null || text.trim().isEmpty()) return new ReplayDocument(new JsonObject());
        try {
            JsonElement parsed = JsonParser.parseString(text);
            if (parsed == null || !parsed.isJsonObject()) {
                return new ReplayDocument(new JsonObject());
            }
            return new ReplayDocument(parsed.getAsJsonObject());
        } catch (Exception e) {
            // Gson throws on malformed JSON; a corrupt replay is "no replay", never a crash.
            AppLog.warnOnce("CaramelClient", "ReplayDocument:parse",
                    "Ignored a malformed .glowreplay body", e);
            return new ReplayDocument(new JsonObject());
        }
    }

    /** True when the body is a Caramel replay container (the format tag matches). */
    public static boolean looksLikeGlowReplay(String text) {
        if (text == null || text.trim().isEmpty()) return false;
        try {
            JsonElement parsed = JsonParser.parseString(text);
            if (parsed == null || !parsed.isJsonObject()) return false;
            JsonElement format = parsed.getAsJsonObject().get("format");
            return format != null && format.isJsonPrimitive()
                    && FORMAT.equals(format.getAsString());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Writes the document into {@code dir} as {@code <name>.glowreplay}, returning the file, or null
     * when it cannot be written.
     */
    public static File write(File dir, String name, ReplayDocument document) {
        if (dir == null || name == null || name.isEmpty() || document == null) return null;
        if (!dir.exists() && !dir.mkdirs()) return null;
        File target = new File(dir, name + EXTENSION);
        try {
            Files.write(target.toPath(), document.serialize().getBytes(StandardCharsets.UTF_8));
            return target;
        } catch (IOException e) {
            AppLog.warnOnce("CaramelClient", "ReplayDocument:write",
                    "Could not write the .glowreplay file", e);
            return null;
        }
    }

    /** Reads a document from a file, or an empty document when it cannot be read. */
    public static ReplayDocument read(File file) {
        if (file == null || !file.isFile()) return new ReplayDocument(new JsonObject());
        try {
            return parse(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
        } catch (IOException e) {
            return new ReplayDocument(new JsonObject());
        }
    }

    /** The file extension, including the dot. */
    public static String extension() {
        return EXTENSION;
    }

    /**
     * A clip descriptor for a {@code .glowreplay} file, from this document.
     *
     * <p>Lets the library list a portable replay exactly as it lists a recorded MP4 clip, so a
     * shared replay that lands in the replays folder shows up with its world, version and duration.
     */
    public ReplayClip toClip(File file, long durationMs) {
        ReplayTrack t = track();
        long recorded = recordedAtMs();
        if (recorded <= 0L && file != null) recorded = file.lastModified();
        ReplayClip clip = new ReplayClip(file, durationMs, file == null ? 0L : file.length(),
                recorded, world(), gameVersion(), gameMode());
        clip.setTrack(t);
        clip.setMarkers(markers());
        ReplayCameraPath path = cameraPath();
        clip.setCameraPath(path == null || path.isEmpty() ? null : path);
        return clip;
    }

    /**
     * A document describing an existing clip, carrying the pieces the clip already holds.
     *
     * <p>This is the export path: a recorded clip's sidecar metadata and track are lifted into the
     * portable container so the clip can be moved off the install as one file.
     */
    public static ReplayDocument fromClip(ReplayClip clip) {
        if (clip == null) {
            return new ReplayDocument(new JsonObject());
        }
        ReplayDocument doc = of(clip.recordedAtMs(), clip.world(), clip.gameVersion(),
                clip.gameMode());
        doc.setTrack(clip.track());
        doc.setMarkers(clip.markers());
        doc.setCameraPath(clip.cameraPath());
        return doc;
    }

    private String optString(String key, String fallback) {
        if (root == null || !root.has(key)) return fallback;
        JsonElement value = root.get(key);
        if (value == null || !value.isJsonPrimitive()) return fallback;
        try {
            return value.getAsString();
        } catch (Exception e) {
            return fallback;
        }
    }

    private long optLong(String key, long fallback) {
        if (root == null || !root.has(key)) return fallback;
        JsonElement value = root.get(key);
        if (value == null || !value.isJsonPrimitive()) return fallback;
        try {
            return value.getAsLong();
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

package org.chimeramc.client.core.replay;

import java.io.File;

/**
 * Collects a live session into a portable {@link ReplayDocument}.
 *
 * <p>The playback half of the replay system (the simulator, the camera rig, the timeline) existed but
 * nothing wrote a portable recording: the track and markers only ever reached a clip's sidecar, so a
 * replay could not be moved or shared as one file. This is the other half — it owns the local-player
 * track, the markers and the metadata, and produces the {@code .glowreplay} container when the
 * session ends.
 *
 * <p><b>Honest scope.</b> The recorder captures what the shipped build can actually read through the
 * local-player feed: the player's position, facing and health. The build exposes no symbols for block
 * changes, other entities or the level time, so those are left empty rather than fabricated. A
 * recording is therefore the local player's trajectory plus the camera path and the event markers,
 * and {@link ReplayDocument} says so on its own terms.
 *
 * <p>Pure and clock-free: the caller supplies the elapsed time, so the cadence, the marker merging and
 * the finish behaviour are all unit-tested without a device.
 */
public final class ReplayRecorder {

    private final ReplayTrack track = ReplayTrack.empty();
    /**
     * The live sampler when this recorder pulls from a feed, or null when samples are pushed in.
     *
     * <p>Both modes write the same track: the manager constructs the recorder around a
     * {@link ReplayTrackRecorder.Source} and calls {@link #tick}, while a test calls {@link #sample}
     * with explicit values. Keeping the two on one object means the marker and finish rules are
     * shared rather than duplicated.
     */
    private final ReplayTrackRecorder sampler;
    private ReplayMarkers markers = ReplayMarkers.empty();
    private ReplayCameraPath cameraPath;

    private boolean recording;
    private long startedAtMs;
    private String gameVersion = "";
    private String gameMode = "";
    private String playerSkin = "";

    /** A recorder that takes pushed samples, for a caller that reads the feed itself. */
    public ReplayRecorder() {
        this.sampler = null;
    }

    /** A recorder that pulls from a feed on {@link #tick}. */
    public ReplayRecorder(ReplayTrackRecorder.Source source) {
        this.sampler = source == null ? null : new ReplayTrackRecorder(source);
    }

    /**
     * Begins a recording.
     *
     * <p>Adds the {@link ReplayMarkers.Kind#JOIN} marker at time zero: the one moment the client
     * knows for certain, the start of the recording itself.
     *
     * @param startedAtMs the wall-clock start, written as the document's {@code recordedAtMs}
     */
    public void start(long startedAtMs, String gameVersion, String gameMode, String playerSkin) {
        this.startedAtMs = Math.max(0L, startedAtMs);
        this.gameVersion = nullToEmpty(gameVersion);
        this.gameMode = nullToEmpty(gameMode);
        this.playerSkin = nullToEmpty(playerSkin);
        this.recording = true;
        this.markers = ReplayMarkers.empty();
        this.markers.add(0L, ReplayMarkers.Kind.JOIN, "");
    }

    public boolean isRecording() {
        return recording;
    }

    /**
     * Pulls one sample from the feed and stores it when the cadence allows.
     *
     * @return true when the sample was stored
     */
    public boolean tick(long elapsedMs) {
        if (!recording || sampler == null) return false;
        return sampler.tick(elapsedMs);
    }

    /**
     * Stores one pushed sample when the cadence allows.
     *
     * @return true when the sample was stored (false when not recording, or the track dropped it)
     */
    public boolean sample(long elapsedMs, float x, float y, float z, float yawDeg, float pitchDeg,
                          float health) {
        if (!recording || sampler != null) return false;
        return track.sample(elapsedMs, x, y, z, yawDeg, pitchDeg, health);
    }

    /** Adds a marker, merging into an existing one of the same kind inside the window. */
    public void addMarker(long elapsedMs, ReplayMarkers.Kind kind, String detail) {
        if (!recording) return;
        markers.add(elapsedMs, kind, detail);
    }

    /** Stores the authored cinematic path; a null or empty path clears it. */
    public void setCameraPath(ReplayCameraPath path) {
        this.cameraPath = path != null && !path.isEmpty() ? path : null;
    }

    /** The track captured so far, from whichever mode is in use. */
    public ReplayTrack track() {
        return sampler != null ? sampler.track() : track;
    }

    /** The markers captured so far. */
    public ReplayMarkers markers() {
        return markers;
    }

    /**
     * Ends the recording and produces the document.
     *
     * <p>Idempotent: calling it twice returns an equivalent document and a later {@link #sample} is
     * refused, so a late tick cannot corrupt a recording that has already been saved.
     */
    public ReplayDocument finish() {
        recording = false;
        ReplayDocument doc = ReplayDocument.of(startedAtMs, "", gameVersion, gameMode)
                .setPlayerSkin(playerSkin)
                .setTrack(track())
                .setMarkers(markers);
        if (cameraPath != null && !cameraPath.isEmpty()) {
            doc.setCameraPath(cameraPath);
        }
        return doc;
    }

    /**
     * Ends the recording and writes it beside the clip as {@code <name>.glowreplay}.
     *
     * @return the written file, or null when it could not be written
     */
    public File writeTo(File dir, String name) {
        return ReplayDocument.write(dir, name, finish());
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

package org.chimeramc.client.core.replay;

import org.chimeramc.client.util.AppLog;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import org.chimeramc.client.preloader.PreloaderInput;
import org.chimeramc.client.util.AppExecutors;
import org.chimeramc.client.util.MainThread;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The Replay system's state machine and the one entry point the UI talks to.
 *
 * <p>It owns the permission round-trip, starts and stops {@link ReplayCaptureService}, tracks the
 * elapsed recording time, applies the storage cap after a clip lands, and fans state changes out to
 * whatever Replay UI is open. Every host shares it — the Replay page of the Mod Menu is
 * one feature with one backend, so there is a single recorder state, not one per screen.
 *
 * <p>State lives on the main thread; a listener is invoked on the main thread. The class is a
 * process singleton so a screen that opens mid-recording immediately sees the running state.
 */
public final class ReplayManager {

    private static final String TAG = "ReplayManager";

    /** Request code for the screen-capture consent dialog. */
    public static final int REQUEST_CODE = 0x52A1;

    /** The recording state the UI renders. */
    public enum State {
        IDLE,
        RECORDING,
        SAVING
    }

    /** Receives state changes; every method runs on the main thread. */
    public interface Listener {
        void onReplayStateChanged(State state, long elapsedMs, ReplayQuality.Profile profile);

        /**
         * A capture ended without producing a clip.
         *
         * <p>Distinct from a state change on purpose: a failure drops the state straight back to
         * {@link State#IDLE}, which is byte-for-byte the state the screen is in before the user
         * ever pressed Record. Without this callback the panel could not tell the two apart, so a
         * recording that failed on the first frame looked like the press being ignored, and the
         * "recording could not start" message had no caller at all.
         */
        default void onReplayCaptureFailed() {
        }
    }

    private static volatile Context holderContext;

    /** The singleton, or null before {@link #init} has run. Never throws. */
    public static ReplayManager get() {
        ReplayManager local = instance;
        if (local != null) return local;
        Context context = holderContext;
        if (context == null) return null;
        synchronized (ReplayManager.class) {
            if (instance == null) {
                instance = new ReplayManager(context);
            }
            return instance;
        }
    }

    /** Called once from Application so the singleton can be built before any screen needs it. */
    public static void init(Context context) {
        holderContext = context.getApplicationContext();
        get();
    }

    /** The launcher activity a capture notification should open. */
    public static Class<?> launcherActivityClass() {
        return org.chimeramc.client.ui.activities.MainActivity.class;
    }

    private static volatile ReplayManager instance;

    private final Context appContext;
    private final Handler mainHandler = MainThread.handler();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final ReplayClipRepository repository;

    private State state = State.IDLE;
    private long startedAtMs;
    private ReplayQuality.Profile profile;
    private ReplayHighlightTrigger trigger;
    private Activity foregroundActivity;
    private ReplayMetadataOverlay metadataOverlay;

    /**
     * The capture engine for the clip in flight: the gameplay track, the markers and the metadata.
     *
     * <p>One object owns all three so the recorder is the single place a recording is assembled, and
     * the finished {@link ReplayDocument} it produces is both what the sidecar is written from and
     * what the portable {@code .glowreplay} file is.
     */
    private ReplayRecorder recorder;

    private final Runnable trackTick = new Runnable() {
        @Override
        public void run() {
            ReplayRecorder active = recorder;
            if (active == null || state != State.RECORDING) return;
            long elapsed = elapsedMs();
            active.tick(elapsed);
            sampleVoicePeers(active, elapsed);
            // Re-arm on the same cadence the track stores at, so the loop does no work the track
            // would drop.
            mainHandler.postDelayed(this, ReplayTrack.MIN_SAMPLE_INTERVAL_MS);
        }
    };

    private ReplayManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.repository = new ReplayClipRepository(appContext);
    }

    public State state() {
        return state;
    }

    public boolean isRecording() {
        return state == State.RECORDING;
    }

    /** Elapsed recording time, or zero when idle. */
    public long elapsedMs() {
        return state == State.RECORDING ? Math.max(0L, System.currentTimeMillis() - startedAtMs) : 0L;
    }

    public ReplayQuality.Profile profile() {
        return profile;
    }

    public void addListener(Listener listener) {
        if (listener != null && !listeners.contains(listener)) listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    /** Keeps the activity that should receive the consent dialog result. */
    public void setForegroundActivity(Activity activity) {
        this.foregroundActivity = activity;
    }

    /**
     * Asks for screen-capture consent, then starts the recorder.
     *
     * <p>The consent dialog is a platform activity result, so the caller must forward
     * {@code onActivityResult} to {@link #onActivityResult}. The activity is remembered here so the
     * Replay tab can request consent without holding its own copy.
     */
    public void requestStart(Activity activity) {
        if (state != State.IDLE) return;
        setForegroundActivity(activity);
        try {
            MediaProjectionManager manager = (MediaProjectionManager)
                    activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            if (manager == null) {
                return;
            }
            activity.startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CODE);
        } catch (Throwable t) {
            Log.w(TAG, "Unable to request screen capture", t);
        }
    }

    /**
     * Handles the consent result.
     *
     * @return true when this manager consumed the result
     */
    public static boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE) return false;
        ReplayManager manager = get();
        if (resultCode == Activity.RESULT_OK && data != null) {
            Context context = manager.foregroundActivity != null
                    ? manager.foregroundActivity : holderContext;
            ReplayCaptureService.start(context, resultCode, data);
        }
        return true;
    }

    /** Stops a running capture. */
    public void stop() {
        if (state != State.RECORDING) return;
        ReplayCaptureService.stop(appContext);
    }

    // ---------------------------------------------------------------------------------------------
    // Capture callbacks (called by the service)
    // ---------------------------------------------------------------------------------------------

    public void onRecordingStarted(File file, ReplayQuality.Profile profile, long startedAtMs) {
        this.startedAtMs = startedAtMs;
        this.profile = profile;
        this.state = State.RECORDING;
        this.trigger = ReplaySettings.get(appContext).newTrigger();
        startTrackRecording(startedAtMs);
        attachMetadataOverlay(startedAtMs);
        notifyState();
    }

    /**
     * Starts sampling the local-player feed into the clip's gameplay track.
     *
     * <p>This is what turns the capture into a replay: the video shows what the player saw, the
     * track records where they were so the free camera has a pose to detach from and the timeline
     * can be read as movement. It reads the same feed the HUD uses and degrades to an empty track
     * (the clip still plays as video) when no live read is available.
     */
    private void startTrackRecording(long startedAtMs) {
        recorder = new ReplayRecorder(new ReplayTrackRecorder.Source() {
            @Override
            public float[] position() {
                return PreloaderInput.readLocalPlayerPosition();
            }

            @Override
            public float[] rotation() {
                return PreloaderInput.readLocalPlayerRotation();
            }

            @Override
            public float[] health() {
                return PreloaderInput.readLocalPlayerHealth();
            }
        });
        // The recording starts when the world was already joined, so the opening marker is the start
        // of the recording itself — the one moment the client knows for certain. The skin id is the
        // account's, so a shared replay names the player it belongs to.
        recorder.start(startedAtMs, gameVersionForClip(), "", accountSkinId());
        // Sample immediately so a short clip still carries a starting pose, then keep sampling.
        recorder.tick(0L);
        mainHandler.removeCallbacks(trackTick);
        mainHandler.postDelayed(trackTick, ReplayTrack.MIN_SAMPLE_INTERVAL_MS);
    }

    /** The selected instance's version code, or empty when none is selected. */
    private String gameVersionForClip() {
        try {
            org.chimeramc.client.core.versions.GameVersion version =
                    org.chimeramc.client.core.versions.VersionManager.get(appContext)
                            .getSelectedVersion();
            return version == null ? "" : version.versionCode;
        } catch (Throwable t) {
            return "";
        }
    }

    /** The signed-in account's skin texture id, or empty. Used as the recording's player skin. */
    private String accountSkinId() {
        try {
            org.chimeramc.client.core.cosmetics.AccountSkinCache.Entry entry =
                    org.chimeramc.client.core.cosmetics.AccountSkinCache.read(
                            appContext.getFilesDir());
            if (entry == null) return "";
            String key = entry.accountKey;
            return key.startsWith("account:") ? key.substring("account:".length()) : key;
        } catch (Throwable t) {
            return "";
        }
    }

    /**
     * Shows the burn-in layer while recording.
     *
     * <p>A full-screen capture records whatever is on screen, so the metadata the spec asks to burn
     * in has to be drawn during the capture rather than encoded afterwards. The layer attaches to
     * the activity that requested the recording and is removed when the capture ends.
     */
    private void attachMetadataOverlay(long startedAtMs) {
        ReplaySettings settings = ReplaySettings.get(appContext);
        if (!settings.burnMetadata() && !settings.watermark()) return;
        if (foregroundActivity == null) return;
        try {
            metadataOverlay = new ReplayMetadataOverlay(foregroundActivity);
            metadataOverlay.attach(settings.burnMetadata(), settings.watermark(), startedAtMs);
        } catch (Throwable t) {
            Log.w(TAG, "Unable to attach the replay metadata layer", t);
            metadataOverlay = null;
        }
    }

    private void detachMetadataOverlay() {
        if (metadataOverlay != null) {
            try {
                metadataOverlay.detach();
            } catch (Throwable ignored) {
                AppLog.warnOnce(TAG, "ReplayManager:282", "Ignored a failure at ReplayManager:282", ignored);
            }
            metadataOverlay = null;
        }
    }

    public void onRecordingFinished(File file, long durationMs) {
        detachMetadataOverlay();
        ReplayRecorder finished = stopTrackRecording();
        // The document is built before the state resets so it carries the set that belongs to this
        // clip, not whatever a later recording leaves behind.
        final ReplayDocument document = finished == null
                ? ReplayDocument.of(System.currentTimeMillis(), "", "", "")
                : finished.finish();
        this.state = State.SAVING;
        notifyState();
        final ReplayQuality.Profile finishedProfile = profile;
        // The sidecar, the portable container and the storage prune are file work; keep them off the
        // main thread.
        AppExecutors.run(AppExecutors.Kind.DISK, "replay-finalize", () -> {
            try {
                writeSidecar(file, durationMs, finishedProfile, document);
                writePortableReplay(file, document);
                pruneIfNeeded();
            } catch (Throwable t) {
                Log.w(TAG, "Post-recording bookkeeping failed", t);
            } finally {
                mainHandler.post(() -> {
                    state = State.IDLE;
                    profile = null;
                    notifyState();
                });
            }
        });
    }

    public void onRecordingFailed() {
        detachMetadataOverlay();
        stopTrackRecording();
        state = State.IDLE;
        profile = null;
        notifyState();
        notifyCaptureFailed();
    }

    /**
     * Stops sampling and returns the track captured so far.
     *
     * <p>The tick is cancelled first so no sample lands after this point, then the recorder is
     * released. Returns an empty track when no recording was in flight, which the sidecar writes as
     * "no track" rather than an empty string that would look like a corrupt one.
     */
    private ReplayRecorder stopTrackRecording() {
        mainHandler.removeCallbacks(trackTick);
        ReplayRecorder finished = recorder;
        recorder = null;
        return finished;
    }

    private void notifyCaptureFailed() {
        mainHandler.post(() -> {
            for (Listener listener : listeners) {
                try {
                    listener.onReplayCaptureFailed();
                } catch (Throwable t) {
                    Log.w(TAG, "Replay listener threw", t);
                }
            }
        });
    }

    private void writeSidecar(File file, long durationMs, ReplayQuality.Profile profile,
                              ReplayDocument document) {
        ReplaySidecar sidecar = ReplaySidecar.of(System.currentTimeMillis(),
                document.world(), document.gameVersion(), document.gameMode());
        sidecar.set("durationMs", String.valueOf(durationMs));
        sidecar.set("resolution", profile == null ? "" : profile.label());
        ReplayTrack track = document.track();
        if (track != null && !track.isEmpty()) {
            sidecar.setTrack(track);
        }
        ReplayMarkers clipMarkers = document.markers();
        if (clipMarkers != null && !clipMarkers.isEmpty()) {
            sidecar.setMarkers(clipMarkers);
        }
        ReplayCameraPath path = document.cameraPath();
        if (path != null && !path.isEmpty()) {
            sidecar.setCameraPath(path);
        }
        ReplayStorage.writeSidecar(file, sidecar.serialize());
    }

    /**
     * Writes the portable {@code .glowreplay} container beside the clip.
     *
     * <p>The sidecar is the launcher's internal, per-clip metadata; the container is the replay as a
     * single movable file — the metadata, the track, the markers and the camera path in one
     * self-describing document. It sits beside the MP4 with the same base name, so the library lists
     * both and the clip can be shared or moved as a replay rather than only as a video.
     */
    private void writePortableReplay(File clipFile, ReplayDocument document) {
        if (clipFile == null || document == null) return;
        try {
            String name = clipFile.getName();
            int dot = name.lastIndexOf('.');
            String base = dot > 0 ? name.substring(0, dot) : name;
            ReplayDocument.write(clipFile.getParentFile(), base, document);
        } catch (Throwable t) {
            Log.w(TAG, "Could not write the portable replay container", t);
        }
    }

    /** Applies the storage cap: prunes the oldest non-favorited clips so the new one fits. */
    private void pruneIfNeeded() {
        ReplaySettings settings = ReplaySettings.get(appContext);
        long cap = settings.storageCapBytes();
        List<ReplayClip> clips = repository.scan();
        long used = ReplayStorage.usageBytes(appContext);
        List<ReplayClip> prunable =
                ReplayStoragePolicy.selectPrunable(clips, used, cap, 0L);
        repository.deleteAll(prunable);
    }

    // ---------------------------------------------------------------------------------------------
    // Highlight triggers (Tier 4)
    // ---------------------------------------------------------------------------------------------

    public void onDeath() {
        recordMarker(ReplayMarkers.Kind.DEATH, "");
        fireHighlight(ReplayHighlightTrigger.Event.death());
    }

    public void onKillStreak(int streak) {
        recordMarker(ReplayMarkers.Kind.KILL_STREAK, streak > 0 ? String.valueOf(streak) : "");
        fireHighlight(ReplayHighlightTrigger.Event.killStreak(streak));
    }

    public void onCombo(int hits) {
        recordMarker(ReplayMarkers.Kind.COMBO, hits > 0 ? String.valueOf(hits) : "");
        fireHighlight(ReplayHighlightTrigger.Event.combo(hits));
    }

    /**
     * Records a marker at the current elapsed time while a capture is running.
     *
     * <p>Only a live recording carries markers: a trigger that fires with no capture in flight has
     * no clip to attach to, and adding it to the next recording would put an event on a timeline it
     * did not happen on.
     */
    /**
     * Samples the proximity-voice peers' advertised poses into the recording's entity track.
     *
     * <p>The build cannot read other <em>game</em> entities (the game classes are stripped of
     * symbols), so the only honest source of another player's position is the voice protocol, which
     * already advertises it for the nametag icons. Recording those gives a replay the other players
     * it can actually show, and nothing is invented when the voice module is down: the entity track
     * is simply empty.
     */
    private void sampleVoicePeers(ReplayRecorder active, long elapsedMs) {
        try {
            java.util.List<org.chimeramc.client.core.voice.VoicePeer> peers =
                    org.chimeramc.client.core.mods.inbuilt.overlay.PeerHitboxSource
                            .readPeers();
            for (org.chimeramc.client.core.voice.VoicePeer peer : peers) {
                if (peer == null) continue;
                active.sampleEntity(elapsedMs, peer.id, peer.x, peer.y, peer.z,
                        peer.yaw, peer.pitch);
            }
        } catch (Throwable t) {
            // The entity track is best-effort: a voice failure must never disturb the recording.
            AppLog.warnOnce(TAG, "ReplayManager:voicePeers",
                    "Ignored a failure sampling the voice peers", t);
        }
    }

    private void recordMarker(ReplayMarkers.Kind kind, String detail) {
        if (state != State.RECORDING) return;
        ReplayRecorder active = recorder;
        if (active == null) return;
        active.addMarker(elapsedMs(), kind, detail);
    }

    /**
     * Marks a highlight on the most recent clip when a trigger fires.
     *
     * <p>This build has no rolling ring buffer of encoded frames, so a trigger flags the clip the
     * recorder is currently writing (or the newest clip) rather than slicing the last 30 seconds
     * out of a buffer. That limitation is stated in the UI's scope note instead of being implied
     * away; the trigger decision itself is real and unit-tested.
     */
    private void fireHighlight(ReplayHighlightTrigger.Event event) {
        ReplayHighlightTrigger local = trigger;
        if (local == null) return;
        if (!local.shouldFire(event, SystemClock.uptimeMillis())) return;
        mainHandler.post(() -> {
            List<ReplayClip> clips = repository.scan();
            if (clips.isEmpty()) return;
            ReplayClip newest = clips.get(0);
            newest.setHighlight(true);
            repository.updateSidecar(newest);
        });
    }

    private void notifyState() {
        final State snapshotState = state;
        final long elapsed = elapsedMs();
        final ReplayQuality.Profile snapshotProfile = profile;
        mainHandler.post(() -> {
            for (Listener listener : listeners) {
                try {
                    listener.onReplayStateChanged(snapshotState, elapsed, snapshotProfile);
                } catch (Throwable t) {
                    Log.w(TAG, "Replay listener threw", t);
                }
            }
        });
    }
}

package org.chimeramc.client.core.mods.inbuilt.overlay;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.chimeramc.client.R;
import org.chimeramc.client.databinding.OverlayGyroButtonBinding;
import org.chimeramc.client.core.mods.inbuilt.manager.InbuiltModManager;
import org.chimeramc.client.core.mods.inbuilt.model.ModIds;
import org.chimeramc.client.launcher.core.mods.inbuilt.nativemod.PojavControlsMod;
import org.chimeramc.client.util.MainThread;

/**
 * Gyroscope camera control.
 *
 * <p>The sensor reading is turned into a look delta and pushed through the <em>same</em> path the
 * game already uses for touch and mouse look ({@code PojavControlsMod.nativeSendLookDelta}), rather
 * than a separate native gyro hook. That path is exercised by every mouse-look frame, so it works
 * in worlds, Realms and on servers alike; a bespoke native hook would have to resolve a per-build
 * game address and silently do nothing when it failed. Sensitivity, invert and dead zone are
 * applied here in Java, so the module needs no native gyro symbol at all.
 */
public class GyroOverlay extends BaseOverlayButton implements SensorEventListener {
    /**
     * Look pixels produced by one radian of device rotation at sensitivity 1.0. Chosen so the
     * default sensitivity feels roughly like a mouse at normal DPI; the config scales it.
     */
    private static final float PIXELS_PER_RADIAN = 220f;

    private boolean isActive = false;
    private boolean initialized = false;
    private final Handler handler = MainThread.handler();

    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private int sensorType;

    // Settings, mirrored into plain fields so the sensor callback (which runs on the sensor
    // thread) never touches SharedPreferences.
    private float sensitivityX = 1f;
    private float sensitivityY = 1f;
    private boolean invertX = false;
    private boolean invertY = false;
    private float deadzoneRadians = 0f;

    private final float[] referenceRotationMatrix = new float[9];
    private final float[] inverseReferenceMatrix = new float[9];
    private boolean hasReference = false;

    // Bias capture and live dead-zone preview.
    private static final long LONG_PRESS_MS = 450L;
    private static final long STATE_LABEL_INTERVAL_MS = 100L;
    private final GyroCalibration calibration = new GyroCalibration();
    private float biasX = 0f;
    private float biasY = 0f;
    private float biasZ = 0f;
    private float lastMagnitude = 0f;
    private long lastLabelUpdate = 0L;
    private ImageView gyroIcon;
    private TextView gyroState;
    private final Runnable longPress = this::beginCalibration;

    private final float[] currentRotationMatrix = new float[9];
    private final float[] deltaRotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];

    private float prevDeltaYaw = 0f;
    private float prevDeltaPitch = 0f;

    public GyroOverlay(Activity activity) {
        super(activity);
        sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        selectBestSensor();
    }

    private void selectBestSensor() {
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        if (rotationSensor != null) {
            sensorType = Sensor.TYPE_GAME_ROTATION_VECTOR;
            return;
        }

        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (rotationSensor != null) {
            sensorType = Sensor.TYPE_ROTATION_VECTOR;
            return;
        }

        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (rotationSensor != null) {
            sensorType = Sensor.TYPE_GYROSCOPE;
            return;
        }
    }

    @Override
    protected String getModId() {
        return ModIds.GYRO;
    }

    @Override
    protected int getIconResource() {
        return isActive ? R.drawable.ic_gyro_pressed : R.drawable.ic_gyro_normal;
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.overlay_gyro_button;
    }

    @Override
    protected void configureOverlayView(View view) {
        OverlayGyroButtonBinding binding = OverlayGyroButtonBinding.bind(view);
        gyroIcon = binding.gyroIcon;
        gyroState = binding.gyroState;
        updateStateLabel();
    }

    @Override
    public void show(int startX, int startY) {
        if (!initialized) {
            initializeNative();
        }
        super.show(startX, startY);
    }

    public void initializeForKeyboard() {
        if (!initialized) {
            initializeNative();
        }
    }

    private void initializeNative() {
        // "Initialising" now means the shared look path is reachable. PojavControlsMod is a no-op
        // when its library is missing, so the module degrades to doing nothing rather than
        // crashing, and the toggle simply has no effect on such a build.
        handler.postDelayed(() -> {
            PojavControlsMod.initialize();
            initialized = true;
            applyGyroSettings();
        }, 1000);
    }

    private void applyGyroSettings() {
        InbuiltModManager manager = InbuiltModManager.getInstance(activity);
        sensitivityX = manager.getGyroSensitivityX() / 100f;
        sensitivityY = manager.getGyroSensitivityY() / 100f;
        invertX = manager.isGyroInvertX();
        invertY = manager.isGyroInvertY();
        deadzoneRadians = (float) Math.toRadians(manager.getGyroDeadzone() / 10f);
    }

    @Override
    protected void onButtonClick() {
        if (!initialized) {
            return;
        }

        if (rotationSensor == null) {
            return;
        }

        if (isActive) {
            disableGyro();
        } else {
            enableGyro();
        }
    }

    @Override
    protected void onButtonPressStart() {
        // Long press = calibrate, so the control stays one button.
        handler.postDelayed(longPress, LONG_PRESS_MS);
    }

    @Override
    protected void onButtonPressEnd() {
        handler.removeCallbacks(longPress);
    }

    private void enableGyro() {
        isActive = true;
        hasReference = false;
        prevDeltaYaw = 0f;
        prevDeltaPitch = 0f;

        sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
        updateButtonState(true);
    }

    private void disableGyro() {
        isActive = false;
        hasReference = false;

        sensorManager.unregisterListener(this);
        updateButtonState(false);
    }

    private void calibrate() {
        hasReference = false;
        prevDeltaYaw = 0f;
        prevDeltaPitch = 0f;
    }

    /**
     * Starts a bias capture: the player holds the device still for ~0.3s and the mean
     * gyro rate over that window becomes the offset subtracted from every later sample. Only
     * meaningful on a raw gyroscope; a rotation-vector sensor is already fused and drift-free, so
     * on that path the capture just re-zeros the reference pose.
     */
    private void beginCalibration() {
        if (!isActive || rotationSensor == null) return;
        if (sensorType != Sensor.TYPE_GYROSCOPE) {
            // Fused sensor: "calibrate" means re-zero the reference orientation.
            calibrate();
            return;
        }
        calibration.begin();
        updateStateLabel();
    }

    /** Commits a finished capture, storing the mean rate as the bias. */
    private void commitCalibration() {
        if (!calibration.isActive()) return;
        calibration.stop();
        if (calibration.canCommit()) {
            biasX = calibration.biasX();
            biasY = calibration.biasY();
            biasZ = calibration.biasZ();
        }
        updateStateLabel();
    }

    /** The dead-zone threshold in the same units the label shows (degrees of turn per sample). */
    private float deadzoneDegrees() {
        return (float) Math.toDegrees(deadzoneRadians);
    }

    /**
     * Shows the raw turn magnitude against the dead-zone threshold, so the dead zone is a live
     * preview. Throttled to {@link #STATE_LABEL_INTERVAL_MS}: the sensor runs at
     * SENSOR_DELAY_GAME (~50Hz), and setting text every sample would thrash the label.
     */
    private void updateStateLabel() {
        if (gyroState == null) return;
        if (calibration.isActive()) {
            gyroState.setText(activity.getString(R.string.mod_overlay_gyro_calibrating));
            return;
        }
        if (!isActive) {
            gyroState.setText(R.string.mod_overlay_gyro_off);
            return;
        }
        gyroState.setText(activity.getString(R.string.mod_overlay_gyro_level,
                lastMagnitude, deadzoneDegrees()));
    }

    private void updateButtonState(boolean active) {
        float userOpacity = getButtonOpacity();
        if (gyroIcon != null) {
            gyroIcon.setAlpha(userOpacity);
            gyroIcon.setImageResource(active ? R.drawable.ic_gyro_pressed : R.drawable.ic_gyro_normal);
        } else if (overlayView instanceof ImageButton) {
            ImageButton btn = (ImageButton) overlayView;
            btn.setAlpha(userOpacity);
            btn.setImageResource(active ? R.drawable.ic_gyro_pressed : R.drawable.ic_gyro_normal);
        }
        updateStateLabel();
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isActive || !initialized) return;

        if (sensorType == Sensor.TYPE_GYROSCOPE) {
            handleGyroscopeEvent(event);
        } else {
            handleRotationVectorEvent(event);
        }
    }

    private void handleRotationVectorEvent(SensorEvent event) {
        SensorManager.getRotationMatrixFromVector(currentRotationMatrix, event.values);

        if (!hasReference) {
            System.arraycopy(currentRotationMatrix, 0, referenceRotationMatrix, 0, 9);
            invertMatrix3x3(referenceRotationMatrix, inverseReferenceMatrix);
            hasReference = true;
            return;
        }

        multiplyMatrix3x3(inverseReferenceMatrix, currentRotationMatrix, deltaRotationMatrix);

        float rotX = (deltaRotationMatrix[7] - deltaRotationMatrix[5]) / 2.0f;
        float rotY = (deltaRotationMatrix[2] - deltaRotationMatrix[6]) / 2.0f;
        float rotZ = (deltaRotationMatrix[3] - deltaRotationMatrix[1]) / 2.0f;

        float deltaYaw = 0f;
        float deltaPitch = 0f;

        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
            case android.view.Surface.ROTATION_0:
                deltaPitch = -rotX;
                deltaYaw = rotY;
                break;
            case android.view.Surface.ROTATION_90:
                deltaPitch = -rotY;
                deltaYaw = -rotX;
                break;
            case android.view.Surface.ROTATION_180:
                deltaPitch = rotX;
                deltaYaw = -rotY;
                break;
            case android.view.Surface.ROTATION_270:
                deltaPitch = rotY;
                deltaYaw = rotX;
                break;
        }

        float smoothFactor = 0.5f;
        deltaYaw = smoothFactor * deltaYaw + (1f - smoothFactor) * prevDeltaYaw;
        deltaPitch = smoothFactor * deltaPitch + (1f - smoothFactor) * prevDeltaPitch;
        prevDeltaYaw = deltaYaw;
        prevDeltaPitch = deltaPitch;

        System.arraycopy(currentRotationMatrix, 0, referenceRotationMatrix, 0, 9);
        invertMatrix3x3(referenceRotationMatrix, inverseReferenceMatrix);

        lastMagnitude = (float) Math.toDegrees(Math.hypot(deltaYaw, deltaPitch));
        maybeRefreshLabel();
        sendLookDelta(deltaYaw, deltaPitch);
    }

    private void handleGyroscopeEvent(SensorEvent event) {
        float dt = 0.02f;

        // Feed the raw sample to a running calibration and, once it is armed, treat this sample as
        // calibration input rather than look input -- so the player holding the device
        // still does not also drag the camera while the bias is being measured.
        if (calibration.isActive()) {
            calibration.add(event.values[0], event.values[1], event.values[2]);
            if (calibration.isReady()) commitCalibration();
            return;
        }

        // Subtract the calibrated bias so a still device reports no turn and the camera does not
        // creep. Zero until the player calibrates, so the default behaviour is unchanged.
        float gyroX = event.values[0] - biasX;
        float gyroY = event.values[1] - biasY;
        float gyroZ = event.values[2] - biasZ;

        float deltaYaw = 0f;
        float deltaPitch = 0f;

        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
            case android.view.Surface.ROTATION_0:
                deltaPitch = -gyroX * dt;
                deltaYaw = gyroY * dt;
                break;
            case android.view.Surface.ROTATION_90:
                deltaPitch = -gyroY * dt;
                deltaYaw = -gyroX * dt;
                break;
            case android.view.Surface.ROTATION_180:
                deltaPitch = gyroX * dt;
                deltaYaw = -gyroY * dt;
                break;
            case android.view.Surface.ROTATION_270:
                deltaPitch = gyroY * dt;
                deltaYaw = gyroX * dt;
                break;
        }

        lastMagnitude = (float) Math.toDegrees(Math.hypot(deltaYaw, deltaPitch));
        maybeRefreshLabel();
        sendLookDelta(deltaYaw, deltaPitch);
    }

    /** Refreshes the live level label at most every {@link #STATE_LABEL_INTERVAL_MS}. */
    private void maybeRefreshLabel() {
        long now = SystemClock.uptimeMillis();
        if (now - lastLabelUpdate < STATE_LABEL_INTERVAL_MS) return;
        lastLabelUpdate = now;
        if (!calibration.isActive()) updateStateLabel();
    }

    /**
     * Applies the user's sensitivity, invert and dead-zone settings and pushes the result to the
     * shared look path. The dead zone suppresses slow drift while the device is nearly still; it is
     * applied to the raw delta magnitude, not per axis, so a slow diagonal wobble is filtered too.
     */
    private void sendLookDelta(float deltaYaw, float deltaPitch) {
        float magnitude = (float) Math.hypot(deltaYaw, deltaPitch);
        if (magnitude < deadzoneRadians) {
            return;
        }
        float dx = deltaYaw * sensitivityX * PIXELS_PER_RADIAN * (invertX ? -1f : 1f);
        float dy = deltaPitch * sensitivityY * PIXELS_PER_RADIAN * (invertY ? -1f : 1f);
        if (dx == 0f && dy == 0f) return;
        // Same path the mouse/touch look uses, so it reaches the game identically in every mode.
        // Guarded because this runs on the sensor thread: a build without the library must degrade
        // to "gyro does nothing", not kill the sensor thread with an UnsatisfiedLinkError.
        try {
            PojavControlsMod.nativeSendLookDelta(dx, dy);
        } catch (Throwable ignored) {
            // No native look path on this build.
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed
    }

    @Override
    public void hide() {
        // Drop the pending native init: hiding before it fires must not leave the runnable (and
        // this overlay) referenced, and must not initialise a module the user just switched off.
        handler.removeCallbacksAndMessages(null);
        handler.removeCallbacks(longPress);
        calibration.stop();
        if (isActive && initialized) {
            disableGyro();
        }
        gyroIcon = null;
        gyroState = null;
        super.hide();
    }

    @Override
    public void applyConfigurationChanges() {
        super.applyConfigurationChanges();
        if (initialized) {
            applyGyroSettings();
        }
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void toggleGyro() {
        if (!initialized || rotationSensor == null) return;
        if (isActive) {
            disableGyro();
        } else {
            enableGyro();
        }
    }

    private static void invertMatrix3x3(float[] m, float[] out) {
        out[0] = m[0]; out[1] = m[3]; out[2] = m[6];
        out[3] = m[1]; out[4] = m[4]; out[5] = m[7];
        out[6] = m[2]; out[7] = m[5]; out[8] = m[8];
    }

    private static void multiplyMatrix3x3(float[] a, float[] b, float[] result) {
        result[0] = a[0]*b[0] + a[1]*b[3] + a[2]*b[6];
        result[1] = a[0]*b[1] + a[1]*b[4] + a[2]*b[7];
        result[2] = a[0]*b[2] + a[1]*b[5] + a[2]*b[8];

        result[3] = a[3]*b[0] + a[4]*b[3] + a[5]*b[6];
        result[4] = a[3]*b[1] + a[4]*b[4] + a[5]*b[7];
        result[5] = a[3]*b[2] + a[4]*b[5] + a[5]*b[8];

        result[6] = a[6]*b[0] + a[7]*b[3] + a[8]*b[6];
        result[7] = a[6]*b[1] + a[7]*b[4] + a[8]*b[7];
        result[8] = a[6]*b[2] + a[7]*b[5] + a[8]*b[8];
    }
}

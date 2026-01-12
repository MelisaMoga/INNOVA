package com.melisa.innovamotionapp.utils;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Detects shake gestures using the device accelerometer.
 * 
 * Used to activate the developer panel in debug builds.
 * Only registers for sensor events when {@link FeatureFlags#DEV_MODE_ENABLED} is true.
 * 
 * Usage:
 * <pre>
 * DevShakeDetector detector = new DevShakeDetector(context, () -> {
 *     // Show developer panel
 * });
 * 
 * // In onResume:
 * detector.start();
 * 
 * // In onPause:
 * detector.stop();
 * </pre>
 */
public class DevShakeDetector implements SensorEventListener {
    
    private static final String TAG = "DevShakeDetector";
    
    // Shake detection thresholds
    private static final float SHAKE_THRESHOLD_GRAVITY = 2.5f; // ~2.5g acceleration
    private static final int SHAKE_SLOP_TIME_MS = 500; // Time between shakes
    private static final int SHAKE_COUNT_RESET_TIME_MS = 3000; // Reset counter after 3s
    private static final int REQUIRED_SHAKE_COUNT = 3; // Shakes needed to trigger
    private static final long DEBOUNCE_MS = 2000; // Debounce between activations
    
    private final Context context;
    private final OnShakeListener listener;
    private final SensorManager sensorManager;
    private final Sensor accelerometer;
    
    private long lastShakeTime = 0;
    private int shakeCount = 0;
    private long lastTriggerTime = 0;
    private boolean isRegistered = false;
    
    /**
     * Callback interface for shake detection.
     */
    public interface OnShakeListener {
        void onShakeDetected();
    }
    
    /**
     * Create a new shake detector.
     * 
     * @param context Application or Activity context
     * @param listener Callback invoked when shake is detected
     */
    public DevShakeDetector(Context context, OnShakeListener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        this.sensorManager = (SensorManager) this.context.getSystemService(Context.SENSOR_SERVICE);
        this.accelerometer = sensorManager != null ? 
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) : null;
    }
    
    /**
     * Start listening for shake gestures.
     * Only registers if DEV_MODE_ENABLED is true.
     */
    public void start() {
        if (!FeatureFlags.DEV_MODE_ENABLED) {
            Logger.d(TAG, "Dev mode disabled, not registering shake detector");
            return;
        }
        
        if (isRegistered) {
            Logger.d(TAG, "Already registered");
            return;
        }
        
        if (sensorManager == null || accelerometer == null) {
            Logger.w(TAG, "Accelerometer not available on this device");
            return;
        }
        
        boolean success = sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_UI
        );
        
        if (success) {
            isRegistered = true;
            Logger.d(TAG, "Shake detector started");
        } else {
            Logger.w(TAG, "Failed to register accelerometer listener");
        }
    }
    
    /**
     * Stop listening for shake gestures.
     */
    public void stop() {
        if (!isRegistered) {
            return;
        }
        
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            isRegistered = false;
            Logger.d(TAG, "Shake detector stopped");
        }
    }
    
    /**
     * Check if the detector is currently active.
     */
    public boolean isActive() {
        return isRegistered;
    }
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
            return;
        }
        
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        
        // Calculate acceleration magnitude relative to gravity
        float gX = x / SensorManager.GRAVITY_EARTH;
        float gY = y / SensorManager.GRAVITY_EARTH;
        float gZ = z / SensorManager.GRAVITY_EARTH;
        
        // Calculate total G-force
        float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);
        
        if (gForce > SHAKE_THRESHOLD_GRAVITY) {
            long now = System.currentTimeMillis();
            
            // Reset shake count if too long since last shake
            if (now - lastShakeTime > SHAKE_COUNT_RESET_TIME_MS) {
                shakeCount = 0;
            }
            
            // Ignore if shakes are too close together (slop time)
            if (now - lastShakeTime > SHAKE_SLOP_TIME_MS) {
                lastShakeTime = now;
                shakeCount++;
                
                Logger.v(TAG, "Shake detected: count=" + shakeCount + ", gForce=" + gForce);
                
                if (shakeCount >= REQUIRED_SHAKE_COUNT) {
                    // Check debounce
                    if (now - lastTriggerTime > DEBOUNCE_MS) {
                        lastTriggerTime = now;
                        shakeCount = 0;
                        
                        Logger.i(TAG, "Shake gesture recognized - triggering callback");
                        if (listener != null) {
                            listener.onShakeDetected();
                        }
                    } else {
                        Logger.d(TAG, "Shake ignored (debounce)");
                    }
                }
            }
        }
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }
}

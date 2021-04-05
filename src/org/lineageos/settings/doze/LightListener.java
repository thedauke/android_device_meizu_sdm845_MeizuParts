

package org.lineageos.settings.doze;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class LightListener implements SensorEventListener {

    private static final String TAG = "LightListener";
    private static final boolean DEBUG = false;

    private final DozeService mService;

    private final SensorManager mSensorManager;
    private final Sensor mSensor;
    private ExecutorService mExecutorService;
    private boolean mIsListening = false;
    
    public float currentLux = 0;

    LightListener(DozeService service) {
        mService = service;
        mSensorManager = (SensorManager) service.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT, false);
        mExecutorService = Executors.newSingleThreadExecutor();
    }

    private Future<?> submit(Runnable runnable) {
        return mExecutorService.submit(runnable);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        currentLux = event.values[0];
        mService.onChangedLuxState(currentLux);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    void enable() {
        int READINGRATE = 4000000; // time in us
        if (mIsListening) return;
        if (DEBUG) Log.d(TAG, "Enabling");
        mIsListening = true;
        submit(() -> {
            mSensorManager.registerListener(this, mSensor, READINGRATE);
        });
    }

    void disable() {
        if (!mIsListening) return;
        if (DEBUG) Log.d(TAG, "Disabling");
        mIsListening = false;
        submit(() -> {
            mSensorManager.unregisterListener(this, mSensor);
        });
    }

}
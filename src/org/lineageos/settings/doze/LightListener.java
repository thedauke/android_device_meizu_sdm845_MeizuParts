

package org.lineageos.settings.doze;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

class LightListener implements SensorEventListener {

    private static final String TAG = "LightListener";
    private static final boolean DEBUG = false;

    private final DozeService mService;

    private final SensorManager mSensorManager;
    private final Sensor mSensor;

    private boolean mIsListening = false;
    public float currentLux = 0;

    LightListener(DozeService service) {
        mService = service;
        mSensorManager = (SensorManager) service.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT, false);
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
        if (mIsListening) return;
        if (DEBUG) Log.d(TAG, "Enabling");
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        mIsListening = true;
    }

    void disable() {
        if (!mIsListening) return;
        if (DEBUG) Log.d(TAG, "Disabling");
        mSensorManager.unregisterListener(this, mSensor);
        mIsListening = false;
    }

}
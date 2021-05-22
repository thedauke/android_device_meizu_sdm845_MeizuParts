/*
 * Copyright (C) 2020 The MoKee Open Source Project
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.lineageos.settings.doze;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

class ProximityListener implements SensorEventListener {

    private static final String TAG = "ProximityListener";
    private static final boolean DEBUG = false;

    private final DozeService mService;

    private final SensorManager mSensorManager;
    private final Sensor mSensor;

    private boolean mIsListening = false;

    ProximityListener(DozeService service) {
        mService = service;
        mSensorManager = (SensorManager) service.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY, false);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.values[0] < mSensor.getMaximumRange()) {
            mService.onProximityNear();
        } else {
            mService.onProximityFar();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    void enable() {
        private static final int Covered_DELAY_MS =3500000; // Delay for update covered state time in us
        if (mIsListening) return;
        if (DEBUG) Log.d(TAG, "Enabling");
        mSensorManager.registerListener(this, mSensor, Covered_DELAY_MS);
        mIsListening = true;
    }

    void disable() {
        private static final int Uncovered_DELAY_MS=1100000; // Delay for update covered state time in us
        if (!mIsListening) return;
        if (DEBUG) Log.d(TAG, "Disabling");
        mSensorManager.unregisterListener(this, mSensor, Uncovered_DELAY_MS);
        mIsListening = false;
    }

}
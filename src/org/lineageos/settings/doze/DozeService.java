/*
 * Copyright (C) 2015 The CyanogenMod Project
 *               2017-2018 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.doze;

import android.os.FileUtils;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import android.os.Handler;
import android.os.FileUtils;
import android.os.Looper;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.io.IOException;
import android.os.PowerManager;
import android.view.WindowManager;

public class DozeService extends Service {
    private static final String TAG = "DozeService";
    private static final boolean DEBUG = false;

    private static final long AOD_DELAY_MS = 500; // Delay for enable AOD mode
    private static final long ExitAOD_DELAY_MS = 1100; // Delay for exit from AOD
    private static final long Brightness_DELAY_MS = 2300; // Delay for reaction to brightness changes  
    private static final long WakeUP_DELAY_MS = 80; // WakeUp delay after exit from AOD
    private static final long PULSE_RESTORE_DELAY_MS = 600; // maximum pulse notification time 0.6s

    private static final String PULSE_ACTION = "com.android.systemui.doze.pulse";

    private PickupSensor mPickupSensor;
    private ProximitySensor mProximitySensor;
    private ProximityListener mProximityListener;
    private IntentFilter mScreenStateFilter = new IntentFilter(PULSE_ACTION);

    private Handler mHandler = new Handler(Looper.getMainLooper());
    
    private boolean mInteractive = true;
    private boolean mCovered = false;


    @Override
    public void onCreate() {
        if (DEBUG) Log.d(TAG, "Creating service");
	    super.onCreate();
        mPickupSensor = new PickupSensor(this);
        mProximitySensor = new ProximitySensor(this);
	    mProximityListener = new ProximityListener(this);

        IntentFilter screenStateFilter = new IntentFilter();
        screenStateFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenStateFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenStateReceiver, screenStateFilter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.d(TAG, "Starting service");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "Destroying service");
        super.onDestroy();
        this.unregisterReceiver(mScreenStateReceiver);
        mProximitySensor.disable();
        mPickupSensor.disable();
        mProximityListener.disable();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void onDisplayOn() {
        if (DEBUG) Log.d(TAG, "Display on");
        mInteractive = true;
	    mHandler.removeCallbacksAndMessages(null);
        mProximityListener.disable();
        WakeupScreen();
        if (DozeUtils.isPickUpEnabled(this)) {
            mPickupSensor.disable();
        }
        if (DozeUtils.isHandwaveGestureEnabled(this) ||
             DozeUtils.isPocketGestureEnabled(this)) {
             mProximitySensor.disable();
        }
    }

    private void onDisplayOff() {
	    mInteractive = false;
        if (DEBUG) Log.d(TAG, "Display off");
            mHandler.postDelayed(() -> {
            updateAOD();
            }, AOD_DELAY_MS);
        if (DozeUtils.isPickUpEnabled(this)) {
            mPickupSensor.enable();
        }
        if (DozeUtils.isHandwaveGestureEnabled(this) || DozeUtils.isPocketGestureEnabled(this)) {
            mProximitySensor.enable();
        }
	    if (DozeUtils.isAlwaysOnEnabled(this)) {
		    mProximityListener.enable();
	    }
    }

    void onDozePulse() {
      Log.d(TAG, "Doze pulse state detected");
      mHandler.postDelayed(() -> {
          if (!mInteractive) {
              Log.d(TAG, "Doze pulse triggered AOD");
              EnterAOD();
          }
      }, PULSE_RESTORE_DELAY_MS);
    }

    void onProximityNear() {
        Log.d(TAG, "Device covered");
        mCovered = true;
        updateAOD();
    }

    void onProximityFar() {
        Log.d(TAG, "Device uncovered");
        mCovered = false;
        updateAOD();
    }

    private void updateAOD() {
        final boolean state = mCovered;
        final boolean mAOD = mInteractive;
        if ( mAOD == false ) {
            if (state == false) {
                Log.d(TAG, "Enter AOD");
                EnterAOD();
            } else {
                Log.d(TAG, "Exit AOD");
	        ExitAOD();
            }
        }
    }

    private void EnterAOD() {
        try {
            FileUtils.stringToFile("/sys/class/meizu/lcm/display/doze_s2", "0");
            } catch (IOException e) {
              Log.e(TAG, "FileUtils:Failed to Enter AOD");
            }
            mHandler.postDelayed(() -> {
        try {
            FileUtils.stringToFile("/sys/class/meizu/lcm/display/doze_mode", "1");
        } catch (IOException e) {
            Log.e(TAG, "FileUtils:Failed to switch doze_mode");
        }  
            }, ExitAOD_DELAY_MS);
            mHandler.postDelayed(() -> {
        try {
            FileUtils.stringToFile("/sys/class/meizu/lcm/display/doze_mode", "0");
        } catch (IOException e) {
            Log.e(TAG, "FileUtils:Failed to switch doze_mode");
        }  
            }, 16);
    }

    private void ExitAOD() {
        mHandler.postDelayed(() -> {
            try {
            FileUtils.stringToFile("/sys/class/meizu/lcm/display/aod", "0");
            }   catch (IOException e) {
            Log.e(TAG, "FileUtils:Failed to Exit AOD");
        }
       }, ExitAOD_DELAY_MS);
    }

    private void WakeupScreen() {
        mHandler.postDelayed(() -> {
            PowerManager powerManager = (PowerManager) getApplicationContext()
            .getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            PowerManager.ACQUIRE_CAUSES_WAKEUP, getPackageName() + ":Call");
            wakeLock.acquire();
        }, WakeUP_DELAY_MS);
    }
   
    private void EnterHBM() {
        mHandler.postDelayed(() -> {
            try {
            FileUtils.stringToFile("/sys/class/meizu/lcm/display/doze_mode", "1");
            } catch (IOException e) {
            Log.e(TAG, "FileUtils:Failed to EnterHBMAOD");
        }
       }, Brightness_DELAY_MS);
    }

    private void ExitHBM() {
        mHandler.postDelayed(() -> {
            try {
            FileUtils.stringToFile("/sys/class/meizu/lcm/display/doze_mode", "0");
            } catch (IOException e) {
            Log.e(TAG, "FileUtils:Failed to ExitHBMAOD");
        }
       }, Brightness_DELAY_MS);
    }

    private BroadcastReceiver mScreenStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                onDisplayOn();
            } 
            else if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                onDisplayOff();
              while (PULSE_ACTION.equals(action)) {
                onDozePulse();
              }
            }
        }
    };
}

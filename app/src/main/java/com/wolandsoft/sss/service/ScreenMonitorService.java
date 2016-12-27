/*
    Copyright 2016 Alexander Shulgin

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.wolandsoft.sss.service;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;

public class ScreenMonitorService extends Service {

    private BroadcastReceiver mReceiver;

    public static boolean isServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (ScreenMonitorService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void manageService(boolean isEnabled, Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(context, ScreenMonitorService.class));
        if (isEnabled) {
            context.startService(intent);
        } else {
            context.stopService(intent);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                onScreenStateChange(context, intent);
            }
        };
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void onScreenStateChange(@SuppressWarnings("UnusedParameters") Context context, Intent intent) {
        String strAction = intent.getAction();
        if (strAction.equals(Intent.ACTION_SCREEN_OFF)) {
            stopSelf();
        }
    }
}

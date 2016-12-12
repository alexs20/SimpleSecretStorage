package com.wolandsoft.sss.service;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.v7.preference.PreferenceManager;

import com.wolandsoft.sss.R;
import com.wolandsoft.sss.util.KeySharedPreferences;

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
    public IBinder onBind(Intent intent) {
        return null;
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

    private void onScreenStateChange(Context context, Intent intent) {
        String strAction = intent.getAction();
        if (strAction.equals(Intent.ACTION_SCREEN_OFF)) {
            stopSelf();
        }
    }
}

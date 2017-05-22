/*
    Copyright 2017 Alexander Shulgin

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
package com.wolandsoft.sss.service.external;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;

import com.wolandsoft.sss.external.EConflictResolution;
import com.wolandsoft.sss.external.ExternalException;
import com.wolandsoft.sss.external.ExternalFactory;
import com.wolandsoft.sss.external.IExternal;
import com.wolandsoft.sss.service.core.CoreService;
import com.wolandsoft.sss.util.LogEx;

import java.io.File;
import java.util.LinkedList;

/**
 * Export / Import service
 *
 * @author Alexander Shulgin
 */
public class ExternalService extends Service {
    public static final String ACTION_EXPORT = "com.wolandsoft.sss.ACTION_EXPORT";
    public static final String ACTION_IMPORT = "com.wolandsoft.sss.ACTION_IMPORT";
    public static final String KEY_ENGINE = "engine";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_CONFLICT_RESOLUTION = "conflict_resolution";
    public static final String KEY_PATH = "path";
    public static final String BROADCAST_EVENT_IMPORT_COMPLETED = "com.wolandsoft.sss.IMPORT_COMPLETED";
    public static final String BROADCAST_EVENT_EXPORT_COMPLETED = "com.wolandsoft.sss.EXPORT_COMPLETED";
    public static final String BROADCAST_EVENT_IMPORT_FAILED = "com.wolandsoft.sss.IMPORT_FAILED";
    public static final String BROADCAST_EVENT_EXPORT_FAILED = "com.wolandsoft.sss.EXPORT_FAILED";
    private Handler mHandler;
    private LinkedList<Runnable> mQueue;

    private CoreService mService;
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mService = ((CoreService.LocalBinder) binder).getService();
            runTasks();
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    void doBindService() {
        bindService(new Intent(this, CoreService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    void doUnbindService() {
        if (mService != null) {
            unbindService(mConnection);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
        mQueue = new LinkedList<>();
        doBindService();
    }

    @Override
    public void onDestroy() {
        doUnbindService();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        Runnable rn = new Runnable() {
            @Override
            public void run() {
                String engine = intent.getStringExtra(KEY_ENGINE);
                String password = intent.getStringExtra(KEY_PASSWORD);
                String path = intent.getStringExtra(KEY_PATH);
                if (ACTION_IMPORT.equals(intent.getAction())) {
                    String conflictResolution = intent.getStringExtra(KEY_CONFLICT_RESOLUTION);
                    doImport(engine, password, path, conflictResolution);
                } else if (ACTION_EXPORT.equals(intent.getAction())) {
                    doExport(engine, password, path);
                }
                stopSelf(startId);
            }
        };
        mQueue.add(rn);
        runTasks();
        return START_NOT_STICKY;
    }

    private void runTasks() {
        if (mService != null) {
            while (mQueue.size() > 0) {
                mHandler.post(mQueue.pop());
            }
        }
    }

    private void doExport(String engine, String password, String path) {
        LogEx.d("doExport(", engine, ",********,", path, ")");
        try {
            IExternal iEngine = ExternalFactory.getInstance(this).getExternal(engine);
            File destination = new File(path);
            iEngine.doExport(mService, destination.toURI(), password);
            sendBroadcast(new Intent(BROADCAST_EVENT_EXPORT_COMPLETED));
        } catch (ExternalException e) {
            sendBroadcast(new Intent(BROADCAST_EVENT_EXPORT_FAILED));
            LogEx.e(e.getMessage(), e);
        }
    }

    private void doImport(String engine, String password, String path, String conflictResolution) {
        LogEx.d("doImport(", engine, ",********,", path, ",", conflictResolution, ")");
        try {
            EConflictResolution conflictRes = EConflictResolution.valueOf(conflictResolution);
            IExternal iEngine = ExternalFactory.getInstance(this).getExternal(engine);
            File destination = new File(path);
            iEngine.doImport(mService, conflictRes, destination.toURI(), password);
            sendBroadcast(new Intent(BROADCAST_EVENT_IMPORT_COMPLETED));
        } catch (ExternalException e) {
            sendBroadcast(new Intent(BROADCAST_EVENT_IMPORT_FAILED));
            LogEx.e(e.getMessage(), e);
        }
    }
}

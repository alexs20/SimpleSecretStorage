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
package com.wolandsoft.sss.service;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.wolandsoft.sss.external.ExternalException;
import com.wolandsoft.sss.external.ExternalFactory;
import com.wolandsoft.sss.external.IExternal;
import com.wolandsoft.sss.util.LogEx;

import java.io.File;

/**
 * Export / Import service
 *
 * @author Alexander Shulgin
 */
public class ExportImportService extends Service {
    public static final String KEY_TASK = "task";
    public static final int TASK_IMPORT = 1;
    public static final int TASK_EXPORT = 0;
    public static final String KEY_ENGINE = "engine";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_CONFLICT_RESOLUTION = "conflict_resolution";
    public static final String KEY_PATH = "path";
    public static final String BROADCAST_EVENT_COMPLETED = "com.wolandsoft.sss.IMPORT_EXPORT_COMPLETED";
    public static final String KEY_STATUS = "status";

    private ServiceConnection mServiceConnection;
    private CoreService mCoreService;
    private Handler mHandler;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
        //prepare service connection
        mServiceConnection = new ServiceConnection() {

            public void onServiceDisconnected(ComponentName name) {
                mCoreService = null;
            }

            public void onServiceConnected(ComponentName name, IBinder service) {
                CoreService.LocalBinder binder = (CoreService.LocalBinder) service;
                mCoreService = binder.getService();
            }
        };
        //start connection process
        Intent intent = new Intent(this, CoreService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            final Intent finalIntent = intent;
            final int finalStartId = startId;
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (mCoreService == null) {
                        LogEx.d("Rescheduling Export / Import execution");
                        mHandler.post(this);
                    } else {
                        execute(finalIntent);
                        stopSelf(finalStartId);
                    }
                }
            };
            mHandler.post(runnable);
        } else {
            stopSelf(startId);
        }
        return START_STICKY;
    }

    private void execute(Intent intent) {
        String engine = intent.getStringExtra(KEY_ENGINE);
        int task = intent.getIntExtra(KEY_TASK, TASK_EXPORT);
        String password = intent.getStringExtra(KEY_PASSWORD);
        String path = intent.getStringExtra(KEY_PATH);
        boolean result;
        switch (task) {
            case TASK_IMPORT:
                String conflictResolution = intent.getStringExtra(KEY_CONFLICT_RESOLUTION);
                result = doImport(engine, password, path, conflictResolution);
                intent.putExtra(KEY_STATUS, result);
                break;
            case TASK_EXPORT:
            default:
                result = doExport(engine, password, path);
        }
        Intent bcIntent = new Intent();
        bcIntent.putExtra(KEY_TASK, task);
        bcIntent.setAction(BROADCAST_EVENT_COMPLETED);
        bcIntent.putExtra(KEY_STATUS, result);
        sendBroadcast(bcIntent);
        LogEx.d(BROADCAST_EVENT_COMPLETED, " sent, status: ", result);
    }

    private boolean doExport(String engine, String password, String path) {
        LogEx.d("doExport() started");
        IExternal iEngine = ExternalFactory.getInstance(this).getExternal(engine);
        File destination = new File(path);
        try {
            iEngine.doExport(mCoreService.getSQLiteStorage(), mCoreService.getKeyStoreManager(),
                    destination.toURI(), password);
        } catch (ExternalException e) {
            LogEx.e(e.getMessage(), e);
            return false;
        } finally {
            LogEx.d("doExport() finished");
        }
        return true;

    }

    private boolean doImport(String engine, String password, String path, String conflictResolution) {
        LogEx.d("doImport() started");
        IExternal.ConflictResolution conflictRes = IExternal.ConflictResolution.valueOf(conflictResolution);
        IExternal iEngine = ExternalFactory.getInstance(this).getExternal(engine);
        File destination = new File(path);
        try {
            iEngine.doImport(mCoreService.getSQLiteStorage(), mCoreService.getKeyStoreManager(), conflictRes,
                    destination.toURI(), password);
        } catch (ExternalException e) {
            LogEx.e(e.getMessage(), e);
            return false;
        } finally {
            LogEx.d("doImport() finished");
        }
        return true;
    }

    @Override
    public void onDestroy() {
        if (mCoreService != null) {
            //close live connections to the service
            unbindService(mServiceConnection);
            mCoreService = null;
        }
        super.onDestroy();
    }
}

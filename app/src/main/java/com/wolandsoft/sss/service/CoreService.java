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

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.wolandsoft.sss.storage.SQLiteStorage;
import com.wolandsoft.sss.util.KeyStoreManager;

/**
 * Core components hosting service.
 *
 * @author Alexander Shulgin
 */
public class CoreService extends Service {
    private final IBinder mBinder = new LocalBinder();
    private KeyStoreManager mKSManager;
    private SQLiteStorage mSQLtStorage;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //security keystore initialization
        mKSManager = new KeyStoreManager(getApplicationContext());
        //db initialization
        mSQLtStorage = new SQLiteStorage(getApplicationContext());
    }

    public KeyStoreManager getKeyStoreManager() {
        return mKSManager;
    }

    public SQLiteStorage getSQLiteStorage() {
        return mSQLtStorage;
    }

    /**
     * Service binder implementation
     */
    public class LocalBinder extends Binder {
        public CoreService getService() {
            return CoreService.this;
        }
    }

    public interface CoreServiceProvider{
        void addCoreServiceStateListener(CoreServiceStateListener listener);
        CoreService getCoreService();
    }

    public interface CoreServiceStateListener {
        void onCoreServiceReady(CoreService service);
    }
}

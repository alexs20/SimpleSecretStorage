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
package com.wolandsoft.sss.service.core;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.service.proxy.NotReadyException;
import com.wolandsoft.sss.storage.IStorage;
import com.wolandsoft.sss.storage.SQLiteStorage;

import java.util.List;

/**
 * Core services provider.
 *
 * @author Alexander Shulgin
 */
public class CoreService extends Service implements IStorage {
    private final IBinder mBinder = new LocalBinder();
    private SQLiteStorage mStorage;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mStorage = new SQLiteStorage(getApplicationContext());
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        mStorage.close();
        super.onDestroy();
    }

    @Override
    public List<Integer> findRecords(@Nullable String criteria) throws NotReadyException {
        return mStorage.findRecords(criteria);
    }

    @Override
    public SecretEntry getRecord(int id) throws NotReadyException {
        return mStorage.getRecord(id);
    }

    @Override
    public void deleteRecord(final int id) throws NotReadyException {
        mStorage.deleteRecord(id);
    }

    @Override
    public SecretEntry putRecord(SecretEntry entry) throws NotReadyException {
        return mStorage.putRecord(entry);
    }

    public class LocalBinder extends Binder {
        public CoreService getService() {
            return CoreService.this;
        }
    }

}

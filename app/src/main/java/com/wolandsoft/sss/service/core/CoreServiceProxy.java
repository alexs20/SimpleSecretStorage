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

import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.service.proxy.NotReadyException;
import com.wolandsoft.sss.service.proxy.ServiceProxy;
import com.wolandsoft.sss.service.proxy.ServiceProxyListener;
import com.wolandsoft.sss.storage.IStorage;
import com.wolandsoft.sss.util.ListenerList;
import com.wolandsoft.sss.util.LogEx;

import java.io.IOException;
import java.util.List;

/**
 * ServiceProxy layer for Core services provider.
 *
 * @author Alexander Shulgin
 */
public class CoreServiceProxy extends ContextWrapper implements IStorage, ServiceProxy {

    private CoreService mService;
    private ListenerList<ServiceProxyListener> mListeners;
    private Handler mHandler;
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            LogEx.d("onServiceConnected(", className, binder, ")");
            mService = ((CoreService.LocalBinder) binder).getService();
            Runnable rn = new Runnable() {
                @Override
                public void run() {
                    for (OnCoreServiceReadyListener listener : mListeners.getCompatible(OnCoreServiceReadyListener.class))
                        listener.onCoreServiceReady();
                }
            };
            mHandler.post(rn);
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    public CoreServiceProxy(Context context) {
        super(context);
        LogEx.d("CoreServiceProxy(", context, ")");
        mHandler = new Handler();
        mListeners = new ListenerList<>();
        doBindService();
    }

    public CoreServiceProxy addListener(ServiceProxyListener listener) {
        mListeners.add(listener);
        return this;
    }

    public CoreServiceProxy removeListener(ServiceProxyListener listener) {
        mListeners.remove(listener);
        return this;
    }

    void doBindService() {
        LogEx.d("doBindService()");
        bindService(new Intent(this, CoreService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    void doUnbindService() {
        LogEx.d("doUnbindService()");
        if (isServiceActive()) {
            unbindService(mConnection);
            mService = null;
        }
    }

    @Override
    public void close() throws IOException {
        LogEx.d("close()");
        doUnbindService();
    }

    @Override
    public boolean isServiceActive() {
        return mService != null;
    }

    @Override
    public List<Integer> findRecords(@Nullable String criteria) throws NotReadyException {
        if (isServiceActive())
            return mService.findRecords(criteria);
        throw new NotReadyException();
    }

    @Override
    public SecretEntry getRecord(int id) throws NotReadyException {
        if (isServiceActive())
            return mService.getRecord(id);
        throw new NotReadyException();
    }

    @Override
    public void deleteRecord(int id) throws NotReadyException {
        if (isServiceActive())
            mService.deleteRecord(id);
        else
            throw new NotReadyException();
    }

    @Override
    public SecretEntry putRecord(SecretEntry entry) throws NotReadyException {
        if (isServiceActive())
            return mService.putRecord(entry);
        throw new NotReadyException();
    }

    public interface OnCoreServiceReadyListener extends ServiceProxyListener{

        /**
         * Fired when service is available and ready.
         */
        void onCoreServiceReady();
    }
}

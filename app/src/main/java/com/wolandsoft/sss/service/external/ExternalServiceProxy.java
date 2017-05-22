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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;

import com.wolandsoft.sss.external.EConflictResolution;
import com.wolandsoft.sss.service.ServiceManager;
import com.wolandsoft.sss.service.proxy.ServiceProxy;
import com.wolandsoft.sss.service.proxy.ServiceProxyListener;
import com.wolandsoft.sss.util.ListenerList;

import java.io.IOException;
import java.net.URI;

/**
 * External service proxy.
 *
 * @author Alexander Shulgin
 */
public class ExternalServiceProxy extends ContextWrapper implements ServiceProxy {

    private ListenerList<ServiceProxyListener> mListeners;
    private Handler mHandler;
    private BroadcastReceiver mBrReceiver;

    public ExternalServiceProxy(Context base) {
        super(base);
        mHandler = new Handler();
        mListeners = new ListenerList<>();
        mBrReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ExternalService.BROADCAST_EVENT_EXPORT_COMPLETED.equals(intent.getAction())) {
                    Runnable rn = new Runnable() {
                        @Override
                        public void run() {
                            for (OnExportStatusListener listener : mListeners.getCompatible(OnExportStatusListener.class))
                                listener.onExportStatus(true);
                        }
                    };
                    mHandler.post(rn);
                } else if (ExternalService.BROADCAST_EVENT_EXPORT_FAILED.equals(intent.getAction())) {
                    Runnable rn = new Runnable() {
                        @Override
                        public void run() {
                            for (OnExportStatusListener listener : mListeners.getCompatible(OnExportStatusListener.class))
                                listener.onExportStatus(false);
                        }
                    };
                    mHandler.post(rn);
                } else if (ExternalService.BROADCAST_EVENT_IMPORT_COMPLETED.equals(intent.getAction())) {
                    Runnable rn = new Runnable() {
                        @Override
                        public void run() {
                            for (OnImportStatusListener listener : mListeners.getCompatible(OnImportStatusListener.class))
                                listener.onImportStatus(true);
                        }
                    };
                    mHandler.post(rn);
                } else if (ExternalService.BROADCAST_EVENT_IMPORT_FAILED.equals(intent.getAction())) {
                    Runnable rn = new Runnable() {
                        @Override
                        public void run() {
                            for (OnImportStatusListener listener : mListeners.getCompatible(OnImportStatusListener.class))
                                listener.onImportStatus(false);
                        }
                    };
                    mHandler.post(rn);
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ExternalService.BROADCAST_EVENT_IMPORT_COMPLETED);
        filter.addAction(ExternalService.BROADCAST_EVENT_IMPORT_FAILED);
        filter.addAction(ExternalService.BROADCAST_EVENT_EXPORT_COMPLETED);
        filter.addAction(ExternalService.BROADCAST_EVENT_EXPORT_FAILED);
        registerReceiver(mBrReceiver, filter);
    }

    @Override
    public boolean isServiceActive() {
        return ServiceManager.isServiceRunning(this, ExternalService.class);
    }

    @Override
    public void close() throws IOException {
        unregisterReceiver(mBrReceiver);
    }

    public void doExport(String engine, URI destination, String password) {
        Intent intent = new Intent(ExternalService.ACTION_EXPORT, null, this, ExternalService.class);
        intent.putExtra(ExternalService.KEY_ENGINE, engine);
        intent.putExtra(ExternalService.KEY_PASSWORD, password);
        intent.putExtra(ExternalService.KEY_PATH, destination.getPath());
        startService(intent);
    }

    public void doImport(String engine, EConflictResolution conflictRes, URI source, String password) {
        Intent intent = new Intent(ExternalService.ACTION_IMPORT, null, this, ExternalService.class);
        intent.putExtra(ExternalService.KEY_ENGINE, engine);
        intent.putExtra(ExternalService.KEY_PASSWORD, password);
        intent.putExtra(ExternalService.KEY_PATH, source.getPath());
        intent.putExtra(ExternalService.KEY_CONFLICT_RESOLUTION, conflictRes.name());
        startService(intent);
    }

    public ExternalServiceProxy addListener(ServiceProxyListener listener) {
        mListeners.add(listener);
        return this;
    }

    public ExternalServiceProxy removeListener(ServiceProxyListener listener) {
        mListeners.remove(listener);
        return this;
    }

    public interface OnExportStatusListener extends ServiceProxyListener {
        void onExportStatus(boolean status);
    }

    public interface OnImportStatusListener extends ServiceProxyListener {
        void onImportStatus(boolean status);
    }
}

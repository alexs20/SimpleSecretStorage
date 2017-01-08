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
package com.wolandsoft.sss.common;

import android.app.Application;

import com.wolandsoft.sss.storage.SQLiteStorage;
import com.wolandsoft.sss.util.KeyStoreManager;
import com.wolandsoft.sss.util.LogEx;

/**
 * Core static components.
 *
 * @author Alexander Shulgin
 */
public class TheApp extends Application {

    private static KeyStoreManager mKSManager;
    private static SQLiteStorage mSQLtStorage;

    public static KeyStoreManager getKeyStoreManager() {
        return mKSManager;
    }

    public static SQLiteStorage getSQLiteStorage() {
        return mSQLtStorage;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogEx.d("TheApp.onCreate()");
        //security keystore initialization
        mKSManager = new KeyStoreManager(getApplicationContext());
        //db initialization
        mSQLtStorage = new SQLiteStorage(getApplicationContext());
    }
}
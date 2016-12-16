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
package com.wolandsoft.sss.activity;

import android.util.LruCache;

import com.wolandsoft.sss.entity.SecretEntry;
import com.wolandsoft.sss.storage.SQLiteStorage;
import com.wolandsoft.sss.util.KeyStoreManager;

/**
 * Interface to objects shared across all main activity's children
 */

public interface ISharedObjects {

    KeyStoreManager getKeyStoreManager();
    SQLiteStorage getSQLiteStorage();
    LruCache<Integer, SecretEntry> getSecretEntriesCache();
}

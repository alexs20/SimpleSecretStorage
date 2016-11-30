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
package com.wolandsoft.sss.external;

import com.wolandsoft.sss.storage.SQLiteStorage;
import com.wolandsoft.sss.util.KeyStoreManager;

import java.net.URI;

/**
 * Interface of Export-Import engine.
 *
 * @author Alexander Shulgin
 */

public interface IExternal {

    void doExport(SQLiteStorage fromStorage, KeyStoreManager keystore, URI destination, String password, Object... extra) throws ExternalException;

    void doImport(SQLiteStorage toStorage, KeyStoreManager keystore, ConflictResolution conflictRes, URI source, String password, Object... extra) throws ExternalException;

    /**
     * External identifier.
     *
     * @return Unique identifier across all possible externals implementations.
     */
    String getID();

    enum ConflictResolution {
        overwrite, merge
    }

    interface OnExternalInteract {
        void onPermissionRequest(String permission);
    }
}

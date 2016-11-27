package com.wolandsoft.sss.external;

import com.wolandsoft.sss.storage.SQLiteStorage;
import com.wolandsoft.sss.util.KeyStoreManager;

import java.net.URI;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */

public interface IExternal {

    void doExport(SQLiteStorage fromStorage, KeyStoreManager keystore, OnExternalInteract callback, URI destination, String password, Object... extra) throws ExternalException;

    void doImport(SQLiteStorage toStorage, KeyStoreManager keystore, OnExternalInteract callback, ConflictResolution conflictRes, URI source, String password, Object... extra) throws ExternalException;

    /**
     * External identifier.
     *
     * @return Unique identifier across all possible externals implementations.
     */
    String getID();

    public enum ConflictResolution {
        overwrite, merge;
    }

    public interface OnExternalInteract{
        void onPermissionRequest(String permission);
    }
}

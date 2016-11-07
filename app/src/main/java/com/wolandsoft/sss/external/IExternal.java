package com.wolandsoft.sss.external;

import com.wolandsoft.sss.storage.SQLiteStorage;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */

public interface IExternal {

    /**
     * Initialize external storage.
     *
     * @throws ExternalException on any error.
     */
    void startup() throws ExternalException;

    /**
     * Close storage.
     */
    void shutdown();

    void doExport(SQLiteStorage fromStorage) throws ExternalException;

    void doImport(SQLiteStorage toStorage, boolean isOverwrite) throws ExternalException;

    /**
     * External identifier.
     *
     * @return Unique identifier across all possible externals implementations.
     */
    String getID();
}

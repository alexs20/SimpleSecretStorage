package com.wolandsoft.sss.external;

import com.wolandsoft.sss.storage.IStorage;
import com.wolandsoft.sss.storage.StorageException;

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

    void doExport(IStorage fromStorage) throws ExternalException;

    void doImport(IStorage toStorage, boolean isOverwrite) throws ExternalException;

    /**
     * External identifier.
     *
     * @return Unique identifier across all possible externals implementations.
     */
    String getID();
}

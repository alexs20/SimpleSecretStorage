package com.wolandsoft.sss.storage;

import android.content.Context;
import android.content.ContextWrapper;

import com.wolandsoft.sss.storage.db.SQLiteStorage;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alexander on 10/15/2016.
 */

public final class StorageFactory extends ContextWrapper {

    private static StorageFactory thisInstance = null;
    private Map<String, IStorage> storages = null;

    private StorageFactory(Context base) {
        super(base);
        storages = new HashMap<>();
        //TODO multistorage initalization logic here
        IStorage storage = new SQLiteStorage(this);
        storages.put(storage.getID(), storage);
    }

    public static StorageFactory getInstance(Context context) {
        if (thisInstance == null) {
            thisInstance = new StorageFactory(context);
        }
        return thisInstance;
    }

    public IStorage getStorage(String id) throws StorageException {
        return storages.get(id);
    }
}

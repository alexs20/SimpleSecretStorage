package com.wolandsoft.sss.external;

import android.content.Context;
import android.content.ContextWrapper;

import com.wolandsoft.sss.external.json.JsonAes256Zip;
import com.wolandsoft.sss.storage.StorageException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alexander on 10/15/2016.
 */

public final class ExternalFactory extends ContextWrapper {

    private static ExternalFactory thisInstance = null;
    private Map<String, IExternal> externals = null;

    private ExternalFactory(Context base) {
        super(base);
        externals = new HashMap<>();
        //TODO multi-externals initialization logic here
        IExternal external = new JsonAes256Zip(this);
        externals.put(external.getID(), external);
    }

    public static ExternalFactory getInstance(Context context) {
        if (thisInstance == null) {
            thisInstance = new ExternalFactory(context);
        }
        return thisInstance;
    }

    public IExternal getExternal(String id) throws ExternalException {
        return externals.get(id);
    }

    public String[] getAvailableIds() {
        return externals.keySet().toArray(new String[0]);
    }
}

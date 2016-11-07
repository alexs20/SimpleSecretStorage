package com.wolandsoft.sss.storage;

import android.content.Context;
import android.content.ContextWrapper;

/**
 * Created by Alexander on 10/22/2016.
 */

public abstract class AStorage extends ContextWrapper implements IStorage {
    public AStorage(Context base) {
        super(base);
    }
}

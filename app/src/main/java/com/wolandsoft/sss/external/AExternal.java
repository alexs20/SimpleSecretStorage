package com.wolandsoft.sss.external;

import android.content.Context;
import android.content.ContextWrapper;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */

public abstract class AExternal extends ContextWrapper implements IExternal {
    public AExternal(Context base) {
        super(base);
    }
}

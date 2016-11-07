package com.wolandsoft.sss.entity;

import com.wolandsoft.sss.R;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */

public enum PredefinedAttributes {
    NAME(false, true, R.string.enum_NAME),
    URL(false, false, R.string.enum_URL),
    PASSWORD(true, false, R.string.enum_PASSWORD);

    int keyResID;
    boolean isDefault;
    boolean isProtected;

    private PredefinedAttributes(boolean isProtected, boolean isDefault, int keyResID) {
        this.isProtected = isProtected;
        this.isDefault = isDefault;
        this.keyResID = keyResID;
    }
}

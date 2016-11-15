package com.wolandsoft.sss.entity;

import com.wolandsoft.sss.R;

/**
 * @author Alexander Shulgin /alexs20@gmail.com/
 */

public enum PredefinedAttribute {
    NAME(false, R.string.enum_NAME),
    URL(false, R.string.enum_URL),
    USER(false, R.string.enum_USER),
    PASSWORD(true, R.string.enum_PASSWORD);

    int keyResID;
    boolean isProtected;

    private PredefinedAttribute(boolean isProtected, int keyResID) {
        this.isProtected = isProtected;
        this.keyResID = keyResID;
    }

    public int getKeyResID() {
        return keyResID;
    }

    public boolean isProtected() {
        return isProtected;
    }
}

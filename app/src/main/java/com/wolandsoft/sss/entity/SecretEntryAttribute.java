package com.wolandsoft.sss.entity;

/**
 * Created by Alexander on 10/15/2016.
 */

public class SecretEntryAttribute {

    private String key;
    private String value;
    private boolean isProtected;

    public SecretEntryAttribute(String key, String value, boolean isProtected) {
        this.key = key;
        this.value = value;
        this.isProtected = isProtected;
    }

    public String getKey() {
        return key;
    }

    public boolean isProtected() {
        return isProtected;
    }

    public void setProtected(boolean isProtected) {
        this.isProtected = isProtected;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

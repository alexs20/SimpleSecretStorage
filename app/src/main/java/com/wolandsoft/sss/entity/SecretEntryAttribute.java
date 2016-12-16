/*
    Copyright 2016 Alexander Shulgin

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
 */
package com.wolandsoft.sss.entity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Attribute entity of secret entry.
 *
 * @author Alexander Shulgin
 */

public class SecretEntryAttribute implements Parcelable {

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator<SecretEntryAttribute>() {
        public SecretEntryAttribute createFromParcel(Parcel in) {
            return new SecretEntryAttribute(in);
        }

        public SecretEntryAttribute[] newArray(int size) {
            return new SecretEntryAttribute[size];
        }
    };

    private String key;
    private String value;
    private boolean isProtected;

    public SecretEntryAttribute(String key, String value, boolean isProtected) {
        this.key = key;
        this.value = value;
        this.isProtected = isProtected;
    }

    public SecretEntryAttribute(Parcel parcel) {
        this.key = parcel.readString();
        this.value = parcel.readString();
        this.isProtected = parcel.readByte() != 0;
    }

    public String getKey() {
        return key;
    }

    public boolean isProtected() {
        return this.isProtected;
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(key);
        parcel.writeString(value);
        parcel.writeByte((byte) (isProtected ? 1 : 0));
    }
}

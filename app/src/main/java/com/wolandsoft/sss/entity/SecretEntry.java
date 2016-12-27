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

import java.util.LinkedList;

/**
 * Secret Entry entity.
 *
 * @author Alexander Shulgin
 */

public class SecretEntry extends LinkedList<SecretEntryAttribute> implements Parcelable {

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator<SecretEntry>() {
        public SecretEntry createFromParcel(Parcel in) {
            return new SecretEntry(in);
        }

        public SecretEntry[] newArray(int size) {
            return new SecretEntry[size];
        }
    };

    private final int id;
    private final long created;
    private final long updated;

    public SecretEntry() {
        this(0, 0, 0);
    }

    public SecretEntry(int id, long created, long updated) {
        this.id = id;
        this.created = created;
        this.updated = updated;
    }

    @SuppressWarnings("WeakerAccess")
    public SecretEntry(Parcel parcel) {
        this.id = parcel.readInt();
        this.created = parcel.readLong();
        this.updated = parcel.readLong();
        parcel.readList(this, getClass().getClassLoader());
    }

    public int getID() {
        return id;
    }

    public long getCreated() {
        return created;
    }

    public long getUpdated() {
        return updated;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(id);
        parcel.writeLong(created);
        parcel.writeLong(updated);
        parcel.writeList(this);
    }
}

/*
    Copyright 2017 Alexander Shulgin

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
package com.wolandsoft.sss.service.pccomm;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Paired device information. Contains remote host, port and security key.
 */

public class PairedDevice implements Parcelable {
    public static final Parcelable.Creator<PairedDevice> CREATOR = new Parcelable.Creator<PairedDevice>() {
        public PairedDevice createFromParcel(Parcel in) {
            return new PairedDevice(in);
        }

        public PairedDevice[] newArray(int size) {
            return new PairedDevice[size];
        }
    };
    public byte[] mIp;
    public String mHost;
    public int mPort;
    public byte[] mKey;

    public PairedDevice() {

    }

    private PairedDevice(Parcel in) {
        mIp = new byte[in.readInt()];
        in.readByteArray(mIp);
        mHost = in.readString();
        mPort = in.readInt();
        mKey = new byte[in.readInt()];
        in.readByteArray(mKey);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mIp.length);
        dest.writeByteArray(mIp);
        dest.writeString(mHost);
        dest.writeInt(mPort);
        dest.writeInt(mKey.length);
        dest.writeByteArray(mKey);
    }

}

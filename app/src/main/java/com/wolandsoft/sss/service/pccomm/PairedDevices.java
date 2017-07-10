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

import com.google.gson.Gson;

import java.util.ArrayList;

/**
 * Collection of paired devices
 */

public class PairedDevices extends ArrayList<PairedDevice> {

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static PairedDevices fromJson(String json) {
        Gson gson = new Gson();
        if(json == null ){
            return new PairedDevices();
        }
        return gson.fromJson(json, PairedDevices.class);
    }
}

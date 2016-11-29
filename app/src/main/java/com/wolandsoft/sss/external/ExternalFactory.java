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
package com.wolandsoft.sss.external;

import android.content.Context;
import android.content.ContextWrapper;

import com.wolandsoft.sss.external.json.JsonAes256Zip;

import java.util.HashMap;
import java.util.Map;

/**
 * Factory of all possible Export-Import engines.
 *
 * @author Alexander Shulgin
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

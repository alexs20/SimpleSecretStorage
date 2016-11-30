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

import com.wolandsoft.sss.R;

/**
 * Enum of predefined fields for new secret entry.
 *
 * @author Alexander Shulgin
 */

public enum PredefinedAttribute {
    NAME(false, R.string.enum_NAME),
    URL(false, R.string.enum_URL),
    USER(false, R.string.enum_USER),
    PASSWORD(true, R.string.enum_PASSWORD);

    int keyResID;
    boolean isProtected;

    PredefinedAttribute(boolean isProtected, int keyResID) {
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

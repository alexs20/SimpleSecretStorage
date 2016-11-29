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

import java.io.Serializable;

/**
 * Attribute entity of secret entry.
 * @author Alexander Shulgin
 */

public class SecretEntryAttribute  implements Serializable {

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

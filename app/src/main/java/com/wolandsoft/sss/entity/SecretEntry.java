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
import java.util.LinkedList;

/**
 * Secret Entry entity.
 *
 * @author Alexander Shulgin
 */

public class SecretEntry extends LinkedList<SecretEntryAttribute> implements Serializable {
    private int id;
    private long created;
    private long updated;

    public SecretEntry() {
        this(0, 0, 0);
    }

    public SecretEntry(int id, long created, long updated) {
        this.id = id;
        this.created = created;
        this.updated = updated;
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
}

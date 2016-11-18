package com.wolandsoft.sss.entity;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * Created by Alexander on 10/14/2016.
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

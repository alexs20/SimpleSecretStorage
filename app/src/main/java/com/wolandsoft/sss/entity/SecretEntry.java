package com.wolandsoft.sss.entity;

import java.util.LinkedList;
import java.util.UUID;

/**
 * Created by Alexander on 10/14/2016.
 */

public class SecretEntry extends LinkedList<SecretEntryAttribute> {
    private UUID id;
    private long created;
    private long updated;

    public SecretEntry() {
        this(UUID.randomUUID());
    }

    public SecretEntry(UUID id) {
        this.id = id;
    }

    public UUID getID() {
        return id;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public long getUpdated() {
        return updated;
    }

    public void setUpdated(long updated) {
        this.updated = updated;
    }
}

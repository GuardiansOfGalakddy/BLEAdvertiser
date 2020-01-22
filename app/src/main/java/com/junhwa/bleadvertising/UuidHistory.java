package com.junhwa.bleadvertising;

import java.util.UUID;

public class UuidHistory {
    private UUID uuid;

    public UuidHistory(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
}

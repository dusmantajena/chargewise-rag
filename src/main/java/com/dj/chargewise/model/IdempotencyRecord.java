package com.dj.chargewise.model;

import java.util.UUID;

public record IdempotencyRecord(String requestHash, UUID disputeId) {

    public boolean isCompleted() {
        return disputeId != null;
    }
}

package com.readverse.framework.mq;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record DomainEvent(String eventId, String type, Long bizId, Instant occurredAt, Map<String, Object> payload) {

    public static DomainEvent of(String type, Long bizId, Map<String, Object> payload) {
        return new DomainEvent(UUID.randomUUID().toString().replace("-", ""), type, bizId, Instant.now(), payload);
    }
}

package com.anchoriq.core.domain.common.event;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 도메인 이벤트 기반 클래스.
 * 모든 도메인 이벤트는 이 클래스를 상속한다.
 * 이벤트 발생 시각과 고유 ID를 보장한다.
 */
public abstract class DomainEvent {

    private final String eventId;
    private final Instant occurredAt;

    protected DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = Instant.now();
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DomainEvent that = (DomainEvent) o;
        return eventId.equals(that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId);
    }
}

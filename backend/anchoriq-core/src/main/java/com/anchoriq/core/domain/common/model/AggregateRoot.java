package com.anchoriq.core.domain.common.model;

import com.anchoriq.core.domain.common.event.DomainEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate Root 기반 클래스.
 * 도메인 이벤트를 수집하고 발행하는 책임을 가진다.
 * 모든 Aggregate Root 엔티티는 이 클래스를 상속한다.
 */
public abstract class AggregateRoot {

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    protected void registerEvent(DomainEvent event) {
        this.domainEvents.add(event);
    }

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        this.domainEvents.clear();
    }
}

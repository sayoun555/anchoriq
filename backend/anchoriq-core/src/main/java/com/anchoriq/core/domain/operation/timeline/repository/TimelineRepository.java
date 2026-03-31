package com.anchoriq.core.domain.operation.timeline.repository;

import com.anchoriq.core.domain.operation.timeline.model.TimelineEvent;

import java.time.Instant;
import java.util.List;

/**
 * 타임라인 저장소 인터페이스 — Elasticsearch 구현.
 */
public interface TimelineRepository {

    void save(TimelineEvent event);

    List<TimelineEvent> findByTimeRange(Instant from, Instant to, int page, int size);

    List<TimelineEvent> findByRelatedEntityId(String entityId, int page, int size);
}

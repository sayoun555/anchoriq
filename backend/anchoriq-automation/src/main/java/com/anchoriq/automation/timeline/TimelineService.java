package com.anchoriq.automation.timeline;

import com.anchoriq.core.domain.operation.timeline.model.TimelineEvent;

import java.time.Instant;
import java.util.List;

/**
 * 타임라인 서비스 인터페이스 — 이벤트-판단-액션 이력 관리.
 */
public interface TimelineService {

    /**
     * 타임라인 이벤트를 Elasticsearch에 저장.
     */
    void recordEvent(TimelineEvent event);

    /**
     * 시간 범위로 타임라인 이벤트 조회.
     */
    List<TimelineEvent> getTimeline(Instant from, Instant to, int page, int size);

    /**
     * 관련 엔티티 ID로 타임라인 조회.
     */
    List<TimelineEvent> getTimelineByEntity(String entityId, int page, int size);
}

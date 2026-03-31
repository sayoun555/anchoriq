package com.anchoriq.automation.timeline;

import com.anchoriq.core.domain.operation.timeline.model.TimelineEvent;
import com.anchoriq.core.domain.operation.timeline.repository.TimelineRepository;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * 타임라인 서비스 구현체 — Elasticsearch를 통한 이벤트-판단-액션 이력 관리.
 * Tier 3 (로그) — 저장 실패 시 예외를 던지지 않고 로그만 남긴다.
 * Bean 등록은 AutomationConfig에서 수행한다.
 */
@Slf4j
public class TimelineServiceImpl implements TimelineService {

    private final TimelineRepository timelineRepository;

    public TimelineServiceImpl(TimelineRepository timelineRepository) {
        this.timelineRepository = timelineRepository;
    }

    @Override
    public void recordEvent(TimelineEvent event) {
        try {
            timelineRepository.save(event);
            log.debug("Timeline event saved: {} - {}", event.getId(), event.getEventType());
        } catch (Exception e) {
            log.error("Failed to save timeline event (non-critical): {}", e.getMessage());
        }
    }

    @Override
    public List<TimelineEvent> getTimeline(Instant from, Instant to, int page, int size) {
        try {
            return timelineRepository.findByTimeRange(from, to, page, size);
        } catch (Exception e) {
            log.error("Failed to query timeline: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<TimelineEvent> getTimelineByEntity(String entityId, int page, int size) {
        try {
            return timelineRepository.findByRelatedEntityId(entityId, page, size);
        } catch (Exception e) {
            log.error("Failed to query timeline by entity: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}

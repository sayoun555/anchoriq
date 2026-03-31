package com.anchoriq.api.application.timeline;

import com.anchoriq.api.dto.response.timeline.TimelineEventResponse;
import com.anchoriq.automation.timeline.TimelineService;
import com.anchoriq.core.domain.operation.timeline.model.TimelineEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

/**
 * 타임라인 Application Service 구현체.
 * Elasticsearch가 비활성화된 환경에서는 TimelineService가 없을 수 있다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TimelineApplicationServiceImpl implements TimelineApplicationService {

    @Nullable
    private final TimelineService timelineService;

    @Override
    public List<TimelineEventResponse> getTimeline(Instant from, Instant to, int page, int size) {
        if (timelineService == null) {
            log.warn("TimelineService unavailable (Elasticsearch disabled)");
            return Collections.emptyList();
        }

        if (from == null) {
            from = Instant.now().minus(24, ChronoUnit.HOURS);
        }
        if (to == null) {
            to = Instant.now();
        }

        List<TimelineEvent> events = timelineService.getTimeline(from, to, page, size);
        return events.stream()
                .map(TimelineEventResponse::from)
                .toList();
    }

    @Override
    public List<TimelineEventResponse> getTimelineByEntity(String entityId, int page, int size) {
        if (timelineService == null) {
            log.warn("TimelineService unavailable (Elasticsearch disabled)");
            return Collections.emptyList();
        }

        List<TimelineEvent> events = timelineService.getTimelineByEntity(entityId, page, size);
        return events.stream()
                .map(TimelineEventResponse::from)
                .toList();
    }
}

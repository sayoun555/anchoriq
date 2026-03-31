package com.anchoriq.api.application.timeline;

import com.anchoriq.api.dto.response.timeline.TimelineEventResponse;

import java.time.Instant;
import java.util.List;

/**
 * 타임라인 Application Service 인터페이스.
 */
public interface TimelineApplicationService {

    List<TimelineEventResponse> getTimeline(Instant from, Instant to, int page, int size);

    List<TimelineEventResponse> getTimelineByEntity(String entityId, int page, int size);
}

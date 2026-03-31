package com.anchoriq.api.dto.response.timeline;

import com.anchoriq.core.domain.operation.timeline.model.TimelineEvent;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 타임라인 이벤트 응답 DTO.
 */
@Getter
@Builder
public class TimelineEventResponse {

    private String id;
    private String eventType;
    private String source;
    private String description;
    private String riskLevel;
    private String relatedEntityId;
    private Instant timestamp;

    public static TimelineEventResponse from(TimelineEvent event) {
        return TimelineEventResponse.builder()
                .id(event.getId())
                .eventType(event.getEventType())
                .source(event.getSource())
                .description(event.getDescription())
                .riskLevel(event.getRiskLevel() != null ? event.getRiskLevel().name() : null)
                .relatedEntityId(event.getRelatedEntityId())
                .timestamp(event.getTimestamp())
                .build();
    }
}

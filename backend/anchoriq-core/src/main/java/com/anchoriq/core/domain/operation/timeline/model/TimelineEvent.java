package com.anchoriq.core.domain.operation.timeline.model;

import com.anchoriq.core.domain.intelligence.risk.model.RiskLevel;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * 타임라인 이벤트 — Elasticsearch에 저장되는 이벤트-판단-액션 이력.
 * JPA Entity가 아닌 ES 문서 모델.
 */
@Getter
@Builder
public class TimelineEvent {

    private String id;
    private String eventType;
    private String source;
    private String description;
    private RiskLevel riskLevel;
    private String relatedEntityId;
    private Instant timestamp;

    public boolean isHighRisk() {
        return this.riskLevel != null && this.riskLevel.isHighOrAbove();
    }

    public boolean isRelatedTo(String entityId) {
        return this.relatedEntityId != null && this.relatedEntityId.equals(entityId);
    }
}

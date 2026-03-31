package com.anchoriq.core.event;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * 리스크 알림 이벤트 — Kafka risk-alerts 토픽 메시지와 매핑.
 */
@Getter
@Builder
public class RiskAlertEvent {

    private String alertId;
    private String type;
    private String riskLevel;
    private String vesselImo;
    private String vesselName;
    private String chokepoint;
    private String reason;
    private String recommendedAction;
    private Double aiConfidence;
    private Map<String, String> relatedEntities;
    private Instant timestamp;
}

package com.anchoriq.automation.consumer;

import com.anchoriq.automation.timeline.TimelineService;
import com.anchoriq.core.domain.intelligence.risk.model.RiskLevel;
import com.anchoriq.core.domain.operation.timeline.model.TimelineEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * risk-alerts 토픽 Consumer — Elasticsearch ai-decisions 인덱스 저장.
 * Consumer Group: alert-es-logger
 * Kafka 및 TimelineService가 활성화된 환경에서만 Bean이 생성된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(TimelineService.class)
public class RiskAlertElasticsearchConsumer {

    private final TimelineService timelineService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "risk-alerts",
            groupId = "alert-es-logger",
            containerFactory = "riskAlertListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.debug("Saving risk alert to timeline: offset={}", record.offset());

        try {
            Map<String, Object> map = objectMapper.readValue(record.value(),
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));

            TimelineEvent event = TimelineEvent.builder()
                    .id(getString(map, "alertId"))
                    .eventType(getString(map, "type"))
                    .source("AI_ENGINE")
                    .description(getString(map, "reason"))
                    .riskLevel(parseRiskLevel(getString(map, "riskLevel")))
                    .relatedEntityId(getString(map, "vesselImo"))
                    .timestamp(Instant.parse(getString(map, "timestamp")))
                    .build();

            timelineService.recordEvent(event);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to save risk alert to ES: {}", e.getMessage(), e);
            throw new RuntimeException("ES logging failed", e);
        }
    }

    private RiskLevel parseRiskLevel(String level) {
        try {
            return RiskLevel.valueOf(level.toUpperCase());
        } catch (Exception e) {
            return RiskLevel.MEDIUM;
        }
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }
}

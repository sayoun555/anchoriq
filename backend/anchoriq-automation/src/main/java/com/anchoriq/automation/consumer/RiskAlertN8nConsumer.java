package com.anchoriq.automation.consumer;

import com.anchoriq.automation.n8n.N8nClient;
import com.anchoriq.automation.notification.NotificationDispatcher;
import com.anchoriq.core.domain.operation.workflow.model.Workflow;
import com.anchoriq.core.domain.operation.workflow.model.WorkflowExecution;
import com.anchoriq.core.domain.operation.workflow.repository.WorkflowExecutionRepository;
import com.anchoriq.core.domain.operation.workflow.repository.WorkflowRepository;
import com.anchoriq.core.domain.operation.workflow.service.WorkflowDomainService;
import com.anchoriq.core.event.RiskAlertEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * risk-alerts 토픽 Consumer — 워크플로우 매칭 후 n8n 트리거 + 알림 발송.
 * Consumer Group: alert-n8n-trigger
 * 수동 커밋, 3번 재시도 후 DLT.
 * Kafka가 비활성화된 환경에서는 Bean이 생성되지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class RiskAlertN8nConsumer {

    private final N8nClient n8nClient;
    private final NotificationDispatcher notificationDispatcher;
    private final WorkflowRepository workflowRepository;
    private final WorkflowExecutionRepository executionRepository;
    private final WorkflowDomainService workflowDomainService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "risk-alerts",
            groupId = "alert-n8n-trigger",
            containerFactory = "riskAlertListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.info("Received risk alert: key={}, offset={}", record.key(), record.offset());

        try {
            RiskAlertEvent event = parseEvent(record.value());

            // 1. n8n 웹훅 트리거
            n8nClient.triggerRiskAlert(event);

            // 2. 워크플로우 매칭 및 실행
            processMatchingWorkflows(event);

            // 3. 알림 규칙 매칭 및 발송
            notificationDispatcher.dispatch(event);

            ack.acknowledge();
            log.info("Risk alert processed successfully: {}", event.getAlertId());
        } catch (Exception e) {
            log.error("Failed to process risk alert: {}", e.getMessage(), e);
            throw new RuntimeException("Risk alert processing failed", e);
        }
    }

    private RiskAlertEvent parseEvent(String json) {
        try {
            Map<String, Object> map = objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));

            return RiskAlertEvent.builder()
                    .alertId(getString(map, "alertId"))
                    .type(getString(map, "type"))
                    .riskLevel(getString(map, "riskLevel"))
                    .vesselImo(getString(map, "vesselImo"))
                    .vesselName(getString(map, "vesselName"))
                    .chokepoint(getString(map, "chokepoint"))
                    .reason(getString(map, "reason"))
                    .recommendedAction(getString(map, "recommendedAction"))
                    .aiConfidence(getDouble(map, "aiConfidence"))
                    .timestamp(Instant.parse(getString(map, "timestamp")))
                    .build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse risk alert event", e);
        }
    }

    private void processMatchingWorkflows(RiskAlertEvent event) {
        List<Workflow> activeWorkflows = workflowRepository.findActiveWorkflows();
        List<Workflow> matching = workflowDomainService.findMatchingWorkflows(
                activeWorkflows, event.getType(), event.getRiskLevel());

        for (Workflow workflow : matching) {
            try {
                if (workflow.getN8nWorkflowId() != null) {
                    n8nClient.triggerWebhook(
                            "/webhook/" + workflow.getN8nWorkflowId(),
                            buildWorkflowPayload(event));
                }
                executionRepository.save(
                        WorkflowExecution.recordSuccess(workflow.getId(), event.getAlertId()));
                log.info("Workflow {} executed for alert {}", workflow.getId(), event.getAlertId());
            } catch (Exception e) {
                executionRepository.save(
                        WorkflowExecution.recordFailure(workflow.getId(), event.getAlertId()));
                log.error("Workflow {} execution failed for alert {}: {}",
                        workflow.getId(), event.getAlertId(), e.getMessage());
            }
        }
    }

    private Map<String, Object> buildWorkflowPayload(RiskAlertEvent event) {
        return Map.of(
                "alertId", event.getAlertId(),
                "type", event.getType(),
                "riskLevel", event.getRiskLevel(),
                "vesselImo", event.getVesselImo() != null ? event.getVesselImo() : "",
                "reason", event.getReason() != null ? event.getReason() : "",
                "timestamp", event.getTimestamp().toString()
        );
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }

    private Double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }
}

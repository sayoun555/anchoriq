package com.anchoriq.automation.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * risk-alerts 토픽 Consumer — WebSocket으로 프론트엔드에 실시간 푸시.
 * Consumer Group: alert-ws-pusher
 * Kafka가 비활성화된 환경에서는 Bean이 생성되지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class RiskAlertWebSocketConsumer {

    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(
            topics = "risk-alerts",
            groupId = "alert-ws-pusher",
            containerFactory = "riskAlertListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        log.debug("Pushing risk alert to WebSocket: offset={}", record.offset());

        try {
            messagingTemplate.convertAndSend("/topic/alerts", record.value());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("Failed to push risk alert to WebSocket: {}", e.getMessage());
            throw new RuntimeException("WebSocket push failed", e);
        }
    }
}

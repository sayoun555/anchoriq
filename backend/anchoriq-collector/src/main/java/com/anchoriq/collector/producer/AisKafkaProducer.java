package com.anchoriq.collector.producer;

import com.anchoriq.collector.config.KafkaTopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

/**
 * AIS 위치 데이터 Kafka Producer.
 * 파티션 키 = mmsi (같은 선박의 위치 데이터 순서 보장).
 * Bean 등록은 CollectorConfig에서 수행한다.
 */
public class AisKafkaProducer implements KafkaMessageProducer {

    private static final Logger log = LoggerFactory.getLogger(AisKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public AisKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void send(String mmsi, Map<String, Object> message) {
        kafkaTemplate.send(topicName(), mmsi, message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send AIS message for MMSI {}: {}", mmsi, ex.getMessage());
                    }
                });
    }

    @Override
    public String topicName() {
        return KafkaTopicConfig.AIS_POSITIONS;
    }
}

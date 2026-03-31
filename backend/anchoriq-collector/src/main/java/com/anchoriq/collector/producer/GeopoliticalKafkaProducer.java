package com.anchoriq.collector.producer;

import com.anchoriq.collector.config.KafkaTopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

/**
 * 지정학 이벤트 Kafka Producer.
 * Bean 등록은 CollectorConfig에서 수행한다.
 */
public class GeopoliticalKafkaProducer implements KafkaMessageProducer {

    private static final Logger log = LoggerFactory.getLogger(GeopoliticalKafkaProducer.class);
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public GeopoliticalKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void send(String key, Map<String, Object> message) {
        kafkaTemplate.send(topicName(), message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send geopolitical event: {}", ex.getMessage());
                    }
                });
    }

    @Override
    public String topicName() {
        return KafkaTopicConfig.GEOPOLITICAL_EVENTS;
    }
}

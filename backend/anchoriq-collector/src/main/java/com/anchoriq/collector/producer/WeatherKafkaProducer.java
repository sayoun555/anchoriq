package com.anchoriq.collector.producer;

import com.anchoriq.collector.config.KafkaTopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

/**
 * 기상 데이터 Kafka Producer.
 * Bean 등록은 CollectorConfig에서 수행한다.
 */
public class WeatherKafkaProducer implements KafkaMessageProducer {

    private static final Logger log = LoggerFactory.getLogger(WeatherKafkaProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public WeatherKafkaProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void send(String key, Map<String, Object> message) {
        kafkaTemplate.send(topicName(), message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send weather event: {}", ex.getMessage());
                    }
                });
    }

    @Override
    public String topicName() {
        return KafkaTopicConfig.WEATHER_EVENTS;
    }
}

package com.anchoriq.collector.consumer.weather;

import com.anchoriq.collector.config.KafkaTopicConfig;
import com.anchoriq.core.domain.maritime.weather.model.WeatherCondition;
import com.anchoriq.core.domain.maritime.weather.model.WeatherType;
import com.anchoriq.core.domain.maritime.weather.repository.WeatherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 기상 이벤트 Consumer (Neo4j 저장).
 * weather-events 토픽에서 기상 데이터를 수신하여 Neo4j WeatherCondition 노드를 업데이트한다.
 * Kafka가 비활성화된 환경에서는 Bean이 생성되지 않는다.
 */
@Component
@ConditionalOnBean(ConcurrentKafkaListenerContainerFactory.class)
public class WeatherNeo4jConsumer {

    private static final Logger log = LoggerFactory.getLogger(WeatherNeo4jConsumer.class);

    private final WeatherRepository weatherRepository;

    public WeatherNeo4jConsumer(WeatherRepository weatherRepository) {
        this.weatherRepository = weatherRepository;
    }

    @KafkaListener(
            topics = KafkaTopicConfig.WEATHER_EVENTS,
            groupId = "weather-neo4j-updater",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(Map<String, Object> message, Acknowledgment acknowledgment) {
        try {
            WeatherType type = parseWeatherType(String.valueOf(message.get("type")));
            String severity = String.valueOf(message.get("severity"));
            double lat = toDouble(message.get("lat"));
            double lon = toDouble(message.get("lon"));
            String zone = String.valueOf(message.get("zone"));

            WeatherCondition condition = WeatherCondition.create(type, severity, lat, lon, zone);
            weatherRepository.save(condition);

            acknowledgment.acknowledge();
            log.debug("Weather condition saved: {} - {}", zone, type);
        } catch (Exception e) {
            log.error("Failed to save weather condition: {}", e.getMessage());
            throw e;
        }
    }

    private WeatherType parseWeatherType(String type) {
        try {
            return WeatherType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return WeatherType.CLEAR;
        }
    }

    private double toDouble(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        return 0.0;
    }
}

package com.anchoriq.collector.consumer.ais;

import com.anchoriq.collector.config.KafkaTopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/**
 * AIS 위치 데이터 Consumer (Redis GEO 저장).
 * ais-positions 토픽에서 선박 위치를 수신하여 Redis GEO에 저장한다.
 * Kafka가 비활성화된 환경에서는 Bean이 생성되지 않는다.
 */
@Component
@ConditionalOnBean(ConcurrentKafkaListenerContainerFactory.class)
public class AisRedisConsumer {

    private static final Logger log = LoggerFactory.getLogger(AisRedisConsumer.class);
    private static final String GEO_KEY = "vessels:positions";
    private static final String VESSEL_STATUS_KEY_PREFIX = "vessels:status:";
    private static final Duration GEO_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;

    public AisRedisConsumer(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @KafkaListener(
            topics = KafkaTopicConfig.AIS_POSITIONS,
            groupId = "ais-redis-writer",
            containerFactory = "aisKafkaListenerContainerFactory"
    )
    public void consume(Map<String, Object> message, Acknowledgment acknowledgment) {
        try {
            String mmsi = String.valueOf(message.get("mmsi"));
            double lon = toDouble(message.get("lon"));
            double lat = toDouble(message.get("lat"));
            if (mmsi == null || mmsi.isBlank()) {
                log.warn("AIS message missing MMSI, skipping");
                acknowledgment.acknowledge();
                return;
            }

            Long added = redisTemplate.opsForGeo().add(GEO_KEY,
                    new org.springframework.data.geo.Point(lon, lat), mmsi);
            log.debug("AIS Redis GEO add: mmsi={}, result={}", mmsi, added);

            // TTL 설정은 키 레벨로만 가능하므로, 개별 멤버는 TTL 없이 관리
            // 키 자체에 TTL을 설정하면 모든 위치 데이터가 함께 만료됨
            // 대신 별도 키로 MMSI별 타임스탬프를 관리하여 오래된 위치를 정리
            String timestampKey = "vessels:timestamp:" + mmsi;
            redisTemplate.opsForValue().set(timestampKey,
                    String.valueOf(message.get("timestamp")), GEO_TTL);

            // 선박 상태 저장 (항만 혼잡도 계산 시 ANCHORED/MOORED 필터링에 사용)
            String status = String.valueOf(message.getOrDefault("status", "UNKNOWN"));
            String statusKey = VESSEL_STATUS_KEY_PREFIX + mmsi;
            redisTemplate.opsForValue().set(statusKey, status, GEO_TTL);

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process AIS position: {}", e.getMessage());
            throw e;
        }
    }

    private double toDouble(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) return Double.parseDouble((String) value);
        return 0.0;
    }
}

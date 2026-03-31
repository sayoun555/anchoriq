package com.anchoriq.collector.consumer.sanction;

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
 * 제재 업데이트 Consumer (Redis 캐시 갱신).
 * sanction-updates 토픽에서 제재 정보를 수신하여 Redis 제재 선박 목록 캐시를 갱신한다.
 * Kafka가 비활성화된 환경에서는 Bean이 생성되지 않는다.
 */
@Component
@ConditionalOnBean(ConcurrentKafkaListenerContainerFactory.class)
public class SanctionRedisConsumer {

    private static final Logger log = LoggerFactory.getLogger(SanctionRedisConsumer.class);
    private static final String SANCTIONED_VESSELS_KEY = "sanctioned:vessels";
    private static final Duration TTL = Duration.ofHours(1);

    private final StringRedisTemplate redisTemplate;

    public SanctionRedisConsumer(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @KafkaListener(
            topics = KafkaTopicConfig.SANCTION_UPDATES,
            groupId = "sanction-redis-cache",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(Map<String, Object> message, Acknowledgment acknowledgment) {
        try {
            String action = String.valueOf(message.get("action"));
            String targetType = String.valueOf(message.get("targetType"));
            String targetImo = String.valueOf(message.getOrDefault("targetImo", ""));

            if ("VESSEL".equalsIgnoreCase(targetType) && !targetImo.isBlank()) {
                if ("REMOVED".equalsIgnoreCase(action)) {
                    redisTemplate.opsForSet().remove(SANCTIONED_VESSELS_KEY, targetImo);
                } else {
                    redisTemplate.opsForSet().add(SANCTIONED_VESSELS_KEY, targetImo);
                    redisTemplate.expire(SANCTIONED_VESSELS_KEY, TTL);
                }
            }

            acknowledgment.acknowledge();
            log.debug("Sanction cache updated: {} - {} (IMO: {})", action, targetType, targetImo);
        } catch (Exception e) {
            log.error("Failed to update sanction cache: {}", e.getMessage());
            // 캐시 실패는 Tier 3 - 로그만 남기고 무시
            acknowledgment.acknowledge();
        }
    }
}

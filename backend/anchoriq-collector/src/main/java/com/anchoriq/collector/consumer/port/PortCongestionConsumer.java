package com.anchoriq.collector.consumer.port;

import com.anchoriq.collector.config.KafkaTopicConfig;
import com.anchoriq.core.domain.maritime.port.repository.PortRepository;
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
 * 항만 혼잡도 Consumer (Neo4j + Redis 갱신).
 * port-congestion 토픽에서 혼잡도를 수신하여
 * Neo4j Port 노드의 혼잡도를 업데이트하고 Redis에 캐싱한다.
 * Kafka가 비활성화된 환경에서는 Bean이 생성되지 않는다.
 */
@Component
@ConditionalOnBean(ConcurrentKafkaListenerContainerFactory.class)
public class PortCongestionConsumer {

    private static final Logger log = LoggerFactory.getLogger(PortCongestionConsumer.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    private final PortRepository portRepository;
    private final StringRedisTemplate redisTemplate;

    public PortCongestionConsumer(PortRepository portRepository,
                                  StringRedisTemplate redisTemplate) {
        this.portRepository = portRepository;
        this.redisTemplate = redisTemplate;
    }

    @KafkaListener(
            topics = KafkaTopicConfig.PORT_CONGESTION,
            groupId = "port-congestion-updater",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(Map<String, Object> message, Acknowledgment acknowledgment) {
        try {
            String locode = String.valueOf(message.get("locode"));
            double congestionIndex = toDouble(message.getOrDefault("congestionIndex",
                    message.get("congestionLevel")));

            // Neo4j 업데이트
            portRepository.findByLocode(locode).ifPresent(port -> {
                port.updateCongestion(congestionIndex);
                portRepository.save(port);
            });

            // Redis 캐시 업데이트 (Tier 3 - 실패 무시)
            cachePortCongestion(locode, message, congestionIndex);

            acknowledgment.acknowledge();
            log.debug("Port congestion updated: {} = {}% [severity={}]",
                    locode, congestionIndex, message.getOrDefault("severity", "N/A"));
        } catch (Exception e) {
            log.error("Failed to process port congestion: {}", e.getMessage());
            throw e;
        }
    }

    private void cachePortCongestion(String locode, Map<String, Object> message,
                                      double congestionIndex) {
        try {
            redisTemplate.opsForValue().set(
                    "port:congestion:" + locode,
                    String.valueOf(congestionIndex),
                    CACHE_TTL);

            // 상세 정보도 캐싱 (anchoredVessels, mooredVessels, severity 등)
            String detailKey = "port:congestion:detail:" + locode;
            redisTemplate.opsForHash().putAll(detailKey, Map.of(
                    "congestionIndex", String.valueOf(congestionIndex),
                    "anchoredVessels", String.valueOf(toDouble(message.get("anchoredVessels"))),
                    "mooredVessels", String.valueOf(toDouble(message.get("mooredVessels"))),
                    "baselineRatio", String.valueOf(toDouble(message.get("baselineRatio"))),
                    "severity", String.valueOf(message.getOrDefault("severity", "LOW")),
                    "source", String.valueOf(message.getOrDefault("source", "UNKNOWN")),
                    "timestamp", String.valueOf(message.getOrDefault("timestamp", ""))
            ));
            redisTemplate.expire(detailKey, CACHE_TTL);
        } catch (Exception e) {
            log.warn("Failed to cache port congestion for {}: {}", locode, e.getMessage());
        }
    }

    private double toDouble(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }
}

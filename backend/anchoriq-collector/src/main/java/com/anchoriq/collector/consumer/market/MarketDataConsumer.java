package com.anchoriq.collector.consumer.market;

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
 * 시장 데이터 Consumer (Redis 캐시 업데이트).
 * market-data 토픽에서 유가/환율 데이터를 수신하여 Redis에 캐싱한다.
 * Kafka가 비활성화된 환경에서는 Bean이 생성되지 않는다.
 */
@Component
@ConditionalOnBean(ConcurrentKafkaListenerContainerFactory.class)
public class MarketDataConsumer {

    private static final Logger log = LoggerFactory.getLogger(MarketDataConsumer.class);
    private static final Duration CACHE_TTL = Duration.ofHours(6);

    private final StringRedisTemplate redisTemplate;

    public MarketDataConsumer(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @KafkaListener(
            topics = KafkaTopicConfig.MARKET_DATA,
            groupId = "market-data-processor",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @SuppressWarnings("unchecked")
    public void consume(Map<String, Object> message, Acknowledgment acknowledgment) {
        try {
            String dataType = String.valueOf(message.get("dataType"));

            if ("OIL_PRICE".equals(dataType)) {
                Map<String, Object> indicators = (Map<String, Object>) message.get("indicators");
                if (indicators != null) {
                    indicators.forEach((key, value) ->
                            redisTemplate.opsForValue().set(
                                    "market:oil:" + key, String.valueOf(value), CACHE_TTL));
                }
            } else if ("EXCHANGE_RATE".equals(dataType)) {
                Map<String, Object> rates = (Map<String, Object>) message.get("rates");
                if (rates != null) {
                    rates.forEach((currency, rate) ->
                            redisTemplate.opsForValue().set(
                                    "market:exchange:" + currency, String.valueOf(rate), CACHE_TTL));
                }
            }

            acknowledgment.acknowledge();
            log.debug("Market data cached: {}", dataType);
        } catch (Exception e) {
            log.error("Failed to process market data: {}", e.getMessage());
            // Tier 3 - 캐시 실패 무시
            acknowledgment.acknowledge();
        }
    }
}

package com.anchoriq.collector.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Redis GEO 유령 선박 정리 스케줄러.
 * AIS 수신이 끊긴 선박(timestamp 키 만료)을 GEO에서 제거한다.
 *
 * vessels:timestamp:{mmsi} 키는 TTL 30초.
 * 이 키가 만료되면 해당 MMSI는 더 이상 AIS 데이터를 수신하지 않는 것이므로
 * vessels:positions GEO에서 제거하여 지도에 유령 선박이 표시되지 않도록 한다.
 */
@Component
public class RedisGeoCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(RedisGeoCleanupScheduler.class);
    private static final String GEO_KEY = "vessels:positions";
    private static final String TIMESTAMP_PREFIX = "vessels:timestamp:";

    private final StringRedisTemplate redisTemplate;

    public RedisGeoCleanupScheduler(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 매 1분마다 실행.
     * GEO에 등록된 모든 MMSI를 확인하고,
     * timestamp 키가 만료된(존재하지 않는) MMSI를 GEO에서 제거한다.
     */
    @Scheduled(fixedDelay = 360_000)
    public void cleanupStalePositions() {
        try {
            Set<String> members = redisTemplate.opsForZSet().range(GEO_KEY, 0, -1);
            if (members == null || members.isEmpty()) return;

            int removed = 0;
            for (String mmsi : members) {
                Boolean exists = redisTemplate.hasKey(TIMESTAMP_PREFIX + mmsi);
                if (Boolean.FALSE.equals(exists)) {
                    redisTemplate.opsForGeo().remove(GEO_KEY, mmsi);
                    redisTemplate.delete("vessels:heading:" + mmsi);
                    redisTemplate.delete("vessels:speed:" + mmsi);
                    redisTemplate.delete("vessels:status:" + mmsi);
                    removed++;
                }
            }

            if (removed > 0) {
                log.info("GEO cleanup: removed {} stale vessel positions", removed);
            }
        } catch (Exception e) {
            log.warn("GEO cleanup failed: {}", e.getMessage());
        }
    }
}

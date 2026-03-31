package com.anchoriq.api.infrastructure.analytics;

import com.anchoriq.core.domain.analytics.model.Distribution;
import com.anchoriq.core.domain.analytics.model.TrendPoint;
import com.anchoriq.core.domain.analytics.service.GeopoliticalAnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 지정학 리스크 분석 서비스 구현체.
 * Redis SortedSet과 시계열 데이터를 활용한다.
 */
public class GeopoliticalAnalyticsServiceImpl implements GeopoliticalAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(GeopoliticalAnalyticsServiceImpl.class);

    private static final String REGION_EVENTS_KEY = "analytics:geopolitical:events-by-region";
    private static final String SEVERITY_PREFIX = "analytics:geopolitical:severity:daily:";
    private static final String HOTSPOTS_KEY = "analytics:geopolitical:hotspots";
    private static final Duration DAILY_TTL = Duration.ofDays(90);
    private static final Duration HOTSPOT_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;

    public GeopoliticalAnalyticsServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<Distribution> getEventsByRegion() {
        try {
            Set<ZSetOperations.TypedTuple<String>> tuples =
                    redisTemplate.opsForZSet().reverseRangeWithScores(REGION_EVENTS_KEY, 0, -1);
            if (tuples == null || tuples.isEmpty()) {
                return List.of();
            }

            long totalEvents = tuples.stream()
                    .mapToLong(t -> t.getScore() != null ? t.getScore().longValue() : 0L)
                    .sum();

            return tuples.stream()
                    .map(t -> Distribution.ofTotal(
                            t.getValue(),
                            t.getScore() != null ? t.getScore().longValue() : 0L,
                            totalEvents))
                    .toList();
        } catch (Exception e) {
            log.debug("Failed to get geopolitical events by region: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<TrendPoint> getSeverityTrend(int days) {
        List<TrendPoint> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String key = SEVERITY_PREFIX + date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            try {
                String value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    trend.add(TrendPoint.of(date, Double.parseDouble(value)));
                }
            } catch (Exception e) {
                log.debug("Failed to get severity trend: {}", e.getMessage());
            }
        }

        return trend;
    }

    @Override
    public List<Distribution> getHotspots() {
        try {
            Set<ZSetOperations.TypedTuple<String>> tuples =
                    redisTemplate.opsForZSet().reverseRangeWithScores(HOTSPOTS_KEY, 0, 9);
            if (tuples == null || tuples.isEmpty()) {
                return List.of();
            }

            long totalSeverity = tuples.stream()
                    .mapToLong(t -> t.getScore() != null ? t.getScore().longValue() : 0L)
                    .sum();

            return tuples.stream()
                    .map(t -> Distribution.ofTotal(
                            t.getValue(),
                            t.getScore() != null ? t.getScore().longValue() : 0L,
                            totalSeverity))
                    .toList();
        } catch (Exception e) {
            log.debug("Failed to get geopolitical hotspots: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public void recordEvent(String region, double severity) {
        try {
            redisTemplate.opsForZSet().incrementScore(REGION_EVENTS_KEY, region, 1);

            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            String severityKey = SEVERITY_PREFIX + today;

            String current = redisTemplate.opsForValue().get(severityKey);
            double currentSeverity = current != null ? Double.parseDouble(current) : 0.0;
            double newSeverity = Math.max(currentSeverity, severity);
            redisTemplate.opsForValue().set(severityKey, String.valueOf(newSeverity), DAILY_TTL);

            redisTemplate.opsForZSet().incrementScore(HOTSPOTS_KEY, region, severity);
            setTtlIfAbsent(HOTSPOTS_KEY, HOTSPOT_TTL);
        } catch (Exception e) {
            log.warn("Failed to record geopolitical event: {}", e.getMessage());
        }
    }

    private void setTtlIfAbsent(String key, Duration ttl) {
        Long expireSec = redisTemplate.getExpire(key, java.util.concurrent.TimeUnit.SECONDS);
        if (expireSec == null || expireSec < 0) {
            redisTemplate.expire(key, ttl);
        }
    }
}

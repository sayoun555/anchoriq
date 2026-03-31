package com.anchoriq.api.infrastructure.analytics;

import com.anchoriq.core.domain.analytics.model.Distribution;
import com.anchoriq.core.domain.analytics.model.TrendPoint;
import com.anchoriq.core.domain.analytics.service.AiAnalyticsService;
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
 * AI 사용 통계 서비스 구현체.
 * Redis 카운터와 SortedSet을 활용하여 AI 질의 통계를 관리한다.
 */
public class AiAnalyticsServiceImpl implements AiAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalyticsServiceImpl.class);

    private static final String TOTAL_KEY = "analytics:ai:queries:total";
    private static final String DAILY_PREFIX = "analytics:ai:queries:daily:";
    private static final String AVG_RESPONSE_KEY = "analytics:ai:response-time:avg";
    private static final String RESPONSE_COUNT_KEY = "analytics:ai:response-time:count";
    private static final String POPULAR_KEY = "analytics:ai:popular-queries";
    private static final Duration DAILY_TTL = Duration.ofDays(90);
    private static final Duration POPULAR_TTL = Duration.ofDays(30);

    private final StringRedisTemplate redisTemplate;

    public AiAnalyticsServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public long getTotalQueries() {
        return getLongValue(TOTAL_KEY);
    }

    @Override
    public long getTodayQueries() {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        return getLongValue(DAILY_PREFIX + today);
    }

    @Override
    public double getAverageResponseTimeMs() {
        try {
            String avg = redisTemplate.opsForValue().get(AVG_RESPONSE_KEY);
            return avg != null ? Double.parseDouble(avg) : 0.0;
        } catch (Exception e) {
            log.debug("Failed to get avg response time: {}", e.getMessage());
            return 0.0;
        }
    }

    @Override
    public List<Distribution> getPopularQueries(int limit) {
        try {
            Set<ZSetOperations.TypedTuple<String>> tuples =
                    redisTemplate.opsForZSet().reverseRangeWithScores(POPULAR_KEY, 0, limit - 1L);
            if (tuples == null || tuples.isEmpty()) {
                return List.of();
            }

            long totalQueries = getTotalQueries();
            return tuples.stream()
                    .map(t -> Distribution.ofTotal(
                            t.getValue(),
                            t.getScore() != null ? t.getScore().longValue() : 0L,
                            totalQueries))
                    .toList();
        } catch (Exception e) {
            log.debug("Failed to get popular queries: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<TrendPoint> getUsageTrend(int days) {
        List<TrendPoint> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String key = DAILY_PREFIX + date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            long count = getLongValue(key);
            trend.add(TrendPoint.of(date, count));
        }

        return trend;
    }

    @Override
    public void recordQuery(String query, long responseTimeMs) {
        try {
            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            String dailyKey = DAILY_PREFIX + today;

            redisTemplate.opsForValue().increment(TOTAL_KEY);
            redisTemplate.opsForValue().increment(dailyKey);
            setTtlIfAbsent(dailyKey, DAILY_TTL);

            updateAverageResponseTime(responseTimeMs);

            if (query != null && !query.isBlank()) {
                String normalizedQuery = normalizeQuery(query);
                redisTemplate.opsForZSet().incrementScore(POPULAR_KEY, normalizedQuery, 1);
                setTtlIfAbsent(POPULAR_KEY, POPULAR_TTL);
            }
        } catch (Exception e) {
            log.warn("Failed to record AI query: {}", e.getMessage());
        }
    }

    private void updateAverageResponseTime(long responseTimeMs) {
        Long count = redisTemplate.opsForValue().increment(RESPONSE_COUNT_KEY);
        if (count == null || count <= 1) {
            redisTemplate.opsForValue().set(AVG_RESPONSE_KEY, String.valueOf(responseTimeMs));
        } else {
            String currentAvgStr = redisTemplate.opsForValue().get(AVG_RESPONSE_KEY);
            double currentAvg = currentAvgStr != null ? Double.parseDouble(currentAvgStr) : 0.0;
            double newAvg = currentAvg + (responseTimeMs - currentAvg) / count;
            redisTemplate.opsForValue().set(AVG_RESPONSE_KEY,
                    String.valueOf(Math.round(newAvg * 100.0) / 100.0));
        }
    }

    private String normalizeQuery(String query) {
        String trimmed = query.trim().toLowerCase();
        return trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed;
    }

    private void setTtlIfAbsent(String key, Duration ttl) {
        Long expireSec = redisTemplate.getExpire(key, java.util.concurrent.TimeUnit.SECONDS);
        if (expireSec == null || expireSec < 0) {
            redisTemplate.expire(key, ttl);
        }
    }

    private long getLongValue(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            return value != null ? Long.parseLong(value) : 0L;
        } catch (Exception e) {
            log.debug("Failed to get Redis value for {}: {}", key, e.getMessage());
            return 0L;
        }
    }
}

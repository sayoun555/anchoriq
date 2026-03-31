package com.anchoriq.api.infrastructure.analytics;

import com.anchoriq.core.domain.analytics.model.CollectorStatistics;
import com.anchoriq.core.domain.analytics.service.CollectorStatisticsService;
import com.anchoriq.core.domain.operation.collector.model.CollectorName;
import com.anchoriq.core.domain.operation.collector.model.CollectorStatus;
import com.anchoriq.core.domain.operation.collector.service.CollectorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * 수집기 통계 서비스 구현체.
 * Redis 카운터를 활용하여 수집기별 처리량과 에러를 추적한다.
 * CollectorRegistry에서 마지막 실행 정보를 가져온다.
 */
public class CollectorStatisticsServiceImpl implements CollectorStatisticsService {

    private static final Logger log = LoggerFactory.getLogger(CollectorStatisticsServiceImpl.class);

    private static final String KEY_PREFIX = "analytics:collector:";
    private static final String PROCESSED_SUFFIX = ":processed:";
    private static final String ERROR_SUFFIX = ":errors:";
    private static final String TOTAL_SUFFIX = ":total";
    private static final String LAST_SUCCESS_SUFFIX = ":last-success";
    private static final String LAST_ERROR_SUFFIX = ":last-error";
    private static final Duration DAILY_TTL = Duration.ofDays(90);

    private final StringRedisTemplate redisTemplate;
    private final CollectorRegistry collectorRegistry;

    public CollectorStatisticsServiceImpl(StringRedisTemplate redisTemplate,
                                           CollectorRegistry collectorRegistry) {
        this.redisTemplate = redisTemplate;
        this.collectorRegistry = collectorRegistry;
    }

    @Override
    public List<CollectorStatistics> getAllStatistics() {
        return Arrays.stream(CollectorName.values())
                .map(this::getStatistics)
                .toList();
    }

    @Override
    public CollectorStatistics getStatistics(CollectorName name) {
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        long todayProcessed = getLongValue(dailyKey(name, PROCESSED_SUFFIX, today));
        long todayErrors = getLongValue(dailyKey(name, ERROR_SUFFIX, today));
        long totalProcessed = getLongValue(KEY_PREFIX + name.value() + TOTAL_SUFFIX);

        Instant lastSuccessAt = getInstantValue(KEY_PREFIX + name.value() + LAST_SUCCESS_SUFFIX);
        Instant lastErrorAt = getInstantValue(KEY_PREFIX + name.value() + LAST_ERROR_SUFFIX);

        // CollectorRegistry에서 마지막 실행 정보 보강
        try {
            CollectorStatus status = collectorRegistry.getStatus(name);
            if (lastSuccessAt == null && status.lastRunAt() != null && !status.hasError()) {
                lastSuccessAt = status.lastRunAt();
            }
            if (lastErrorAt == null && status.lastRunAt() != null && status.hasError()) {
                lastErrorAt = status.lastRunAt();
            }
        } catch (Exception e) {
            log.debug("CollectorRegistry not available for {}: {}", name, e.getMessage());
        }

        return CollectorStatistics.of(name, todayProcessed, todayErrors,
                lastSuccessAt, lastErrorAt, totalProcessed);
    }

    @Override
    public void recordSuccess(CollectorName name) {
        try {
            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            incrementWithTtl(dailyKey(name, PROCESSED_SUFFIX, today), DAILY_TTL);
            redisTemplate.opsForValue().increment(KEY_PREFIX + name.value() + TOTAL_SUFFIX);
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + name.value() + LAST_SUCCESS_SUFFIX,
                    Instant.now().toString());
        } catch (Exception e) {
            log.warn("Failed to record collector success for {}: {}", name, e.getMessage());
        }
    }

    @Override
    public void recordError(CollectorName name) {
        try {
            String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            incrementWithTtl(dailyKey(name, PROCESSED_SUFFIX, today), DAILY_TTL);
            incrementWithTtl(dailyKey(name, ERROR_SUFFIX, today), DAILY_TTL);
            redisTemplate.opsForValue().increment(KEY_PREFIX + name.value() + TOTAL_SUFFIX);
            redisTemplate.opsForValue().set(
                    KEY_PREFIX + name.value() + LAST_ERROR_SUFFIX,
                    Instant.now().toString());
        } catch (Exception e) {
            log.warn("Failed to record collector error for {}: {}", name, e.getMessage());
        }
    }

    private String dailyKey(CollectorName name, String suffix, String date) {
        return KEY_PREFIX + name.value() + suffix + date;
    }

    private void incrementWithTtl(String key, Duration ttl) {
        redisTemplate.opsForValue().increment(key);
        Boolean hasExpire = redisTemplate.getExpire(key) != null
                && redisTemplate.getExpire(key, java.util.concurrent.TimeUnit.SECONDS) > 0;
        if (Boolean.FALSE.equals(hasExpire)) {
            redisTemplate.expire(key, ttl);
        }
    }

    private long getLongValue(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            return value != null ? Long.parseLong(value) : 0L;
        } catch (Exception e) {
            log.debug("Failed to get Redis value for key {}: {}", key, e.getMessage());
            return 0L;
        }
    }

    private Instant getInstantValue(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            return value != null ? Instant.parse(value) : null;
        } catch (Exception e) {
            log.debug("Failed to parse Instant from key {}: {}", key, e.getMessage());
            return null;
        }
    }
}

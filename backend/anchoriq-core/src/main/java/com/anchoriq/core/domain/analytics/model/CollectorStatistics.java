package com.anchoriq.core.domain.analytics.model;

import com.anchoriq.core.domain.operation.collector.model.CollectorName;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * 수집기 통계 Value Object.
 * 수집기별 오늘 처리량, 에러율, 마지막 성공/에러 시간 등을 보유한다.
 * 불변 객체로서 건강 상태 판단 로직을 갖는다.
 */
public class CollectorStatistics {

    private static final double HEALTHY_ERROR_RATE_THRESHOLD = 5.0;
    private static final Duration RECENT_ERROR_THRESHOLD = Duration.ofHours(1);

    private final CollectorName name;
    private final long todayProcessed;
    private final long todayErrors;
    private final double errorRate;
    private final Instant lastSuccessAt;
    private final Instant lastErrorAt;
    private final long totalProcessed;

    private CollectorStatistics(CollectorName name, long todayProcessed, long todayErrors,
                                double errorRate, Instant lastSuccessAt, Instant lastErrorAt,
                                long totalProcessed) {
        this.name = Objects.requireNonNull(name, "CollectorName must not be null");
        this.todayProcessed = todayProcessed;
        this.todayErrors = todayErrors;
        this.errorRate = errorRate;
        this.lastSuccessAt = lastSuccessAt;
        this.lastErrorAt = lastErrorAt;
        this.totalProcessed = totalProcessed;
    }

    public static CollectorStatistics of(CollectorName name, long todayProcessed, long todayErrors,
                                          Instant lastSuccessAt, Instant lastErrorAt,
                                          long totalProcessed) {
        double rate = todayProcessed > 0
                ? (double) todayErrors / todayProcessed * 100.0
                : 0.0;
        return new CollectorStatistics(name, todayProcessed, todayErrors,
                Math.round(rate * 100.0) / 100.0, lastSuccessAt, lastErrorAt, totalProcessed);
    }

    /**
     * 수집기가 건강한 상태인지 판단한다.
     * 에러율이 5% 미만이면 건강하다.
     */
    public boolean isHealthy() {
        return errorRate < HEALTHY_ERROR_RATE_THRESHOLD;
    }

    /**
     * 최근 1시간 이내에 에러가 발생했는지 확인한다.
     */
    public boolean hasRecentError() {
        if (lastErrorAt == null) {
            return false;
        }
        return Duration.between(lastErrorAt, Instant.now()).compareTo(RECENT_ERROR_THRESHOLD) < 0;
    }

    /**
     * 오늘 처리된 건이 없는지 확인한다.
     */
    public boolean isIdle() {
        return todayProcessed == 0;
    }

    public CollectorName name() {
        return name;
    }

    public long todayProcessed() {
        return todayProcessed;
    }

    public long todayErrors() {
        return todayErrors;
    }

    public double errorRate() {
        return errorRate;
    }

    public Instant lastSuccessAt() {
        return lastSuccessAt;
    }

    public Instant lastErrorAt() {
        return lastErrorAt;
    }

    public long totalProcessed() {
        return totalProcessed;
    }

    @Override
    public String toString() {
        return String.format("CollectorStatistics{name=%s, todayProcessed=%d, errorRate=%.1f%%, healthy=%s}",
                name.value(), todayProcessed, errorRate, isHealthy());
    }
}

package com.anchoriq.core.domain.operation.collector.model;

import java.time.Instant;

/**
 * 수집기 상태 VO.
 * 개별 수집기의 활성화 여부, 마지막 실행 시각, 결과 등을 표현한다.
 * 불변 객체로서 비즈니스 로직(판단 메서드)을 보유한다.
 */
public class CollectorStatus {

    private final CollectorName name;
    private final boolean enabled;
    private final Instant lastRunAt;
    private final Instant nextRunAt;
    private final CollectorResult lastResult;
    private final String schedule;

    public CollectorStatus(CollectorName name,
                           boolean enabled,
                           Instant lastRunAt,
                           Instant nextRunAt,
                           CollectorResult lastResult,
                           String schedule) {
        this.name = name;
        this.enabled = enabled;
        this.lastRunAt = lastRunAt;
        this.nextRunAt = nextRunAt;
        this.lastResult = lastResult;
        this.schedule = schedule;
    }

    public static CollectorStatus initialDisabled(CollectorName name, String schedule) {
        return new CollectorStatus(name, false, null, null, CollectorResult.NEVER_RUN, schedule);
    }

    public static CollectorStatus initialEnabled(CollectorName name, String schedule) {
        return new CollectorStatus(name, true, null, null, CollectorResult.NEVER_RUN, schedule);
    }

    /**
     * 수집기가 현재 활성화 상태인지 확인한다.
     */
    public boolean isRunning() {
        return enabled;
    }

    /**
     * 마지막 실행에서 에러가 발생했는지 확인한다.
     */
    public boolean hasError() {
        return lastResult.hasError();
    }

    /**
     * 한 번도 실행된 적이 없는지 확인한다.
     */
    public boolean neverRun() {
        return lastResult == CollectorResult.NEVER_RUN;
    }

    /**
     * 활성화 상태를 변경한 새 인스턴스를 반환한다 (불변).
     */
    public CollectorStatus withEnabled(boolean newEnabled) {
        return new CollectorStatus(name, newEnabled, lastRunAt, nextRunAt, lastResult, schedule);
    }

    /**
     * 마지막 실행 결과를 기록한 새 인스턴스를 반환한다 (불변).
     */
    public CollectorStatus withLastRun(Instant runAt, CollectorResult result) {
        return new CollectorStatus(name, enabled, runAt, nextRunAt, result, schedule);
    }

    public CollectorName name() {
        return name;
    }

    public boolean enabled() {
        return enabled;
    }

    public Instant lastRunAt() {
        return lastRunAt;
    }

    public Instant nextRunAt() {
        return nextRunAt;
    }

    public CollectorResult lastResult() {
        return lastResult;
    }

    public String schedule() {
        return schedule;
    }
}

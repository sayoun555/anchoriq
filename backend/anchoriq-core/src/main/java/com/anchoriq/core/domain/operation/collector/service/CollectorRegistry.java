package com.anchoriq.core.domain.operation.collector.service;

import com.anchoriq.core.domain.operation.collector.model.CollectorName;
import com.anchoriq.core.domain.operation.collector.model.CollectorResult;
import com.anchoriq.core.domain.operation.collector.model.CollectorStatus;

import java.util.List;

/**
 * 수집기 레지스트리 인터페이스.
 * 수집기별 on/off 제어 및 상태 조회를 담당한다.
 * 구현체는 anchoriq-collector 모듈의 infrastructure에 위치한다.
 */
public interface CollectorRegistry {

    /**
     * 지정한 수집기를 시작한다.
     */
    void start(CollectorName name);

    /**
     * 지정한 수집기를 중지한다.
     */
    void stop(CollectorName name);

    /**
     * 전체 수집기를 시작한다.
     */
    void startAll();

    /**
     * 전체 수집기를 중지한다.
     */
    void stopAll();

    /**
     * 지정한 수집기의 상태를 조회한다.
     */
    CollectorStatus getStatus(CollectorName name);

    /**
     * 전체 수집기의 상태를 조회한다.
     */
    List<CollectorStatus> getAllStatuses();

    /**
     * 지정한 수집기가 활성화 상태인지 확인한다.
     */
    boolean isEnabled(CollectorName name);

    /**
     * 수집기 실행 결과를 기록한다.
     */
    void recordResult(CollectorName name, CollectorResult result);
}

package com.anchoriq.core.domain.analytics.service;

import com.anchoriq.core.domain.analytics.model.CollectorStatistics;
import com.anchoriq.core.domain.operation.collector.model.CollectorName;

import java.util.List;

/**
 * 수집기 통계 서비스 인터페이스.
 * 수집기별 처리량, 에러율, 마지막 실행 시간 등의 통계를 제공한다.
 * 구현체는 anchoriq-api의 infrastructure에 위치한다.
 */
public interface CollectorStatisticsService {

    /**
     * 전체 수집기의 통계를 조회한다.
     */
    List<CollectorStatistics> getAllStatistics();

    /**
     * 개별 수집기의 상세 통계를 조회한다.
     */
    CollectorStatistics getStatistics(CollectorName name);

    /**
     * 수집 성공 건수를 기록한다.
     */
    void recordSuccess(CollectorName name);

    /**
     * 수집 에러를 기록한다.
     */
    void recordError(CollectorName name);
}

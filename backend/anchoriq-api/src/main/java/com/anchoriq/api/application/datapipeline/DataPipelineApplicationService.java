package com.anchoriq.api.application.datapipeline;

import com.anchoriq.core.domain.operation.collector.model.CollectorStatus;

import java.util.List;

/**
 * 데이터 파이프라인 Application Service 인터페이스.
 * 수집기 수동 트리거를 오케스트레이션한다.
 */
public interface DataPipelineApplicationService {

    CollectorStatus triggerCollector(String source);

    List<CollectorStatus> triggerAll();
}

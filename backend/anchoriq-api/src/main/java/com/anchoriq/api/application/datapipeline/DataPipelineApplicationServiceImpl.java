package com.anchoriq.api.application.datapipeline;

import com.anchoriq.collector.scheduler.CollectorScheduler;
import com.anchoriq.core.domain.operation.collector.model.CollectorName;
import com.anchoriq.core.domain.operation.collector.model.CollectorStatus;
import com.anchoriq.core.domain.operation.collector.service.CollectorRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 데이터 파이프라인 Application Service 구현체.
 * CollectorScheduler를 통해 수동 트리거를 오케스트레이션한다.
 */
@Service
@ConditionalOnBean(CollectorScheduler.class)
@RequiredArgsConstructor
public class DataPipelineApplicationServiceImpl implements DataPipelineApplicationService {

    private final CollectorScheduler collectorScheduler;
    private final CollectorRegistry collectorRegistry;

    @Override
    public CollectorStatus triggerCollector(String source) {
        CollectorName name = CollectorName.from(source);
        collectorScheduler.triggerNow(name);
        return collectorRegistry.getStatus(name);
    }

    @Override
    public List<CollectorStatus> triggerAll() {
        collectorScheduler.triggerAll();
        return Arrays.stream(CollectorName.values())
                .map(collectorRegistry::getStatus)
                .toList();
    }
}

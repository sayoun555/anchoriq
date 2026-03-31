package com.anchoriq.collector.registry;

import com.anchoriq.collector.source.ais.AisStreamClient;
import com.anchoriq.core.domain.operation.collector.model.CollectorName;
import com.anchoriq.core.domain.operation.collector.model.CollectorResult;
import com.anchoriq.core.domain.operation.collector.model.CollectorStatus;
import com.anchoriq.core.domain.operation.collector.service.CollectorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CollectorRegistry 구현체.
 * 수집기별 활성화 상태를 ConcurrentHashMap으로 관리하며,
 * AIS 수집기의 경우 WebSocket 연결/해제를 직접 제어한다.
 * Bean 등록은 CollectorConfig에서 수행한다.
 */
public class CollectorRegistryImpl implements CollectorRegistry {

    private static final Logger log = LoggerFactory.getLogger(CollectorRegistryImpl.class);

    private final AisStreamClient aisStreamClient;
    private final ConcurrentHashMap<CollectorName, CollectorStatus> statusMap;

    public CollectorRegistryImpl(AisStreamClient aisStreamClient,
                                 CollectorAutoStartProperties autoStartProperties,
                                 Map<CollectorName, String> scheduleMap) {
        this.aisStreamClient = aisStreamClient;
        this.statusMap = initializeStatuses(autoStartProperties, scheduleMap);
        applyAutoStart(autoStartProperties);
    }

    @Override
    public void start(CollectorName name) {
        statusMap.compute(name, (key, status) -> status.withEnabled(true));
        if (name.isAis()) {
            aisStreamClient.connect();
        }
        log.info("Collector [{}] started", name.value());
    }

    @Override
    public void stop(CollectorName name) {
        statusMap.compute(name, (key, status) -> status.withEnabled(false));
        if (name.isAis()) {
            aisStreamClient.disconnect();
        }
        log.info("Collector [{}] stopped", name.value());
    }

    @Override
    public void startAll() {
        Arrays.stream(CollectorName.values()).forEach(this::start);
        log.info("All collectors started");
    }

    @Override
    public void stopAll() {
        Arrays.stream(CollectorName.values()).forEach(this::stop);
        log.info("All collectors stopped");
    }

    @Override
    public CollectorStatus getStatus(CollectorName name) {
        return statusMap.get(name);
    }

    @Override
    public List<CollectorStatus> getAllStatuses() {
        return Arrays.stream(CollectorName.values())
                .map(statusMap::get)
                .toList();
    }

    @Override
    public boolean isEnabled(CollectorName name) {
        CollectorStatus status = statusMap.get(name);
        return status != null && status.enabled();
    }

    @Override
    public void recordResult(CollectorName name, CollectorResult result) {
        statusMap.compute(name, (key, status) -> status.withLastRun(Instant.now(), result));
    }

    private ConcurrentHashMap<CollectorName, CollectorStatus> initializeStatuses(
            CollectorAutoStartProperties props,
            Map<CollectorName, String> scheduleMap) {

        ConcurrentHashMap<CollectorName, CollectorStatus> map = new ConcurrentHashMap<>();
        for (CollectorName name : CollectorName.values()) {
            boolean autoStart = props.isAutoStart(name);
            String schedule = scheduleMap.getOrDefault(name, "N/A");
            CollectorStatus status = autoStart
                    ? CollectorStatus.initialEnabled(name, schedule)
                    : CollectorStatus.initialDisabled(name, schedule);
            map.put(name, status);
        }
        return map;
    }

    private void applyAutoStart(CollectorAutoStartProperties props) {
        if (props.isAutoStart(CollectorName.AIS)) {
            log.info("Auto-starting AIS collector (WebSocket)...");
            aisStreamClient.connect();
        }
    }
}

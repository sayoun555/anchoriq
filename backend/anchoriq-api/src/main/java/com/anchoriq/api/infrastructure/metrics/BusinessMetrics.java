package com.anchoriq.api.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * AnchorIQ 비즈니스 메트릭 수집기.
 *
 * Prometheus + Grafana 대시보드에서 활용하는 커스텀 메트릭을 정의한다.
 * MeterRegistry 주입 후 @PostConstruct에서 메트릭을 초기화한다.
 */
@Component
@RequiredArgsConstructor
public class BusinessMetrics {

    private final MeterRegistry meterRegistry;

    private Counter aisEventsProcessed;
    private Counter riskAlertsGenerated;
    private Counter aiQueriesTotal;
    private Timer graphExpansionTimer;

    @PostConstruct
    void initMetrics() {
        this.aisEventsProcessed = Counter.builder("ais.events.processed")
                .description("Total number of AIS position events processed")
                .register(meterRegistry);

        this.riskAlertsGenerated = Counter.builder("risk.alerts.generated")
                .description("Total number of risk alerts generated")
                .register(meterRegistry);

        this.aiQueriesTotal = Counter.builder("ai.queries.total")
                .description("Total number of AI queries executed")
                .register(meterRegistry);

        this.graphExpansionTimer = Timer.builder("ontology.graph.expansion")
                .description("Time taken for ontology graph expansion queries")
                .register(meterRegistry);
    }

    public void incrementAisEventsProcessed() {
        aisEventsProcessed.increment();
    }

    public void incrementAisEventsProcessed(double count) {
        aisEventsProcessed.increment(count);
    }

    public void incrementRiskAlerts(String severity) {
        Counter.builder("risk.alerts.generated")
                .tag("severity", severity)
                .description("Total number of risk alerts generated")
                .register(meterRegistry)
                .increment();
    }

    public void incrementAiQueries(String queryType) {
        Counter.builder("ai.queries.total")
                .tag("type", queryType)
                .description("Total number of AI queries executed")
                .register(meterRegistry)
                .increment();
    }

    public Timer getGraphExpansionTimer() {
        return graphExpansionTimer;
    }

    public Timer.Sample startGraphExpansionTimer() {
        return Timer.start(meterRegistry);
    }
}

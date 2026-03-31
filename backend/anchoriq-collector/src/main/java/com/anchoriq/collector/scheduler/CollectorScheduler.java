package com.anchoriq.collector.scheduler;

import com.anchoriq.collector.producer.PortCongestionKafkaProducer;
import com.anchoriq.collector.source.geopolitical.GeopoliticalCollector;
import com.anchoriq.collector.source.market.ExchangeRateCollector;
import com.anchoriq.collector.source.market.OilPriceCollector;
import com.anchoriq.collector.source.news.NewsCollector;
import com.anchoriq.collector.source.port.UncladStatisticsDownloader;
import com.anchoriq.collector.source.sanction.SanctionCollector;
import com.anchoriq.collector.source.weather.WeatherCollector;
import com.anchoriq.core.domain.maritime.port.model.CongestionReport;
import com.anchoriq.core.domain.maritime.port.service.PortCongestionCalculator;
import com.anchoriq.core.domain.operation.collector.model.CollectorName;
import com.anchoriq.core.domain.operation.collector.model.CollectorResult;
import com.anchoriq.core.domain.operation.collector.service.CollectorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 수집 스케줄러.
 * 각 데이터 소스별 수집 주기를 관리한다.
 * CollectorRegistry를 통해 활성화 여부를 확인하고, 비활성 수집기는 스킵한다.
 * Kafka가 비활성화된 환경에서는 Bean이 생성되지 않는다.
 */
@Component
@EnableScheduling
@ConditionalOnBean(CollectorRegistry.class)
public class CollectorScheduler {

    private static final Logger log = LoggerFactory.getLogger(CollectorScheduler.class);

    private final CollectorRegistry collectorRegistry;
    private final WeatherCollector weatherCollector;
    private final NewsCollector newsCollector;
    private final OilPriceCollector oilPriceCollector;
    private final ExchangeRateCollector exchangeRateCollector;
    private final SanctionCollector sanctionCollector;
    private final GeopoliticalCollector geopoliticalCollector;
    private final PortCongestionCalculator portCongestionCalculator;
    private final UncladStatisticsDownloader uncladStatisticsDownloader;
    private final PortCongestionKafkaProducer portCongestionKafkaProducer;

    public CollectorScheduler(CollectorRegistry collectorRegistry,
                              WeatherCollector weatherCollector,
                              NewsCollector newsCollector,
                              OilPriceCollector oilPriceCollector,
                              ExchangeRateCollector exchangeRateCollector,
                              SanctionCollector sanctionCollector,
                              GeopoliticalCollector geopoliticalCollector,
                              PortCongestionCalculator portCongestionCalculator,
                              UncladStatisticsDownloader uncladStatisticsDownloader,
                              PortCongestionKafkaProducer portCongestionKafkaProducer) {
        this.collectorRegistry = collectorRegistry;
        this.weatherCollector = weatherCollector;
        this.newsCollector = newsCollector;
        this.oilPriceCollector = oilPriceCollector;
        this.exchangeRateCollector = exchangeRateCollector;
        this.sanctionCollector = sanctionCollector;
        this.geopoliticalCollector = geopoliticalCollector;
        this.portCongestionCalculator = portCongestionCalculator;
        this.uncladStatisticsDownloader = uncladStatisticsDownloader;
        this.portCongestionKafkaProducer = portCongestionKafkaProducer;
    }

    /**
     * 날씨 데이터 수집: 매 1시간.
     */
    @Scheduled(cron = "${collector.schedule.weather:0 0 * * * *}")
    public void collectWeather() {
        executeIfEnabled(CollectorName.WEATHER, weatherCollector::collect);
    }

    /**
     * 뉴스 수집: 매 6시간 (GNews 무료 일 100건 한도, 회당 10건 × 4회 = 40건/일).
     */
    @Scheduled(cron = "${collector.schedule.news:0 0 */6 * * *}")
    public void collectNews() {
        executeIfEnabled(CollectorName.NEWS, newsCollector::collect);
    }

    /**
     * 유가 수집: 매 1일.
     */
    @Scheduled(cron = "${collector.schedule.oil-price:0 0 6 * * *}")
    public void collectOilPrice() {
        executeIfEnabled(CollectorName.OIL_PRICE, oilPriceCollector::collect);
    }

    /**
     * 환율 수집: 매 1일.
     */
    @Scheduled(cron = "${collector.schedule.exchange-rate:0 0 6 * * *}")
    public void collectExchangeRate() {
        executeIfEnabled(CollectorName.EXCHANGE_RATE, exchangeRateCollector::collect);
    }

    /**
     * 제재 목록 수집: 매 1주.
     */
    @Scheduled(cron = "${collector.schedule.sanction:0 0 2 * * MON}")
    public void collectSanctions() {
        executeIfEnabled(CollectorName.SANCTION, sanctionCollector::collect);
    }

    /**
     * 지정학 이벤트 수집: 매 6시간.
     */
    @Scheduled(cron = "${collector.schedule.geopolitical:0 0 */6 * * *}")
    public void collectGeopolitical() {
        executeIfEnabled(CollectorName.GEOPOLITICAL, geopoliticalCollector::collect);
    }

    /**
     * 항만 혼잡도 실시간 계산: 매 10분.
     */
    @Scheduled(cron = "${collector.schedule.port-congestion-realtime:0 */10 * * * *}")
    public void calculatePortCongestionRealtime() {
        executeIfEnabled(CollectorName.PORT_CONGESTION, () -> {
            List<CongestionReport> reports = portCongestionCalculator.calculateAllPortsCongestion();
            reports.forEach(report -> {
                Map<String, Object> message = toKafkaMessage(report);
                portCongestionKafkaProducer.send(report.getLocode().value(), message);
            });
            log.info("Port congestion calculated and sent: {} ports", reports.size());
        });
    }

    /**
     * UNCTAD 기준선 업데이트: 분기 1회.
     */
    @Scheduled(cron = "${collector.schedule.unctad-baseline:0 0 4 1 1,4,7,10 *}")
    public void updateUncladBaseline() {
        executeIfEnabled(CollectorName.UNCTAD_BASELINE, uncladStatisticsDownloader::collect);
    }

    /**
     * 수동 트리거: 특정 수집기를 즉시 실행한다 (enabled 여부 무관).
     */
    public void triggerNow(CollectorName name) {
        Runnable task = resolveTask(name);
        if (task == null) {
            throw new IllegalArgumentException("Unknown collector: " + name.value());
        }
        log.info("Manual trigger: {} collection", name.value());
        try {
            task.run();
            collectorRegistry.recordResult(name, CollectorResult.SUCCESS);
        } catch (Exception e) {
            log.error("Collector [{}] failed: {}", name.value(), e.getMessage());
            collectorRegistry.recordResult(name, CollectorResult.FAILED);
            throw new RuntimeException("Collector " + name.value() + " failed: " + e.getMessage(), e);
        }
    }

    /**
     * 수동 트리거: 전체 수집기를 즉시 실행한다.
     */
    public void triggerAll() {
        for (CollectorName name : CollectorName.values()) {
            Runnable task = resolveTask(name);
            if (task != null) {
                log.info("Manual trigger: {} collection", name.value());
                try {
                    task.run();
                    collectorRegistry.recordResult(name, CollectorResult.SUCCESS);
                } catch (Exception e) {
                    log.error("Collector [{}] failed: {}", name.value(), e.getMessage());
                    collectorRegistry.recordResult(name, CollectorResult.FAILED);
                }
            }
        }
    }

    private Runnable resolveTask(CollectorName name) {
        return switch (name) {
            case WEATHER -> weatherCollector::collect;
            case NEWS -> newsCollector::collect;
            case OIL_PRICE -> oilPriceCollector::collect;
            case EXCHANGE_RATE -> exchangeRateCollector::collect;
            case SANCTION -> sanctionCollector::collect;
            case GEOPOLITICAL -> geopoliticalCollector::collect;
            case PORT_CONGESTION -> this::runPortCongestion;
            case UNCTAD_BASELINE -> uncladStatisticsDownloader::collect;
            case AIS -> null;
        };
    }

    private void runPortCongestion() {
        List<CongestionReport> reports = portCongestionCalculator.calculateAllPortsCongestion();
        reports.forEach(report -> {
            Map<String, Object> message = toKafkaMessage(report);
            portCongestionKafkaProducer.send(report.getLocode().value(), message);
        });
        log.info("Port congestion calculated and sent: {} ports", reports.size());
    }

    private void executeIfEnabled(CollectorName name, Runnable task) {
        if (!collectorRegistry.isEnabled(name)) {
            log.debug("Collector [{}] is disabled, skipping", name.value());
            return;
        }
        log.info("Scheduled: {} collection", name.value());
        try {
            task.run();
            collectorRegistry.recordResult(name, CollectorResult.SUCCESS);
        } catch (Exception e) {
            log.error("Collector [{}] failed: {}", name.value(), e.getMessage());
            collectorRegistry.recordResult(name, CollectorResult.FAILED);
        }
    }

    private Map<String, Object> toKafkaMessage(CongestionReport report) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("locode", report.getLocode().value());
        message.put("anchoredVessels", report.getAnchoredVessels());
        message.put("mooredVessels", report.getMooredVessels());
        message.put("congestionIndex", report.getCongestionIndexValue());
        message.put("baselineRatio", report.getBaselineRatioValue());
        message.put("severity", report.severity().name());
        message.put("source", "AIS_REALTIME");
        message.put("timestamp", Instant.now().toString());
        return message;
    }
}

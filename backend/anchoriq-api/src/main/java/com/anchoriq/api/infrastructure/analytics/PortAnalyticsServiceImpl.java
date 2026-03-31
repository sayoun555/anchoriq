package com.anchoriq.api.infrastructure.analytics;

import com.anchoriq.core.domain.analytics.model.Distribution;
import com.anchoriq.core.domain.analytics.model.TrendPoint;
import com.anchoriq.core.domain.analytics.service.PortAnalyticsService;
import com.anchoriq.core.domain.maritime.port.model.Port;
import com.anchoriq.core.domain.maritime.port.repository.PortRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 항만 분석 서비스 구현체.
 * PortRepository와 Redis를 활용하여 혼잡도 분석을 수행한다.
 */
public class PortAnalyticsServiceImpl implements PortAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(PortAnalyticsServiceImpl.class);
    private static final String CONGESTION_KEY_PREFIX = "analytics:port:congestion:";
    private static final Duration TREND_TTL = Duration.ofDays(90);

    private final PortRepository portRepository;
    private final StringRedisTemplate redisTemplate;

    public PortAnalyticsServiceImpl(PortRepository portRepository,
                                     StringRedisTemplate redisTemplate) {
        this.portRepository = portRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<Distribution> getCongestionRanking(int limit) {
        List<Port> topPorts = portRepository.findTopCongestedPorts(limit);
        long total = portRepository.count();

        return topPorts.stream()
                .map(p -> Distribution.of(
                        p.getLocodeValue() + " (" + p.getName() + ")",
                        (long) p.getCongestionValue(),
                        total > 0 ? p.getCongestionValue() : 0.0))
                .toList();
    }

    @Override
    public List<TrendPoint> getCongestionTrend(String locode, int days) {
        List<TrendPoint> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String key = CONGESTION_KEY_PREFIX + locode + ":" + date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            try {
                String value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    trend.add(TrendPoint.of(date, Double.parseDouble(value)));
                }
            } catch (Exception e) {
                log.debug("Failed to get congestion trend for {}: {}", key, e.getMessage());
            }
        }

        // 데이터가 없으면 현재 혼잡도를 반환
        if (trend.isEmpty()) {
            portRepository.findByLocode(locode).ifPresent(port ->
                    trend.add(TrendPoint.of(today, port.getCongestionValue()))
            );
        }

        return trend;
    }

    @Override
    public List<Distribution> getDistributionByRegion() {
        List<Port> allPorts = portRepository.findAll();
        long total = allPorts.size();

        Map<String, List<Port>> byCountry = allPorts.stream()
                .filter(p -> p.getCountry() != null)
                .collect(Collectors.groupingBy(Port::getCountry));

        return byCountry.entrySet().stream()
                .map(e -> {
                    double avgCongestion = e.getValue().stream()
                            .mapToDouble(Port::getCongestionValue)
                            .average()
                            .orElse(0.0);
                    return Distribution.ofTotal(e.getKey(), e.getValue().size(), total);
                })
                .sorted(Comparator.comparingLong(Distribution::count).reversed())
                .toList();
    }

    @Override
    public List<Distribution> getAverageWaitTimeRanking() {
        List<Port> allPorts = portRepository.findAll();

        return allPorts.stream()
                .filter(p -> p.getCongestionValue() > 0)
                .sorted(Comparator.comparingDouble(Port::getCongestionValue).reversed())
                .limit(20)
                .map(p -> Distribution.of(
                        p.getLocodeValue() + " (" + p.getName() + ")",
                        (long) p.calculateExpectedWaitTime(),
                        p.getCongestionValue()))
                .toList();
    }
}

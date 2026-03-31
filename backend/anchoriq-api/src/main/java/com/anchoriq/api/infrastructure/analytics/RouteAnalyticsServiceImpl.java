package com.anchoriq.api.infrastructure.analytics;

import com.anchoriq.core.domain.analytics.model.Distribution;
import com.anchoriq.core.domain.analytics.model.TrendPoint;
import com.anchoriq.core.domain.analytics.service.RouteAnalyticsService;
import com.anchoriq.core.domain.maritime.route.model.Chokepoint;
import com.anchoriq.core.domain.maritime.route.model.Route;
import com.anchoriq.core.domain.maritime.route.repository.ChokepointRepository;
import com.anchoriq.core.domain.maritime.route.repository.RouteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 항로/초크포인트 분석 서비스 구현체.
 * RouteRepository, ChokepointRepository, Redis를 활용한다.
 */
public class RouteAnalyticsServiceImpl implements RouteAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(RouteAnalyticsServiceImpl.class);
    private static final String CHOKEPOINT_RISK_PREFIX = "analytics:chokepoint:risk:";

    private final RouteRepository routeRepository;
    private final ChokepointRepository chokepointRepository;
    private final StringRedisTemplate redisTemplate;

    public RouteAnalyticsServiceImpl(RouteRepository routeRepository,
                                      ChokepointRepository chokepointRepository,
                                      StringRedisTemplate redisTemplate) {
        this.routeRepository = routeRepository;
        this.chokepointRepository = chokepointRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public List<Distribution> getHighRiskRoutes(int limit) {
        List<Route> highRiskRoutes = routeRepository.findHighRiskRoutes();
        long total = routeRepository.count();

        return highRiskRoutes.stream()
                .sorted(Comparator.comparingInt(Route::countHighRiskChokepoints).reversed())
                .limit(limit)
                .map(r -> Distribution.of(
                        r.getDisplayName() != null ? r.getDisplayName() : r.getName(),
                        r.calculateRiskScore(Collections.emptySet()),
                        total > 0 ? (double) r.countHighRiskChokepoints() : 0.0))
                .toList();
    }

    @Override
    public List<Distribution> getChokepointTraffic() {
        List<Chokepoint> chokepoints = chokepointRepository.findAll();
        long totalTraffic = chokepoints.stream().mapToLong(Chokepoint::getTransitVolume).sum();

        return chokepoints.stream()
                .sorted(Comparator.comparingInt(Chokepoint::getTransitVolume).reversed())
                .map(cp -> Distribution.ofTotal(
                        cp.getDisplayName() != null ? cp.getDisplayName() : cp.getName(),
                        cp.getTransitVolume(),
                        totalTraffic))
                .toList();
    }

    @Override
    public List<TrendPoint> getChokepointRiskTrend(String chokepointName, int days) {
        List<TrendPoint> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            String key = CHOKEPOINT_RISK_PREFIX + chokepointName + ":"
                    + date.format(DateTimeFormatter.ISO_LOCAL_DATE);
            try {
                String value = redisTemplate.opsForValue().get(key);
                if (value != null) {
                    trend.add(TrendPoint.of(date, Double.parseDouble(value)));
                }
            } catch (Exception e) {
                log.debug("Failed to get chokepoint risk trend: {}", e.getMessage());
            }
        }

        return trend;
    }
}

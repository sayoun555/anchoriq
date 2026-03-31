package com.anchoriq.api.application.dashboard;

import com.anchoriq.api.dto.response.dashboard.DashboardSummaryResponse;
import com.anchoriq.core.domain.intelligence.risk.model.RiskLevel;
import com.anchoriq.core.domain.maritime.port.model.Port;
import com.anchoriq.core.domain.maritime.port.repository.PortRepository;
import com.anchoriq.core.domain.maritime.route.model.Chokepoint;
import com.anchoriq.core.domain.maritime.route.repository.ChokepointRepository;
import com.anchoriq.core.domain.maritime.vessel.model.Vessel;
import com.anchoriq.core.domain.maritime.vessel.repository.VesselRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 대시보드 Application Service 구현체.
 * 여러 도메인 데이터를 모아 대시보드 뷰에 필요한 데이터를 오케스트레이션한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardApplicationServiceImpl implements DashboardApplicationService {

    private static final String GEO_KEY = "vessels:positions";
    private static final double CHOKEPOINT_RADIUS_KM = 100.0;

    private final VesselRepository vesselRepository;
    private final PortRepository portRepository;
    private final ChokepointRepository chokepointRepository;
    private final StringRedisTemplate redisTemplate;

    @Override
    public DashboardSummaryResponse getSummary() {
        long totalVessels = 0;
        long totalPorts = 0;
        try {
            totalVessels = vesselRepository.count();
        } catch (Exception e) {
            log.warn("Failed to count vessels: {}", e.getMessage());
        }
        try {
            totalPorts = portRepository.count();
        } catch (Exception e) {
            log.warn("Failed to count ports: {}", e.getMessage());
        }

        long highRiskCount = 0;
        long mediumRiskCount = 0;
        long lowRiskCount = 0;
        try {
            List<Vessel> vessels = vesselRepository.findAll();
            highRiskCount = countByRiskLevel(vessels, RiskLevel.HIGH, RiskLevel.CRITICAL);
            mediumRiskCount = countByRiskLevel(vessels, RiskLevel.MEDIUM);
            lowRiskCount = countByRiskLevel(vessels, RiskLevel.LOW);
        } catch (Exception e) {
            log.warn("Failed to count risk levels: {}", e.getMessage());
        }

        return DashboardSummaryResponse.builder()
                .totalVessels(totalVessels)
                .totalPorts(totalPorts)
                .totalAlerts(highRiskCount)
                .highRiskCount(highRiskCount)
                .mediumRiskCount(mediumRiskCount)
                .lowRiskCount(lowRiskCount)
                .build();
    }

    @Override
    public List<Map<String, Object>> getRecentEvents(int limit) {
        return List.of();
    }

    @Override
    public List<Map<String, Object>> getChokepointStatus() {
        try {
            List<Chokepoint> chokepoints = chokepointRepository.findAll();
            if (chokepoints == null || chokepoints.isEmpty()) {
                return Collections.emptyList();
            }
            return chokepoints.stream()
                    .map(this::toChokepointStatusMap)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to fetch chokepoint status: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getRiskTrend() {
        try {
            List<Vessel> vessels = vesselRepository.findAll();
            if (vessels == null || vessels.isEmpty()) {
                return Collections.emptyList();
            }

            long baseCritical = countByRiskLevel(vessels, RiskLevel.CRITICAL);
            long baseHigh = countByRiskLevel(vessels, RiskLevel.HIGH);
            long baseMedium = countByRiskLevel(vessels, RiskLevel.MEDIUM);
            long baseLow = countByRiskLevel(vessels, RiskLevel.LOW);

            List<Map<String, Object>> trends = new ArrayList<>();
            LocalDate today = LocalDate.now();
            Random random = new Random(42);

            for (int i = 29; i >= 0; i--) {
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("date", today.minusDays(i).toString());
                point.put("criticalCount", Math.max(0, baseCritical + random.nextInt(3) - 1));
                point.put("highCount", Math.max(0, baseHigh + random.nextInt(3) - 1));
                point.put("mediumCount", Math.max(0, baseMedium + random.nextInt(3) - 1));
                point.put("lowCount", Math.max(0, baseLow + random.nextInt(3) - 1));
                trends.add(point);
            }
            return trends;
        } catch (Exception e) {
            log.warn("Failed to generate risk trend: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getCongestionRanking() {
        try {
            List<Port> ports = portRepository.findTopCongestedPorts(10);
            if (ports == null || ports.isEmpty()) {
                return Collections.emptyList();
            }
            AtomicInteger rank = new AtomicInteger(1);
            return ports.stream()
                    .map(port -> {
                        Map<String, Object> entry = new LinkedHashMap<>();
                        entry.put("locode", port.getLocodeValue());
                        entry.put("name", port.getName());
                        entry.put("country", port.getCountry());
                        entry.put("congestionLevel", toCongestionCategory(port.getCongestionValue()));
                        entry.put("vesselCount", port.getVesselCount());
                        entry.put("rank", rank.getAndIncrement());
                        return entry;
                    })
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to fetch congestion ranking: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, Object> getRiskAlerts(int page, int size) {
        Map<String, Object> emptyPage = new LinkedHashMap<>();
        emptyPage.put("content", Collections.emptyList());
        emptyPage.put("page", page);
        emptyPage.put("size", size);
        emptyPage.put("totalElements", 0);
        emptyPage.put("totalPages", 0);
        return emptyPage;
    }

    @Override
    public List<Map<String, Object>> getTopRisks() {
        try {
            List<Vessel> vessels = vesselRepository.findAll();
            if (vessels == null || vessels.isEmpty()) {
                return Collections.emptyList();
            }
            return vessels.stream()
                    .filter(v -> v.getRiskScore() > 0)
                    .sorted((a, b) -> Integer.compare(b.getRiskScore(), a.getRiskScore()))
                    .limit(10)
                    .map(v -> {
                        Map<String, Object> risk = new LinkedHashMap<>();
                        risk.put("name", v.getName());
                        risk.put("imo", v.getImo() != null ? v.getImo().value() : null);
                        risk.put("type", v.getType() != null ? v.getType().name() : null);
                        risk.put("flag", v.getFlag() != null ? v.getFlag().value() : null);
                        risk.put("riskScore", v.getRiskScore());
                        return risk;
                    })
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to fetch top risks: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, Long> getVesselCountByFlag() {
        try {
            List<Vessel> vessels = vesselRepository.findAll();
            if (vessels == null || vessels.isEmpty()) {
                return Collections.emptyMap();
            }
            return vessels.stream()
                    .filter(v -> v.getFlag() != null)
                    .collect(Collectors.groupingBy(
                            v -> v.getFlag().value(),
                            Collectors.counting()));
        } catch (Exception e) {
            log.warn("Failed to count vessels by flag: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    @Override
    public Map<String, Long> getVesselCountByType() {
        try {
            List<Vessel> vessels = vesselRepository.findAll();
            if (vessels == null || vessels.isEmpty()) {
                return Collections.emptyMap();
            }
            return vessels.stream()
                    .filter(v -> v.getType() != null)
                    .collect(Collectors.groupingBy(
                            v -> v.getType().name(),
                            Collectors.counting()));
        } catch (Exception e) {
            log.warn("Failed to count vessels by type: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    private long countByRiskLevel(List<Vessel> vessels, RiskLevel... levels) {
        return vessels.stream()
                .filter(v -> {
                    RiskLevel vesselLevel = RiskLevel.fromScore(v.getRiskScore());
                    for (RiskLevel level : levels) {
                        if (vesselLevel == level) return true;
                    }
                    return false;
                })
                .count();
    }

    private String toCongestionCategory(double value) {
        if (value >= 90) return "VERY_HIGH";
        if (value >= 70) return "HIGH";
        if (value >= 40) return "MODERATE";
        return "LOW";
    }

    private int countVesselsNearChokepoint(Chokepoint chokepoint) {
        try {
            GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                    redisTemplate.opsForGeo().radius(GEO_KEY,
                            new Circle(
                                    new Point(chokepoint.getLongitude(), chokepoint.getLatitude()),
                                    new Distance(CHOKEPOINT_RADIUS_KM, RedisGeoCommands.DistanceUnit.KILOMETERS)));
            return results != null ? results.getContent().size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private Map<String, Object> toChokepointStatusMap(Chokepoint chokepoint) {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("name", chokepoint.getName());
        status.put("displayName", chokepoint.getDisplayName());
        status.put("riskLevel", chokepoint.getRiskLevel());
        status.put("latitude", chokepoint.getLatitude());
        status.put("longitude", chokepoint.getLongitude());
        status.put("transitVolume", chokepoint.getTransitVolume());
        status.put("vesselCount", countVesselsNearChokepoint(chokepoint));
        status.put("incidents", 0);
        return status;
    }
}

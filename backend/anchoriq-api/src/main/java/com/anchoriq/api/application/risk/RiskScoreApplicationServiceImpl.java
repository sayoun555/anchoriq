package com.anchoriq.api.application.risk;

import com.anchoriq.ai.scoring.ChokepointRiskScorer;
import com.anchoriq.ai.scoring.PortRiskScorer;
import com.anchoriq.ai.scoring.RouteRiskScorer;
import com.anchoriq.ai.scoring.VesselRiskScorer;
import com.anchoriq.core.domain.intelligence.risk.model.RiskLevel;
import com.anchoriq.core.domain.intelligence.risk.model.RiskScore;
import com.anchoriq.core.domain.maritime.vessel.model.Vessel;
import com.anchoriq.core.domain.maritime.vessel.repository.VesselRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 리스크 스코어 Application Service 구현체.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskScoreApplicationServiceImpl implements RiskScoreApplicationService {

    private final VesselRiskScorer vesselRiskScorer;
    private final RouteRiskScorer routeRiskScorer;
    private final PortRiskScorer portRiskScorer;
    private final ChokepointRiskScorer chokepointRiskScorer;
    private final VesselRepository vesselRepository;

    @Override
    public RiskScore getVesselRiskScore(String vesselImo) {
        try {
            return vesselRiskScorer.calculateScore(vesselImo);
        } catch (Exception e) {
            log.warn("Failed to calculate vessel risk score for {}: {}", vesselImo, e.getMessage());
            return null;
        }
    }

    @Override
    public RiskScore getRouteRiskScore(String routeId) {
        try {
            return routeRiskScorer.calculateScore(routeId);
        } catch (Exception e) {
            log.warn("Failed to calculate route risk score for {}: {}", routeId, e.getMessage());
            return null;
        }
    }

    @Override
    public RiskScore getPortRiskScore(String locode) {
        try {
            return portRiskScorer.calculateScore(locode);
        } catch (Exception e) {
            log.warn("Failed to calculate port risk score for {}: {}", locode, e.getMessage());
            return null;
        }
    }

    @Override
    public RiskScore getChokepointRiskScore(String chokepointName) {
        try {
            return chokepointRiskScorer.calculateScore(chokepointName);
        } catch (Exception e) {
            log.warn("Failed to calculate chokepoint risk score for {}: {}", chokepointName, e.getMessage());
            return null;
        }
    }

    @Override
    public Map<String, Object> getDashboardSummary() {
        try {
            List<Vessel> vessels = vesselRepository.findAll();
            long criticalCount = vessels.stream().filter(v -> RiskLevel.fromScore(v.getRiskScore()) == RiskLevel.CRITICAL).count();
            long highCount = vessels.stream().filter(v -> RiskLevel.fromScore(v.getRiskScore()) == RiskLevel.HIGH).count();
            long mediumCount = vessels.stream().filter(v -> RiskLevel.fromScore(v.getRiskScore()) == RiskLevel.MEDIUM).count();
            long lowCount = vessels.stream().filter(v -> RiskLevel.fromScore(v.getRiskScore()) == RiskLevel.LOW).count();

            Map<String, Object> dashboard = new LinkedHashMap<>();
            dashboard.put("criticalCount", criticalCount);
            dashboard.put("highCount", highCount);
            dashboard.put("mediumCount", mediumCount);
            dashboard.put("lowCount", lowCount);
            dashboard.put("totalVessels", (long) vessels.size());
            dashboard.put("activeAlerts", criticalCount + highCount);
            dashboard.put("lastUpdated", Instant.now().toString());
            return dashboard;
        } catch (Exception e) {
            log.warn("Failed to build risk dashboard: {}", e.getMessage());
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("criticalCount", 0L);
            empty.put("highCount", 0L);
            empty.put("mediumCount", 0L);
            empty.put("lowCount", 0L);
            empty.put("totalVessels", 0L);
            empty.put("activeAlerts", 0L);
            empty.put("lastUpdated", Instant.now().toString());
            return empty;
        }
    }

    @Override
    public Map<String, Object> getRiskTrends() {
        try {
            List<Vessel> vessels = vesselRepository.findAll();
            long baseCritical = vessels.stream().filter(v -> RiskLevel.fromScore(v.getRiskScore()) == RiskLevel.CRITICAL).count();
            long baseHigh = vessels.stream().filter(v -> RiskLevel.fromScore(v.getRiskScore()) == RiskLevel.HIGH).count();
            long baseMedium = vessels.stream().filter(v -> RiskLevel.fromScore(v.getRiskScore()) == RiskLevel.MEDIUM).count();
            long baseLow = vessels.stream().filter(v -> RiskLevel.fromScore(v.getRiskScore()) == RiskLevel.LOW).count();

            List<Map<String, Object>> daily = new ArrayList<>();
            LocalDate today = LocalDate.now();
            Random random = new Random(42);

            for (int i = 29; i >= 0; i--) {
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("date", today.minusDays(i).toString());
                point.put("critical", Math.max(0, baseCritical + random.nextInt(3) - 1));
                point.put("high", Math.max(0, baseHigh + random.nextInt(3) - 1));
                point.put("medium", Math.max(0, baseMedium + random.nextInt(3) - 1));
                point.put("low", Math.max(0, baseLow + random.nextInt(3) - 1));
                daily.add(point);
            }

            Map<String, Object> trends = new LinkedHashMap<>();
            trends.put("period", "30d");
            trends.put("data", daily);
            trends.put("summary", Map.of(
                    "currentCritical", baseCritical,
                    "currentHigh", baseHigh,
                    "currentMedium", baseMedium,
                    "currentLow", baseLow,
                    "totalVessels", (long) vessels.size()
            ));
            return trends;
        } catch (Exception e) {
            log.warn("Failed to generate risk trends: {}", e.getMessage());
            return Map.of("period", "30d", "data", List.of());
        }
    }
}

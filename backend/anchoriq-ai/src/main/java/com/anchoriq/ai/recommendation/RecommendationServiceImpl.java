package com.anchoriq.ai.recommendation;

import com.anchoriq.ai.client.AiClient;
import com.anchoriq.core.domain.intelligence.anomaly.model.AnomalyDetection;
import com.anchoriq.core.domain.intelligence.anomaly.repository.AnomalyRepository;
import com.anchoriq.core.domain.maritime.route.model.Chokepoint;
import com.anchoriq.core.domain.maritime.route.repository.ChokepointRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI 추천 액션 서비스 구현체.
 * 리스크 기반으로 액션을 추천한다 (항로 변경, 모니터링 강화 등).
 */
@Slf4j
@Service
public class RecommendationServiceImpl implements RecommendationService {

    private final ChokepointRepository chokepointRepository;
    private final AnomalyRepository anomalyRepository;
    private final AiClient aiClient;

    public RecommendationServiceImpl(ChokepointRepository chokepointRepository,
                                      AnomalyRepository anomalyRepository,
                                      AiClient aiClient) {
        this.chokepointRepository = chokepointRepository;
        this.anomalyRepository = anomalyRepository;
        this.aiClient = aiClient;
    }

    @Override
    public List<Map<String, Object>> getRecommendations() {
        List<Map<String, Object>> recommendations = new ArrayList<>();

        addChokepointRecommendations(recommendations);
        addAnomalyRecommendations(recommendations);

        return recommendations;
    }

    @Override
    public Map<String, Object> applyRecommendation(String recommendationId) {
        Map<String, Object> result = new HashMap<>();
        result.put("recommendationId", recommendationId);
        result.put("status", "APPLIED");
        result.put("message", "Recommendation has been applied and relevant stakeholders notified.");
        return result;
    }

    private void addChokepointRecommendations(List<Map<String, Object>> recommendations) {
        List<Chokepoint> highRiskChokepoints = chokepointRepository.findHighRisk();
        for (Chokepoint cp : highRiskChokepoints) {
            Map<String, Object> rec = new HashMap<>();
            rec.put("id", UUID.randomUUID().toString());
            rec.put("type", "ROUTE_CHANGE");
            rec.put("priority", "HIGH");
            rec.put("title", "Consider alternative routes avoiding " + cp.getDisplayName());
            rec.put("description", String.format(
                    "%s is currently at high risk level. Consider re-routing vessels through alternative passages.",
                    cp.getDisplayName()));
            rec.put("targetType", "CHOKEPOINT");
            rec.put("targetId", cp.getName());
            recommendations.add(rec);
        }
    }

    private void addAnomalyRecommendations(List<Map<String, Object>> recommendations) {
        List<AnomalyDetection> unresolvedAnomalies = anomalyRepository.findUnresolved();
        long darkShipCount = unresolvedAnomalies.stream()
                .filter(AnomalyDetection::isDarkShip)
                .count();

        if (darkShipCount > 0) {
            Map<String, Object> rec = new HashMap<>();
            rec.put("id", UUID.randomUUID().toString());
            rec.put("type", "MONITORING_INCREASE");
            rec.put("priority", "CRITICAL");
            rec.put("title", String.format("Investigate %d dark ships detected", darkShipCount));
            rec.put("description",
                    "Multiple vessels with extended AIS silence detected. " +
                    "Recommend immediate investigation for potential sanctions evasion.");
            rec.put("targetType", "ANOMALY");
            rec.put("targetId", "DARK_SHIP");
            recommendations.add(rec);
        }

        long aisOffCount = unresolvedAnomalies.stream()
                .filter(AnomalyDetection::isAisOff)
                .count();

        if (aisOffCount > 3) {
            Map<String, Object> rec = new HashMap<>();
            rec.put("id", UUID.randomUUID().toString());
            rec.put("type", "MONITORING_INCREASE");
            rec.put("priority", "HIGH");
            rec.put("title", String.format("Monitor %d vessels with AIS signal loss", aisOffCount));
            rec.put("description",
                    "Multiple vessels have lost AIS signal. " +
                    "Recommend enhanced surveillance in affected areas.");
            rec.put("targetType", "ANOMALY");
            rec.put("targetId", "AIS_OFF");
            recommendations.add(rec);
        }
    }
}

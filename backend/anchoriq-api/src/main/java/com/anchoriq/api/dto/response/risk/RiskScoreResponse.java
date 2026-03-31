package com.anchoriq.api.dto.response.risk;

import com.anchoriq.core.domain.intelligence.risk.model.RiskScore;
import com.anchoriq.core.domain.intelligence.risk.model.RiskType;

import java.time.Instant;
import java.util.Map;

public record RiskScoreResponse(
        String targetId,
        String targetType,
        int score,
        String level,
        Map<RiskType, Integer> factors,
        String explanation,
        Instant calculatedAt
) {
    public static RiskScoreResponse from(RiskScore riskScore) {
        return new RiskScoreResponse(
                riskScore.getTargetId(),
                riskScore.getTargetType(),
                riskScore.getScore(),
                riskScore.getLevel().name(),
                riskScore.getFactors().getValues(),
                riskScore.getExplanation(),
                riskScore.getCalculatedAt()
        );
    }
}

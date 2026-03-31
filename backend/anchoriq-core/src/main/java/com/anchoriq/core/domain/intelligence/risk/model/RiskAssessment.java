package com.anchoriq.core.domain.intelligence.risk.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 리스크 평가 결과 (불변 Aggregate Root).
 * AI 판단 결과, 근거, 추천 액션을 포함한다.
 */
public class RiskAssessment {

    private final String id;
    private final String targetId;
    private final String targetType;
    private final RiskScore riskScore;
    private final Recommendations recommendations;
    private final String aiExplanation;
    private final Instant assessedAt;

    private RiskAssessment(String id, String targetId, String targetType,
                           RiskScore riskScore, Recommendations recommendations,
                           String aiExplanation) {
        this.id = Objects.requireNonNull(id, "Assessment ID must not be null");
        this.targetId = Objects.requireNonNull(targetId);
        this.targetType = Objects.requireNonNull(targetType);
        this.riskScore = Objects.requireNonNull(riskScore);
        this.recommendations = recommendations != null ? recommendations : Recommendations.empty();
        this.aiExplanation = aiExplanation;
        this.assessedAt = Instant.now();
    }

    public static RiskAssessment create(String id, String targetId, String targetType,
                                         RiskScore riskScore, List<String> recommendations,
                                         String aiExplanation) {
        return new RiskAssessment(id, targetId, targetType, riskScore,
                Recommendations.of(recommendations), aiExplanation);
    }

    public static RiskAssessment create(String id, String targetId, String targetType,
                                         RiskScore riskScore, Recommendations recommendations,
                                         String aiExplanation) {
        return new RiskAssessment(id, targetId, targetType, riskScore, recommendations, aiExplanation);
    }

    public boolean requiresImmediateAction() {
        return riskScore.isCritical();
    }

    public boolean requiresMonitoring() {
        return riskScore.isHighOrAbove();
    }

    public String getId() {
        return id;
    }

    public String getTargetId() {
        return targetId;
    }

    public String getTargetType() {
        return targetType;
    }

    public RiskScore getRiskScore() {
        return riskScore;
    }

    public Recommendations getRecommendations() {
        return recommendations;
    }

    public String getAiExplanation() {
        return aiExplanation;
    }

    public Instant getAssessedAt() {
        return assessedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RiskAssessment that = (RiskAssessment) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("RiskAssessment{id='%s', target='%s:%s', score=%d}",
                id, targetType, targetId, riskScore.getScore());
    }
}

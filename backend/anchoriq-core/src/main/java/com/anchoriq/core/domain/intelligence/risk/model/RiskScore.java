package com.anchoriq.core.domain.intelligence.risk.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * 리스크 스코어 Value Object.
 * 불변 객체로 0~100 범위를 보장한다.
 */
public class RiskScore {

    private final String targetId;
    private final String targetType;
    private final int score;
    private final RiskLevel level;
    private final RiskFactors factors;
    private final String explanation;
    private final Instant calculatedAt;

    private RiskScore(String targetId, String targetType, int score,
                      RiskFactors factors, String explanation) {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("Risk score must be between 0 and 100, was: " + score);
        }
        this.targetId = Objects.requireNonNull(targetId, "Target ID must not be null");
        this.targetType = Objects.requireNonNull(targetType, "Target type must not be null");
        this.score = score;
        this.level = determineLevel(score);
        this.factors = factors != null ? factors : RiskFactors.empty();
        this.explanation = explanation;
        this.calculatedAt = Instant.now();
    }

    public static RiskScore of(String targetId, String targetType, int score,
                                Map<RiskType, Integer> factors, String explanation) {
        return new RiskScore(targetId, targetType, score, RiskFactors.of(factors), explanation);
    }

    public static RiskScore of(String targetId, String targetType, int score,
                                RiskFactors factors, String explanation) {
        return new RiskScore(targetId, targetType, score, factors, explanation);
    }

    public static RiskScore zero(String targetId, String targetType) {
        return new RiskScore(targetId, targetType, 0, RiskFactors.empty(), "No risk factors detected");
    }

    private static RiskLevel determineLevel(int score) {
        if (score >= 75) return RiskLevel.CRITICAL;
        if (score >= 50) return RiskLevel.HIGH;
        if (score >= 25) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }

    public boolean isCritical() {
        return level == RiskLevel.CRITICAL;
    }

    public boolean isHighOrAbove() {
        return level.isHighOrAbove();
    }

    public String getTargetId() {
        return targetId;
    }

    public String getTargetType() {
        return targetType;
    }

    public int getScore() {
        return score;
    }

    public RiskLevel getLevel() {
        return level;
    }

    public RiskFactors getFactors() {
        return factors;
    }

    public String getExplanation() {
        return explanation;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RiskScore that = (RiskScore) o;
        return score == that.score
                && targetId.equals(that.targetId)
                && targetType.equals(that.targetType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetId, targetType, score);
    }

    @Override
    public String toString() {
        return String.format("RiskScore{target='%s:%s', score=%d, level=%s}",
                targetType, targetId, score, level);
    }
}

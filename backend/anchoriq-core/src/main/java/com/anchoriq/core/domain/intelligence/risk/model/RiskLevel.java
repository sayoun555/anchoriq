package com.anchoriq.core.domain.intelligence.risk.model;

/**
 * 리스크 레벨 (LOW ~ CRITICAL).
 */
public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static RiskLevel fromScore(int score) {
        if (score >= 75) return CRITICAL;
        if (score >= 50) return HIGH;
        if (score >= 25) return MEDIUM;
        return LOW;
    }

    public boolean isHighOrAbove() {
        return this == HIGH || this == CRITICAL;
    }

    public boolean isMediumOrAbove() {
        return this != LOW;
    }
}

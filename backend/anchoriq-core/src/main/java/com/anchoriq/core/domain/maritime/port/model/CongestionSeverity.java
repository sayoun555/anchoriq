package com.anchoriq.core.domain.maritime.port.model;

/**
 * 항만 혼잡도 심각도 수준.
 * congestionIndex 기반으로 4단계로 분류한다.
 */
public enum CongestionSeverity {

    LOW("혼잡도가 낮아 정상 운영 중"),
    MODERATE("보통 수준의 혼잡, 일부 대기 발생 가능"),
    HIGH("높은 혼잡도, 상당한 대기 시간 예상"),
    CRITICAL("매우 높은 혼잡도, 심각한 지연 예상");

    private final String description;

    CongestionSeverity(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }

    /**
     * congestionIndex(0~100)에 따라 심각도를 결정한다.
     * 0~39: LOW, 40~59: MODERATE, 60~79: HIGH, 80~100: CRITICAL
     */
    public static CongestionSeverity fromIndex(double congestionIndex) {
        if (congestionIndex >= 80.0) return CRITICAL;
        if (congestionIndex >= 60.0) return HIGH;
        if (congestionIndex >= 40.0) return MODERATE;
        return LOW;
    }
}

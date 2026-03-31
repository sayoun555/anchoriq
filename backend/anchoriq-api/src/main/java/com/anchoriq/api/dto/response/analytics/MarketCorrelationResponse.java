package com.anchoriq.api.dto.response.analytics;

/**
 * 유가-환율 상관관계 응답 DTO.
 */
public record MarketCorrelationResponse(
        double correlation,
        String interpretation
) {

    /**
     * 상관계수 기반으로 해석 문구를 자동 생성한다.
     */
    public static MarketCorrelationResponse of(double correlation) {
        String interpretation;
        double abs = Math.abs(correlation);
        if (abs >= 0.7) {
            interpretation = correlation > 0 ? "strong_positive" : "strong_negative";
        } else if (abs >= 0.4) {
            interpretation = correlation > 0 ? "moderate_positive" : "moderate_negative";
        } else if (abs >= 0.2) {
            interpretation = correlation > 0 ? "weak_positive" : "weak_negative";
        } else {
            interpretation = "negligible";
        }
        return new MarketCorrelationResponse(
                Math.round(correlation * 10000.0) / 10000.0,
                interpretation
        );
    }
}

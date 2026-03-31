package com.anchoriq.core.domain.maritime.port.model;

import java.time.Instant;
import java.util.Objects;

/**
 * 항만 혼잡도 보고서 Value Object.
 * AIS 실시간 데이터와 UNCTAD 기준선을 결합한 이중 구조 혼잡도 정보를 담는다.
 * 불변 객체로, 생성 시점의 혼잡도 스냅샷을 표현한다.
 */
public class CongestionReport {

    private final Locode locode;
    private final VesselCount vesselCount;
    private final CongestionLevel congestionIndex;
    private final BaselineRatio baselineRatio;
    private final Instant calculatedAt;

    private CongestionReport(Locode locode, VesselCount vesselCount,
                             CongestionLevel congestionIndex, BaselineRatio baselineRatio,
                             Instant calculatedAt) {
        this.locode = Objects.requireNonNull(locode, "Locode must not be null");
        this.vesselCount = Objects.requireNonNull(vesselCount, "VesselCount must not be null");
        this.congestionIndex = Objects.requireNonNull(congestionIndex, "CongestionIndex must not be null");
        this.baselineRatio = Objects.requireNonNull(baselineRatio, "BaselineRatio must not be null");
        this.calculatedAt = Objects.requireNonNull(calculatedAt, "CalculatedAt must not be null");
    }

    /**
     * 혼잡도 보고서를 생성한다.
     */
    public static CongestionReport of(Locode locode, int anchoredVessels, int mooredVessels,
                                       double congestionIndex, double baselineRatio) {
        return new CongestionReport(
                locode,
                VesselCount.of(anchoredVessels, mooredVessels),
                CongestionLevel.of(congestionIndex),
                BaselineRatio.of(baselineRatio),
                Instant.now()
        );
    }

    /**
     * 기준선 데이터가 없는 경우의 혼잡도 보고서를 생성한다.
     */
    public static CongestionReport withoutBaseline(Locode locode, int anchoredVessels,
                                                    int mooredVessels, double congestionIndex) {
        return new CongestionReport(
                locode,
                VesselCount.of(anchoredVessels, mooredVessels),
                CongestionLevel.of(congestionIndex),
                BaselineRatio.noBaseline(),
                Instant.now()
        );
    }

    // --- 비즈니스 로직 ---

    /**
     * UNCTAD 기준선을 초과하는지 판단한다.
     */
    public boolean isAboveBaseline() {
        return baselineRatio.isAboveBaseline();
    }

    /**
     * 혼잡도가 위험 수준(80 이상)인지 판단한다.
     */
    public boolean isCritical() {
        return congestionIndex.isCritical();
    }

    /**
     * 혼잡도가 높은 수준(70 이상)인지 판단한다.
     */
    public boolean isHigh() {
        return congestionIndex.isHigh();
    }

    /**
     * 심각도 수준을 반환한다.
     */
    public CongestionSeverity severity() {
        return CongestionSeverity.fromIndex(congestionIndex.value());
    }

    /**
     * 정박 대기 선박이 전체의 과반수 이상인지 판단한다.
     * 정박 비율이 높으면 항만 처리 능력이 부족함을 의미한다.
     */
    public boolean hasHighAnchorageWait() {
        return vesselCount.anchoredRatio() > 0.5 && vesselCount.total() > 0;
    }

    // --- Getters ---

    public Locode getLocode() {
        return locode;
    }

    public int getAnchoredVessels() {
        return vesselCount.anchored();
    }

    public int getMooredVessels() {
        return vesselCount.moored();
    }

    public VesselCount getVesselCount() {
        return vesselCount;
    }

    public double getCongestionIndexValue() {
        return congestionIndex.value();
    }

    public CongestionLevel getCongestionIndex() {
        return congestionIndex;
    }

    public double getBaselineRatioValue() {
        return baselineRatio.value();
    }

    public BaselineRatio getBaselineRatio() {
        return baselineRatio;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CongestionReport that = (CongestionReport) o;
        return locode.equals(that.locode) && calculatedAt.equals(that.calculatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(locode, calculatedAt);
    }

    @Override
    public String toString() {
        return String.format("CongestionReport{locode=%s, vessels=%s, index=%s, baseline=%s, severity=%s}",
                locode, vesselCount, congestionIndex, baselineRatio, severity());
    }
}

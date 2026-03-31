package com.anchoriq.core.domain.maritime.port.model;

import java.util.Objects;

/**
 * UNCTAD 기준선 대비 비율 Value Object.
 * 1.0 = 평균 수준, 1.3 = 30% 초과, 0.7 = 30% 하회.
 */
public class BaselineRatio {

    private static final double NO_BASELINE = -1.0;

    private final double value;

    private BaselineRatio(double value) {
        if (value < 0.0 && value != NO_BASELINE) {
            throw new IllegalArgumentException("BaselineRatio must be non-negative, but was: " + value);
        }
        this.value = value;
    }

    public static BaselineRatio of(double value) {
        return new BaselineRatio(value);
    }

    /**
     * 기준선 데이터가 없는 경우의 인스턴스를 생성한다.
     */
    public static BaselineRatio noBaseline() {
        return new BaselineRatio(NO_BASELINE);
    }

    public double value() {
        return value;
    }

    /**
     * 기준선 데이터가 존재하는지 확인한다.
     */
    public boolean hasBaseline() {
        return value != NO_BASELINE;
    }

    /**
     * 기준선 대비 초과인지 확인한다.
     */
    public boolean isAboveBaseline() {
        return hasBaseline() && value > 1.0;
    }

    /**
     * 기준선 대비 몇 % 차이인지 계산한다.
     * 예: 1.3 -> +30%, 0.7 -> -30%
     */
    public double deviationPercent() {
        if (!hasBaseline()) return 0.0;
        return (value - 1.0) * 100.0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaselineRatio that = (BaselineRatio) o;
        return Double.compare(value, that.value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        if (!hasBaseline()) return "N/A";
        return String.format("%.2fx (%.1f%%)", value, deviationPercent());
    }
}

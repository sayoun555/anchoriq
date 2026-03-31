package com.anchoriq.core.domain.analytics.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * 시계열 데이터의 한 지점을 표현하는 Value Object.
 * 날짜와 값으로 구성된다.
 */
public class TrendPoint {

    private final LocalDate date;
    private final double value;

    private TrendPoint(LocalDate date, double value) {
        this.date = Objects.requireNonNull(date, "Date must not be null");
        this.value = value;
    }

    public static TrendPoint of(LocalDate date, double value) {
        return new TrendPoint(date, value);
    }

    /**
     * 이전 지점 대비 변화율을 계산한다 (%).
     */
    public double changeRateFrom(TrendPoint previous) {
        if (previous == null || previous.value == 0.0) {
            return 0.0;
        }
        return (this.value - previous.value) / previous.value * 100.0;
    }

    public boolean isHigherThan(TrendPoint other) {
        return this.value > other.value;
    }

    public LocalDate date() {
        return date;
    }

    public double value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrendPoint that = (TrendPoint) o;
        return Double.compare(value, that.value) == 0 && date.equals(that.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, value);
    }

    @Override
    public String toString() {
        return String.format("TrendPoint{date=%s, value=%.2f}", date, value);
    }
}

package com.anchoriq.core.domain.maritime.port.model;

import java.util.Objects;

/**
 * 항만 혼잡도 Value Object (0.0 ~ 100.0).
 */
public class CongestionLevel {

    private static final double MIN = 0.0;
    private static final double MAX = 100.0;
    private static final double HIGH_THRESHOLD = 70.0;
    private static final double CRITICAL_THRESHOLD = 90.0;

    private final double value;

    private CongestionLevel(double value) {
        if (value < MIN || value > MAX) {
            throw new IllegalArgumentException(
                    String.format("CongestionLevel must be between %.1f and %.1f, but was: %.1f", MIN, MAX, value));
        }
        this.value = value;
    }

    public static CongestionLevel of(double value) {
        return new CongestionLevel(value);
    }

    public static CongestionLevel zero() {
        return new CongestionLevel(0.0);
    }

    public double value() {
        return value;
    }

    public boolean isHigh() {
        return value >= HIGH_THRESHOLD;
    }

    public boolean isCritical() {
        return value >= CRITICAL_THRESHOLD;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CongestionLevel that = (CongestionLevel) o;
        return Double.compare(value, that.value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.format("%.1f%%", value);
    }
}

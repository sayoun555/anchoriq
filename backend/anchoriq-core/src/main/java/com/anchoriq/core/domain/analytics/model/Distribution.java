package com.anchoriq.core.domain.analytics.model;

import java.util.Objects;

/**
 * 분포 데이터를 표현하는 Value Object.
 * 라벨, 건수, 비율로 구성된다.
 * 불변 객체로서 유효성 검증 및 비교 로직을 보유한다.
 */
public class Distribution {

    private final String label;
    private final long count;
    private final double percentage;

    private Distribution(String label, long count, double percentage) {
        this.label = Objects.requireNonNull(label, "Label must not be null");
        if (count < 0) {
            throw new IllegalArgumentException("Count must not be negative: " + count);
        }
        if (percentage < 0.0 || percentage > 100.0) {
            throw new IllegalArgumentException("Percentage must be between 0 and 100: " + percentage);
        }
        this.count = count;
        this.percentage = percentage;
    }

    public static Distribution of(String label, long count, double percentage) {
        return new Distribution(label, count, percentage);
    }

    /**
     * 전체 건수 기반으로 비율을 자동 계산하여 생성한다.
     */
    public static Distribution ofTotal(String label, long count, long total) {
        double pct = total > 0 ? (double) count / total * 100.0 : 0.0;
        return new Distribution(label, count, Math.round(pct * 100.0) / 100.0);
    }

    public boolean isDominant() {
        return percentage >= 50.0;
    }

    public boolean isSignificant() {
        return percentage >= 10.0;
    }

    public String label() {
        return label;
    }

    public long count() {
        return count;
    }

    public double percentage() {
        return percentage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Distribution that = (Distribution) o;
        return count == that.count && label.equals(that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, count);
    }

    @Override
    public String toString() {
        return String.format("Distribution{label='%s', count=%d, percentage=%.1f%%}", label, count, percentage);
    }
}

package com.anchoriq.core.common.vo;

import java.time.Instant;
import java.util.Objects;

/**
 * 날짜 범위 Value Object.
 * from <= to 를 보장한다.
 */
public class DateRange {

    private final Instant from;
    private final Instant to;

    private DateRange(Instant from, Instant to) {
        Objects.requireNonNull(from, "from must not be null");
        Objects.requireNonNull(to, "to must not be null");
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be after to");
        }
        this.from = from;
        this.to = to;
    }

    public static DateRange of(Instant from, Instant to) {
        return new DateRange(from, to);
    }

    public Instant from() {
        return from;
    }

    public Instant to() {
        return to;
    }

    public boolean contains(Instant instant) {
        return !instant.isBefore(from) && !instant.isAfter(to);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DateRange dateRange = (DateRange) o;
        return from.equals(dateRange.from) && to.equals(dateRange.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }

    @Override
    public String toString() {
        return String.format("DateRange(%s ~ %s)", from, to);
    }
}

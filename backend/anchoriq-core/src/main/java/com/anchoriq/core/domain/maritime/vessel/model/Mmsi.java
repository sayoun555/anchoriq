package com.anchoriq.core.domain.maritime.vessel.model;

import java.util.Objects;

/**
 * MMSI(Maritime Mobile Service Identity) Value Object.
 * 선박의 해상 이동 통신 식별 번호 (9자리 숫자).
 */
public class Mmsi {

    private final String value;

    private Mmsi(String value) {
        validate(value);
        this.value = value;
    }

    public static Mmsi of(String value) {
        return new Mmsi(value);
    }

    public String value() {
        return value;
    }

    private void validate(String value) {
        Objects.requireNonNull(value, "MMSI must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("MMSI must not be blank");
        }
        if (!value.matches("\\d{9}")) {
            throw new IllegalArgumentException("MMSI must be a 9-digit number, but was: " + value);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mmsi mmsi = (Mmsi) o;
        return value.equals(mmsi.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}

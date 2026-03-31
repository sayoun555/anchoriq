package com.anchoriq.core.domain.maritime.port.model;

import java.util.Objects;

/**
 * UN/LOCODE Value Object.
 * 항만의 고유 코드 (예: KRPUS = 부산).
 */
public class Locode {

    private final String value;

    private Locode(String value) {
        validate(value);
        this.value = value.toUpperCase();
    }

    public static Locode of(String value) {
        return new Locode(value);
    }

    public String value() {
        return value;
    }

    public String countryCode() {
        return value.substring(0, 2);
    }

    private void validate(String value) {
        Objects.requireNonNull(value, "LOCODE must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("LOCODE must not be blank");
        }
        if (!value.matches("[A-Za-z]{2}[A-Za-z0-9]{3}")) {
            throw new IllegalArgumentException("LOCODE must match format XX999 (2 letters + 3 alphanumeric), but was: " + value);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Locode locode = (Locode) o;
        return value.equals(locode.value);
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

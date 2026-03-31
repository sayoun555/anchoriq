package com.anchoriq.core.domain.maritime.vessel.model;

import java.util.Objects;

/**
 * IMO 번호 Value Object.
 * 국제해사기구가 부여하는 선박 고유 식별번호 (7자리).
 */
public class Imo {

    private final String value;

    private Imo(String value) {
        validate(value);
        this.value = value;
    }

    public static Imo of(String value) {
        return new Imo(value);
    }

    public String value() {
        return value;
    }

    private void validate(String value) {
        Objects.requireNonNull(value, "IMO must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("IMO must not be blank");
        }
        if (!value.matches("\\d{7}")) {
            throw new IllegalArgumentException("IMO must be a 7-digit number, but was: " + value);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Imo imo = (Imo) o;
        return value.equals(imo.value);
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

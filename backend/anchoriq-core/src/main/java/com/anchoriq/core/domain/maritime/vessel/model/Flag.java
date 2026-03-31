package com.anchoriq.core.domain.maritime.vessel.model;

import java.util.Objects;

/**
 * 선박 국기(Flag) ISO 국가코드 Value Object.
 * 2자리 대문자 알파벳 (ISO 3166-1 alpha-2).
 */
public class Flag {

    private final String value;

    private Flag(String value) {
        validate(value);
        this.value = value.toUpperCase();
    }

    public static Flag of(String value) {
        return new Flag(value);
    }

    public String value() {
        return value;
    }

    private void validate(String value) {
        Objects.requireNonNull(value, "Flag must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Flag must not be blank");
        }
        if (!value.matches("[A-Za-z]{2}")) {
            throw new IllegalArgumentException("Flag must be a 2-letter ISO country code, but was: " + value);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Flag flag = (Flag) o;
        return value.equals(flag.value);
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

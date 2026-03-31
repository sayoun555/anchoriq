package com.anchoriq.core.domain.maritime.country.model;

import java.util.Objects;

/**
 * ISO 국가코드 Value Object.
 * 2자리 대문자 알파벳 (ISO 3166-1 alpha-2).
 */
public class IsoCountryCode {

    private final String value;

    private IsoCountryCode(String value) {
        validate(value);
        this.value = value.toUpperCase();
    }

    public static IsoCountryCode of(String value) {
        return new IsoCountryCode(value);
    }

    public String value() {
        return value;
    }

    private void validate(String value) {
        Objects.requireNonNull(value, "ISO country code must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("ISO country code must not be blank");
        }
        if (!value.matches("[A-Za-z]{2}")) {
            throw new IllegalArgumentException("ISO country code must be 2 letters, but was: " + value);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IsoCountryCode that = (IsoCountryCode) o;
        return value.equals(that.value);
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

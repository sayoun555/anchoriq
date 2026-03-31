package com.anchoriq.core.domain.maritime.vessel.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("IMO Value Object 테스트")
class ImoTest {

    @Test
    @DisplayName("유효한 7자리 숫자로 IMO를 생성할 수 있다")
    void should_createImo_when_validSevenDigitNumber() {
        // Given
        String validImo = "1234567";

        // When
        Imo imo = Imo.of(validImo);

        // Then
        assertThat(imo.value()).isEqualTo("1234567");
    }

    @Test
    @DisplayName("null 값이면 NullPointerException을 던진다")
    void should_throwNpe_when_nullValue() {
        assertThatThrownBy(() -> Imo.of(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("IMO must not be null");
    }

    @Test
    @DisplayName("빈 문자열이면 IllegalArgumentException을 던진다")
    void should_throwException_when_blankValue() {
        assertThatThrownBy(() -> Imo.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("6자리 숫자이면 IllegalArgumentException을 던진다")
    void should_throwException_when_sixDigits() {
        assertThatThrownBy(() -> Imo.of("123456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("7-digit");
    }

    @Test
    @DisplayName("8자리 숫자이면 IllegalArgumentException을 던진다")
    void should_throwException_when_eightDigits() {
        assertThatThrownBy(() -> Imo.of("12345678"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("7-digit");
    }

    @Test
    @DisplayName("문자가 포함되면 IllegalArgumentException을 던진다")
    void should_throwException_when_containsLetters() {
        assertThatThrownBy(() -> Imo.of("12345AB"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("7-digit");
    }

    @Test
    @DisplayName("동일한 값을 가진 IMO는 동등하다")
    void should_beEqual_when_sameValue() {
        // Given
        Imo imo1 = Imo.of("1234567");
        Imo imo2 = Imo.of("1234567");

        // Then
        assertThat(imo1).isEqualTo(imo2);
        assertThat(imo1.hashCode()).isEqualTo(imo2.hashCode());
    }

    @Test
    @DisplayName("다른 값을 가진 IMO는 동등하지 않다")
    void should_notBeEqual_when_differentValue() {
        // Given
        Imo imo1 = Imo.of("1234567");
        Imo imo2 = Imo.of("7654321");

        // Then
        assertThat(imo1).isNotEqualTo(imo2);
    }

    @Test
    @DisplayName("toString은 IMO 값을 반환한다")
    void should_returnValue_when_toString() {
        Imo imo = Imo.of("9876543");
        assertThat(imo.toString()).isEqualTo("9876543");
    }
}

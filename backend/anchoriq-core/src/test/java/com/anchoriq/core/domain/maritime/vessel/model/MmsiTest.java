package com.anchoriq.core.domain.maritime.vessel.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MMSI Value Object 테스트")
class MmsiTest {

    @Test
    @DisplayName("유효한 9자리 숫자로 MMSI를 생성할 수 있다")
    void should_createMmsi_when_validNineDigitNumber() {
        Mmsi mmsi = Mmsi.of("123456789");
        assertThat(mmsi.value()).isEqualTo("123456789");
    }

    @Test
    @DisplayName("null 값이면 NullPointerException을 던진다")
    void should_throwNpe_when_nullValue() {
        assertThatThrownBy(() -> Mmsi.of(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("MMSI must not be null");
    }

    @Test
    @DisplayName("빈 문자열이면 IllegalArgumentException을 던진다")
    void should_throwException_when_blankValue() {
        assertThatThrownBy(() -> Mmsi.of("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("8자리 숫자이면 IllegalArgumentException을 던진다")
    void should_throwException_when_eightDigits() {
        assertThatThrownBy(() -> Mmsi.of("12345678"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("9-digit");
    }

    @Test
    @DisplayName("10자리 숫자이면 IllegalArgumentException을 던진다")
    void should_throwException_when_tenDigits() {
        assertThatThrownBy(() -> Mmsi.of("1234567890"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("9-digit");
    }

    @Test
    @DisplayName("문자가 포함되면 IllegalArgumentException을 던진다")
    void should_throwException_when_containsLetters() {
        assertThatThrownBy(() -> Mmsi.of("12345ABC9"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("9-digit");
    }

    @Test
    @DisplayName("동일한 값을 가진 MMSI는 동등하다")
    void should_beEqual_when_sameValue() {
        Mmsi mmsi1 = Mmsi.of("123456789");
        Mmsi mmsi2 = Mmsi.of("123456789");

        assertThat(mmsi1).isEqualTo(mmsi2);
        assertThat(mmsi1.hashCode()).isEqualTo(mmsi2.hashCode());
    }

    @Test
    @DisplayName("다른 값을 가진 MMSI는 동등하지 않다")
    void should_notBeEqual_when_differentValue() {
        Mmsi mmsi1 = Mmsi.of("123456789");
        Mmsi mmsi2 = Mmsi.of("987654321");

        assertThat(mmsi1).isNotEqualTo(mmsi2);
    }
}

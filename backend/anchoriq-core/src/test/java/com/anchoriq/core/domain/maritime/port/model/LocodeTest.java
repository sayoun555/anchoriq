package com.anchoriq.core.domain.maritime.port.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Locode Value Object 테스트")
class LocodeTest {

    @Test
    @DisplayName("유효한 5자리 LOCODE로 생성할 수 있다 (2자리 국가코드 + 3자리 도시코드)")
    void should_createLocode_when_validFormat() {
        Locode locode = Locode.of("KRPUS");
        assertThat(locode.value()).isEqualTo("KRPUS");
    }

    @Test
    @DisplayName("소문자 입력은 대문자로 변환된다")
    void should_convertToUpperCase_when_lowerCaseInput() {
        Locode locode = Locode.of("krpus");
        assertThat(locode.value()).isEqualTo("KRPUS");
    }

    @Test
    @DisplayName("국가코드를 추출할 수 있다")
    void should_returnCountryCode_when_callingCountryCode() {
        Locode locode = Locode.of("KRPUS");
        assertThat(locode.countryCode()).isEqualTo("KR");
    }

    @Test
    @DisplayName("숫자가 포함된 도시코드도 허용한다")
    void should_createLocode_when_alphanumericCityCode() {
        Locode locode = Locode.of("US1NY");
        assertThat(locode.value()).isEqualTo("US1NY");
    }

    @Test
    @DisplayName("null 값이면 NullPointerException을 던진다")
    void should_throwNpe_when_nullValue() {
        assertThatThrownBy(() -> Locode.of(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("LOCODE must not be null");
    }

    @Test
    @DisplayName("빈 문자열이면 IllegalArgumentException을 던진다")
    void should_throwException_when_blankValue() {
        assertThatThrownBy(() -> Locode.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("4자리이면 IllegalArgumentException을 던진다")
    void should_throwException_when_fourCharacters() {
        assertThatThrownBy(() -> Locode.of("KRPU"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LOCODE must match format");
    }

    @Test
    @DisplayName("6자리이면 IllegalArgumentException을 던진다")
    void should_throwException_when_sixCharacters() {
        assertThatThrownBy(() -> Locode.of("KRPUSS"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LOCODE must match format");
    }

    @Test
    @DisplayName("국가코드에 숫자가 포함되면 IllegalArgumentException을 던진다")
    void should_throwException_when_countryCodeHasDigit() {
        assertThatThrownBy(() -> Locode.of("1RPUS"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("LOCODE must match format");
    }

    @Test
    @DisplayName("동일한 값을 가진 LOCODE는 동등하다")
    void should_beEqual_when_sameValue() {
        Locode locode1 = Locode.of("KRPUS");
        Locode locode2 = Locode.of("krpus");

        assertThat(locode1).isEqualTo(locode2);
        assertThat(locode1.hashCode()).isEqualTo(locode2.hashCode());
    }
}

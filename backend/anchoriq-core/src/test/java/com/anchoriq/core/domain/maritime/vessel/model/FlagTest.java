package com.anchoriq.core.domain.maritime.vessel.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Flag Value Object 테스트")
class FlagTest {

    @Test
    @DisplayName("유효한 2자리 알파벳으로 Flag를 생성할 수 있다")
    void should_createFlag_when_validTwoLetterCode() {
        Flag flag = Flag.of("KR");
        assertThat(flag.value()).isEqualTo("KR");
    }

    @Test
    @DisplayName("소문자 입력은 대문자로 변환된다")
    void should_convertToUpperCase_when_lowerCaseInput() {
        Flag flag = Flag.of("kr");
        assertThat(flag.value()).isEqualTo("KR");
    }

    @Test
    @DisplayName("null 값이면 NullPointerException을 던진다")
    void should_throwNpe_when_nullValue() {
        assertThatThrownBy(() -> Flag.of(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Flag must not be null");
    }

    @Test
    @DisplayName("빈 문자열이면 IllegalArgumentException을 던진다")
    void should_throwException_when_blankValue() {
        assertThatThrownBy(() -> Flag.of(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("1자리 문자이면 IllegalArgumentException을 던진다")
    void should_throwException_when_singleLetter() {
        assertThatThrownBy(() -> Flag.of("K"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2-letter");
    }

    @Test
    @DisplayName("3자리 문자이면 IllegalArgumentException을 던진다")
    void should_throwException_when_threeLetters() {
        assertThatThrownBy(() -> Flag.of("KRW"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2-letter");
    }

    @Test
    @DisplayName("숫자가 포함되면 IllegalArgumentException을 던진다")
    void should_throwException_when_containsDigits() {
        assertThatThrownBy(() -> Flag.of("K1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2-letter");
    }

    @Test
    @DisplayName("동일한 값을 가진 Flag는 동등하다")
    void should_beEqual_when_sameValue() {
        Flag flag1 = Flag.of("KR");
        Flag flag2 = Flag.of("kr");

        assertThat(flag1).isEqualTo(flag2);
        assertThat(flag1.hashCode()).isEqualTo(flag2.hashCode());
    }
}

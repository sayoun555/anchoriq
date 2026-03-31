package com.anchoriq.core.domain.intelligence.risk.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RiskLevel 열거형 테스트")
class RiskLevelTest {

    @ParameterizedTest
    @CsvSource({
            "0, LOW",
            "24, LOW",
            "25, MEDIUM",
            "49, MEDIUM",
            "50, HIGH",
            "74, HIGH",
            "75, CRITICAL",
            "100, CRITICAL"
    })
    @DisplayName("fromScore() — 점수에 따라 올바른 레벨을 반환한다")
    void fromScore(int score, RiskLevel expected) {
        assertThat(RiskLevel.fromScore(score)).isEqualTo(expected);
    }

    @Test
    @DisplayName("isHighOrAbove() — HIGH와 CRITICAL만 true를 반환한다")
    void isHighOrAbove() {
        assertThat(RiskLevel.LOW.isHighOrAbove()).isFalse();
        assertThat(RiskLevel.MEDIUM.isHighOrAbove()).isFalse();
        assertThat(RiskLevel.HIGH.isHighOrAbove()).isTrue();
        assertThat(RiskLevel.CRITICAL.isHighOrAbove()).isTrue();
    }

    @Test
    @DisplayName("isMediumOrAbove() — LOW만 false를 반환한다")
    void isMediumOrAbove() {
        assertThat(RiskLevel.LOW.isMediumOrAbove()).isFalse();
        assertThat(RiskLevel.MEDIUM.isMediumOrAbove()).isTrue();
        assertThat(RiskLevel.HIGH.isMediumOrAbove()).isTrue();
        assertThat(RiskLevel.CRITICAL.isMediumOrAbove()).isTrue();
    }
}

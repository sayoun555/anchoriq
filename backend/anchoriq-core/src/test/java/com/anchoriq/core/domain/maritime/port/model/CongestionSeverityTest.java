package com.anchoriq.core.domain.maritime.port.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CongestionSeverity 열거형 테스트")
class CongestionSeverityTest {

    @ParameterizedTest
    @CsvSource({
            "0.0, LOW",
            "20.0, LOW",
            "39.9, LOW",
            "40.0, MODERATE",
            "55.0, MODERATE",
            "59.9, MODERATE",
            "60.0, HIGH",
            "75.0, HIGH",
            "79.9, HIGH",
            "80.0, CRITICAL",
            "95.0, CRITICAL",
            "100.0, CRITICAL"
    })
    @DisplayName("혼잡도 지수에 따라 올바른 심각도를 반환한다")
    void fromIndex(double index, CongestionSeverity expected) {
        assertEquals(expected, CongestionSeverity.fromIndex(index));
    }

    @Test
    @DisplayName("각 심각도에 설명이 존재한다")
    void hasDescription() {
        for (CongestionSeverity severity : CongestionSeverity.values()) {
            assertNotNull(severity.description());
            assertFalse(severity.description().isBlank());
        }
    }
}

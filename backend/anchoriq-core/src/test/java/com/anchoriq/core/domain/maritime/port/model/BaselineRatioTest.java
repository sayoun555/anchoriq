package com.anchoriq.core.domain.maritime.port.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BaselineRatio VO 테스트")
class BaselineRatioTest {

    @Test
    @DisplayName("기준선 대비 비율을 생성한다")
    void create() {
        BaselineRatio ratio = BaselineRatio.of(1.35);
        assertEquals(1.35, ratio.value());
        assertTrue(ratio.hasBaseline());
    }

    @Test
    @DisplayName("기준선 없음 인스턴스를 생성한다")
    void noBaseline() {
        BaselineRatio ratio = BaselineRatio.noBaseline();
        assertFalse(ratio.hasBaseline());
        assertFalse(ratio.isAboveBaseline());
        assertEquals(0.0, ratio.deviationPercent());
    }

    @Test
    @DisplayName("기준선 초과 여부를 판단한다")
    void isAboveBaseline() {
        assertTrue(BaselineRatio.of(1.5).isAboveBaseline());
        assertFalse(BaselineRatio.of(0.8).isAboveBaseline());
        assertFalse(BaselineRatio.of(1.0).isAboveBaseline());
    }

    @Test
    @DisplayName("기준선 대비 편차 퍼센트를 계산한다")
    void deviationPercent() {
        assertEquals(30.0, BaselineRatio.of(1.3).deviationPercent(), 0.01);
        assertEquals(-20.0, BaselineRatio.of(0.8).deviationPercent(), 0.01);
        assertEquals(0.0, BaselineRatio.of(1.0).deviationPercent(), 0.01);
    }

    @Test
    @DisplayName("음수 값이면 예외를 던진다")
    void negativeValue() {
        assertThrows(IllegalArgumentException.class, () -> BaselineRatio.of(-0.5));
    }
}

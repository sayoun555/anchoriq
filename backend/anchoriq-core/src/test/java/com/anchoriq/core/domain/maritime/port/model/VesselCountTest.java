package com.anchoriq.core.domain.maritime.port.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VesselCount VO 테스트")
class VesselCountTest {

    @Test
    @DisplayName("선박 수를 생성한다")
    void create() {
        VesselCount count = VesselCount.of(5, 20);
        assertEquals(5, count.anchored());
        assertEquals(20, count.moored());
        assertEquals(25, count.total());
    }

    @Test
    @DisplayName("빈 선박 수를 생성한다")
    void zero() {
        VesselCount count = VesselCount.zero();
        assertEquals(0, count.anchored());
        assertEquals(0, count.moored());
        assertEquals(0, count.total());
    }

    @Test
    @DisplayName("정박 비율을 계산한다")
    void anchoredRatio() {
        assertEquals(0.5, VesselCount.of(5, 5).anchoredRatio(), 0.01);
        assertEquals(0.75, VesselCount.of(15, 5).anchoredRatio(), 0.01);
        assertEquals(0.0, VesselCount.zero().anchoredRatio(), 0.01);
    }

    @Test
    @DisplayName("음수 정박 수이면 예외를 던진다")
    void negativeAnchored() {
        assertThrows(IllegalArgumentException.class, () -> VesselCount.of(-1, 5));
    }

    @Test
    @DisplayName("음수 접안 수이면 예외를 던진다")
    void negativeMoored() {
        assertThrows(IllegalArgumentException.class, () -> VesselCount.of(5, -1));
    }
}

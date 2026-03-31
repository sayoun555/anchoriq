package com.anchoriq.core.domain.maritime.port.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CongestionReport VO 테스트")
class CongestionReportTest {

    @Nested
    @DisplayName("생성")
    class Creation {

        @Test
        @DisplayName("정상적인 혼잡도 보고서를 생성한다")
        void createWithBaseline() {
            CongestionReport report = CongestionReport.of(
                    Locode.of("KRPUS"), 5, 20, 78.0, 1.35);

            assertEquals("KRPUS", report.getLocode().value());
            assertEquals(5, report.getAnchoredVessels());
            assertEquals(20, report.getMooredVessels());
            assertEquals(78.0, report.getCongestionIndexValue());
            assertEquals(1.35, report.getBaselineRatioValue());
            assertNotNull(report.getCalculatedAt());
        }

        @Test
        @DisplayName("기준선 없이 혼잡도 보고서를 생성한다")
        void createWithoutBaseline() {
            CongestionReport report = CongestionReport.withoutBaseline(
                    Locode.of("KRPUS"), 3, 10, 60.0);

            assertEquals(-1.0, report.getBaselineRatioValue());
            assertFalse(report.getBaselineRatio().hasBaseline());
        }

        @Test
        @DisplayName("locode가 null이면 예외를 던진다")
        void nullLocode() {
            assertThrows(NullPointerException.class, () ->
                    CongestionReport.of(null, 0, 0, 0.0, 1.0));
        }
    }

    @Nested
    @DisplayName("비즈니스 로직")
    class BusinessLogic {

        @Test
        @DisplayName("기준선 초과 여부를 판단한다")
        void isAboveBaseline() {
            CongestionReport above = CongestionReport.of(
                    Locode.of("KRPUS"), 5, 20, 78.0, 1.35);
            CongestionReport below = CongestionReport.of(
                    Locode.of("SGSIN"), 2, 10, 36.0, 0.8);

            assertTrue(above.isAboveBaseline());
            assertFalse(below.isAboveBaseline());
        }

        @Test
        @DisplayName("위험 수준 혼잡도를 판단한다")
        void isCritical() {
            CongestionReport critical = CongestionReport.of(
                    Locode.of("KRPUS"), 8, 10, 95.0, 2.0);
            CongestionReport normal = CongestionReport.of(
                    Locode.of("SGSIN"), 1, 5, 25.0, 0.5);

            assertTrue(critical.isCritical());
            assertFalse(normal.isCritical());
        }

        @Test
        @DisplayName("심각도를 올바르게 분류한다")
        void severity() {
            assertEquals(CongestionSeverity.LOW,
                    CongestionReport.of(Locode.of("KRPUS"), 1, 2, 20.0, 0.5).severity());
            assertEquals(CongestionSeverity.MODERATE,
                    CongestionReport.of(Locode.of("KRPUS"), 3, 5, 50.0, 1.0).severity());
            assertEquals(CongestionSeverity.HIGH,
                    CongestionReport.of(Locode.of("KRPUS"), 5, 10, 70.0, 1.2).severity());
            assertEquals(CongestionSeverity.CRITICAL,
                    CongestionReport.of(Locode.of("KRPUS"), 8, 15, 90.0, 2.0).severity());
        }

        @Test
        @DisplayName("정박 대기 비율이 높은 경우를 판단한다")
        void hasHighAnchorageWait() {
            CongestionReport highAnchorage = CongestionReport.of(
                    Locode.of("KRPUS"), 15, 5, 70.0, 1.5);
            CongestionReport lowAnchorage = CongestionReport.of(
                    Locode.of("SGSIN"), 2, 10, 36.0, 0.8);
            CongestionReport empty = CongestionReport.of(
                    Locode.of("NLRTM"), 0, 0, 0.0, 0.0);

            assertTrue(highAnchorage.hasHighAnchorageWait());
            assertFalse(lowAnchorage.hasHighAnchorageWait());
            assertFalse(empty.hasHighAnchorageWait());
        }
    }
}

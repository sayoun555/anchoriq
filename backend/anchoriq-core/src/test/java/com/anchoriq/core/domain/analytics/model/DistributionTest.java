package com.anchoriq.core.domain.analytics.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DistributionTest {

    @Nested
    @DisplayName("생성")
    class Creation {

        @Test
        @DisplayName("정상 생성")
        void shouldCreateDistribution() {
            Distribution dist = Distribution.of("TANKER", 50, 25.0);

            assertEquals("TANKER", dist.label());
            assertEquals(50, dist.count());
            assertEquals(25.0, dist.percentage());
        }

        @Test
        @DisplayName("전체 건수 기반 비율 자동 계산")
        void shouldCalculatePercentageFromTotal() {
            Distribution dist = Distribution.ofTotal("KR", 30, 100);

            assertEquals("KR", dist.label());
            assertEquals(30, dist.count());
            assertEquals(30.0, dist.percentage());
        }

        @Test
        @DisplayName("전체 건수가 0이면 비율은 0")
        void shouldReturnZeroPercentageWhenTotalIsZero() {
            Distribution dist = Distribution.ofTotal("KR", 0, 0);
            assertEquals(0.0, dist.percentage());
        }

        @Test
        @DisplayName("null 라벨 거부")
        void shouldRejectNullLabel() {
            assertThrows(NullPointerException.class,
                    () -> Distribution.of(null, 10, 5.0));
        }

        @Test
        @DisplayName("음수 건수 거부")
        void shouldRejectNegativeCount() {
            assertThrows(IllegalArgumentException.class,
                    () -> Distribution.of("test", -1, 5.0));
        }

        @Test
        @DisplayName("범위 초과 비율 거부")
        void shouldRejectOutOfRangePercentage() {
            assertThrows(IllegalArgumentException.class,
                    () -> Distribution.of("test", 10, 101.0));
        }
    }

    @Nested
    @DisplayName("비즈니스 로직")
    class BusinessLogic {

        @Test
        @DisplayName("50% 이상이면 dominant")
        void shouldBeDominantWhenPercentageIsHigh() {
            Distribution dist = Distribution.of("TANKER", 60, 60.0);
            assertTrue(dist.isDominant());
        }

        @Test
        @DisplayName("10% 이상이면 significant")
        void shouldBeSignificantWhenPercentageAboveTen() {
            Distribution dist = Distribution.of("TANKER", 15, 15.0);
            assertTrue(dist.isSignificant());
        }

        @Test
        @DisplayName("10% 미만이면 significant 아님")
        void shouldNotBeSignificantWhenPercentageBelowTen() {
            Distribution dist = Distribution.of("OTHER", 5, 5.0);
            assertFalse(dist.isSignificant());
        }
    }

    @Nested
    @DisplayName("동등성")
    class Equality {

        @Test
        @DisplayName("같은 라벨과 건수면 같은 객체")
        void shouldBeEqualWhenSameLabelAndCount() {
            Distribution d1 = Distribution.of("TANKER", 50, 25.0);
            Distribution d2 = Distribution.of("TANKER", 50, 30.0);
            assertEquals(d1, d2);
        }

        @Test
        @DisplayName("다른 라벨이면 다른 객체")
        void shouldNotBeEqualWhenDifferentLabel() {
            Distribution d1 = Distribution.of("TANKER", 50, 25.0);
            Distribution d2 = Distribution.of("BULK", 50, 25.0);
            assertNotEquals(d1, d2);
        }
    }
}

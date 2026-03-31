package com.anchoriq.core.domain.analytics.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TrendPointTest {

    @Nested
    @DisplayName("생성")
    class Creation {

        @Test
        @DisplayName("정상 생성")
        void shouldCreateTrendPoint() {
            LocalDate date = LocalDate.of(2026, 3, 27);
            TrendPoint point = TrendPoint.of(date, 65.5);

            assertEquals(date, point.date());
            assertEquals(65.5, point.value());
        }

        @Test
        @DisplayName("null 날짜 거부")
        void shouldRejectNullDate() {
            assertThrows(NullPointerException.class,
                    () -> TrendPoint.of(null, 10.0));
        }
    }

    @Nested
    @DisplayName("비즈니스 로직")
    class BusinessLogic {

        @Test
        @DisplayName("이전 대비 변화율 계산")
        void shouldCalculateChangeRate() {
            TrendPoint previous = TrendPoint.of(LocalDate.of(2026, 3, 26), 100.0);
            TrendPoint current = TrendPoint.of(LocalDate.of(2026, 3, 27), 120.0);

            assertEquals(20.0, current.changeRateFrom(previous), 0.01);
        }

        @Test
        @DisplayName("이전 값이 0이면 변화율 0")
        void shouldReturnZeroWhenPreviousIsZero() {
            TrendPoint previous = TrendPoint.of(LocalDate.of(2026, 3, 26), 0.0);
            TrendPoint current = TrendPoint.of(LocalDate.of(2026, 3, 27), 50.0);

            assertEquals(0.0, current.changeRateFrom(previous));
        }

        @Test
        @DisplayName("null 이전 값이면 변화율 0")
        void shouldReturnZeroWhenPreviousIsNull() {
            TrendPoint current = TrendPoint.of(LocalDate.of(2026, 3, 27), 50.0);
            assertEquals(0.0, current.changeRateFrom(null));
        }

        @Test
        @DisplayName("값 비교")
        void shouldCompareValues() {
            TrendPoint high = TrendPoint.of(LocalDate.of(2026, 3, 27), 100.0);
            TrendPoint low = TrendPoint.of(LocalDate.of(2026, 3, 26), 50.0);

            assertTrue(high.isHigherThan(low));
            assertFalse(low.isHigherThan(high));
        }
    }

    @Nested
    @DisplayName("동등성")
    class Equality {

        @Test
        @DisplayName("같은 날짜와 값이면 같은 객체")
        void shouldBeEqual() {
            LocalDate date = LocalDate.of(2026, 3, 27);
            TrendPoint p1 = TrendPoint.of(date, 65.5);
            TrendPoint p2 = TrendPoint.of(date, 65.5);
            assertEquals(p1, p2);
        }

        @Test
        @DisplayName("다른 값이면 다른 객체")
        void shouldNotBeEqual() {
            LocalDate date = LocalDate.of(2026, 3, 27);
            TrendPoint p1 = TrendPoint.of(date, 65.5);
            TrendPoint p2 = TrendPoint.of(date, 70.0);
            assertNotEquals(p1, p2);
        }
    }
}

package com.anchoriq.core.domain.analytics.model;

import com.anchoriq.core.domain.operation.collector.model.CollectorName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class CollectorStatisticsTest {

    @Nested
    @DisplayName("생성")
    class Creation {

        @Test
        @DisplayName("정상 생성 및 에러율 자동 계산")
        void shouldCreateWithAutoCalculatedErrorRate() {
            CollectorStatistics stats = CollectorStatistics.of(
                    CollectorName.AIS, 100, 3, null, null, 5000);

            assertEquals(CollectorName.AIS, stats.name());
            assertEquals(100, stats.todayProcessed());
            assertEquals(3, stats.todayErrors());
            assertEquals(3.0, stats.errorRate());
            assertEquals(5000, stats.totalProcessed());
        }

        @Test
        @DisplayName("처리 건수 0이면 에러율 0")
        void shouldReturnZeroErrorRateWhenNoProcessed() {
            CollectorStatistics stats = CollectorStatistics.of(
                    CollectorName.WEATHER, 0, 0, null, null, 0);

            assertEquals(0.0, stats.errorRate());
        }

        @Test
        @DisplayName("null 이름 거부")
        void shouldRejectNullName() {
            assertThrows(NullPointerException.class,
                    () -> CollectorStatistics.of(null, 10, 0, null, null, 10));
        }
    }

    @Nested
    @DisplayName("건강 상태 판단")
    class HealthCheck {

        @Test
        @DisplayName("에러율 5% 미만이면 건강")
        void shouldBeHealthyWhenErrorRateBelow5() {
            CollectorStatistics stats = CollectorStatistics.of(
                    CollectorName.AIS, 100, 4, null, null, 1000);
            assertTrue(stats.isHealthy());
        }

        @Test
        @DisplayName("에러율 5% 이상이면 비건강")
        void shouldNotBeHealthyWhenErrorRate5OrAbove() {
            CollectorStatistics stats = CollectorStatistics.of(
                    CollectorName.AIS, 100, 5, null, null, 1000);
            assertFalse(stats.isHealthy());
        }

        @Test
        @DisplayName("최근 에러 감지 - 1시간 이내 에러")
        void shouldDetectRecentError() {
            Instant recentError = Instant.now().minusSeconds(1800); // 30분 전
            CollectorStatistics stats = CollectorStatistics.of(
                    CollectorName.NEWS, 50, 2, null, recentError, 500);

            assertTrue(stats.hasRecentError());
        }

        @Test
        @DisplayName("최근 에러 없음 - 에러 시간이 null")
        void shouldNotHaveRecentErrorWhenNull() {
            CollectorStatistics stats = CollectorStatistics.of(
                    CollectorName.NEWS, 50, 0, null, null, 500);

            assertFalse(stats.hasRecentError());
        }

        @Test
        @DisplayName("오늘 처리 없으면 idle")
        void shouldBeIdleWhenNoProcessedToday() {
            CollectorStatistics stats = CollectorStatistics.of(
                    CollectorName.SANCTION, 0, 0, null, null, 100);
            assertTrue(stats.isIdle());
        }

        @Test
        @DisplayName("오늘 처리 있으면 idle 아님")
        void shouldNotBeIdleWhenProcessedToday() {
            CollectorStatistics stats = CollectorStatistics.of(
                    CollectorName.SANCTION, 10, 0, null, null, 100);
            assertFalse(stats.isIdle());
        }
    }
}

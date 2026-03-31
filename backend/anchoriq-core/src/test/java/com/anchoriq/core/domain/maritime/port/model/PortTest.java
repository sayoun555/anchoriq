package com.anchoriq.core.domain.maritime.port.model;

import com.anchoriq.core.common.vo.Coordinate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("Port 엔티티 테스트")
class PortTest {

    private Port createPort(String locode, String name, String country,
                            double lat, double lon, double congestion) {
        Port port = Port.builder()
                .locode(Locode.of(locode))
                .name(name)
                .country(country)
                .coordinate(Coordinate.of(lat, lon))
                .congestionLevel(CongestionLevel.of(congestion))
                .build();
        return port;
    }

    @Nested
    @DisplayName("혼잡도 판단 테스트")
    class CongestionTest {

        @Test
        @DisplayName("혼잡도 70 이상이면 혼잡 상태이다")
        void should_returnTrue_when_congestionIsHigh() {
            Port port = createPort("KRPUS", "Busan", "KR", 35.1, 129.0, 75.0);

            assertThat(port.isCongested()).isTrue();
        }

        @Test
        @DisplayName("혼잡도 69이면 혼잡 상태가 아니다")
        void should_returnFalse_when_congestionBelowThreshold() {
            Port port = createPort("KRPUS", "Busan", "KR", 35.1, 129.0, 69.0);

            assertThat(port.isCongested()).isFalse();
        }

        @Test
        @DisplayName("혼잡도 90 이상이면 위기적 혼잡 상태이다")
        void should_returnTrue_when_congestionIsCritical() {
            Port port = createPort("KRPUS", "Busan", "KR", 35.1, 129.0, 95.0);

            assertThat(port.isCriticalCongestion()).isTrue();
        }

        @Test
        @DisplayName("혼잡도 89이면 위기적 혼잡 상태가 아니다")
        void should_returnFalse_when_congestionBelowCritical() {
            Port port = createPort("KRPUS", "Busan", "KR", 35.1, 129.0, 89.0);

            assertThat(port.isCriticalCongestion()).isFalse();
        }
    }

    @Nested
    @DisplayName("예상 대기 시간 계산 테스트")
    class WaitTimeTest {

        @Test
        @DisplayName("혼잡도 30 미만이면 대기 시간 0시간이다")
        void should_returnZero_when_congestionBelow30() {
            Port port = createPort("KRPUS", "Busan", "KR", 35.1, 129.0, 20.0);

            assertThat(port.calculateExpectedWaitTime()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("혼잡도 30이면 대기 시간 0시간이다")
        void should_returnZero_when_congestionExactly30() {
            Port port = createPort("KRPUS", "Busan", "KR", 35.1, 129.0, 30.0);

            assertThat(port.calculateExpectedWaitTime()).isCloseTo(0.0, within(0.01));
        }

        @Test
        @DisplayName("혼잡도 45이면 대기 시간 약 2시간이다")
        void should_returnAboutTwoHours_when_congestion45() {
            Port port = createPort("KRPUS", "Busan", "KR", 35.1, 129.0, 45.0);

            // (45 - 30) / 30 * 4 = 2.0
            assertThat(port.calculateExpectedWaitTime()).isCloseTo(2.0, within(0.01));
        }

        @Test
        @DisplayName("혼잡도 60이면 대기 시간 4시간이다")
        void should_returnFourHours_when_congestion60() {
            Port port = createPort("KRPUS", "Busan", "KR", 35.1, 129.0, 60.0);

            // 4.0 + (60-60)/20 * 8 = 4.0
            assertThat(port.calculateExpectedWaitTime()).isCloseTo(4.0, within(0.01));
        }

        @Test
        @DisplayName("혼잡도 80이면 대기 시간 12시간이다")
        void should_returnTwelveHours_when_congestion80() {
            Port port = createPort("KRPUS", "Busan", "KR", 35.1, 129.0, 80.0);

            // 12.0 + (80-80)/20 * 36 = 12.0
            assertThat(port.calculateExpectedWaitTime()).isCloseTo(12.0, within(0.01));
        }

        @Test
        @DisplayName("혼잡도 100이면 대기 시간 48시간이다")
        void should_return48Hours_when_congestion100() {
            Port port = createPort("KRPUS", "Busan", "KR", 35.1, 129.0, 100.0);

            // 12.0 + (100-80)/20 * 36 = 12 + 36 = 48
            assertThat(port.calculateExpectedWaitTime()).isCloseTo(48.0, within(0.01));
        }
    }

    @Nested
    @DisplayName("Haversine 거리 계산 테스트")
    class DistanceTest {

        @Test
        @DisplayName("부산항에서 싱가포르항까지 거리를 계산할 수 있다 (약 4600km)")
        void should_calculateDistance_when_busanToSingapore() {
            // Given
            Port busan = createPort("KRPUS", "Busan", "KR", 35.1, 129.0, 50.0);
            Port singapore = createPort("SGSIN", "Singapore", "SG", 1.29, 103.85, 60.0);

            // When
            double distance = busan.distanceTo(singapore);

            // Then
            assertThat(distance).isCloseTo(4600.0, within(200.0));
        }

        @Test
        @DisplayName("동일한 항만 간 거리는 0이다")
        void should_returnZero_when_samePort() {
            Port port = createPort("KRPUS", "Busan", "KR", 35.1, 129.0, 50.0);

            assertThat(port.distanceTo(port)).isEqualTo(0.0);
        }
    }

    @Test
    @DisplayName("선박 입항 시 vesselCount가 증가하고 혼잡도가 올라간다")
    void should_increaseCountAndCongestion_when_vesselAccepted() {
        Port port = createPort("KRPUS", "Busan", "KR", 35.1, 129.0, 50.0);
        int initialCount = port.getVesselCount();

        port.acceptVessel();

        assertThat(port.getVesselCount()).isEqualTo(initialCount + 1);
        assertThat(port.getCongestionValue()).isGreaterThan(50.0);
    }

    @Test
    @DisplayName("선박 출항 시 vesselCount가 감소하고 혼잡도가 내려간다")
    void should_decreaseCountAndCongestion_when_vesselReleased() {
        Port port = createPort("KRPUS", "Busan", "KR", 35.1, 129.0, 50.0);
        port.acceptVessel();

        port.releaseVessel();

        assertThat(port.getCongestionValue()).isCloseTo(50.0, within(0.01));
    }

    @Test
    @DisplayName("동일한 LOCODE를 가진 항만은 동등하다")
    void should_beEqual_when_sameLocode() {
        Port port1 = createPort("KRPUS", "Busan", "KR", 35.1, 129.0, 50.0);
        Port port2 = createPort("KRPUS", "Busan Port", "KR", 35.2, 129.1, 30.0);

        assertThat(port1).isEqualTo(port2);
    }
}

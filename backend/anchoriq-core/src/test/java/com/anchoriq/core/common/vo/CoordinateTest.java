package com.anchoriq.core.common.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

@DisplayName("Coordinate Value Object 테스트")
class CoordinateTest {

    @Test
    @DisplayName("유효한 위도/경도로 좌표를 생성할 수 있다")
    void should_createCoordinate_when_validLatLon() {
        Coordinate coord = Coordinate.of(35.1796, 129.0756);

        assertThat(coord.latitude()).isEqualTo(35.1796);
        assertThat(coord.longitude()).isEqualTo(129.0756);
    }

    @Test
    @DisplayName("경계값(위도 -90, 90)으로 생성할 수 있다")
    void should_createCoordinate_when_boundaryLatitude() {
        Coordinate south = Coordinate.of(-90.0, 0.0);
        Coordinate north = Coordinate.of(90.0, 0.0);

        assertThat(south.latitude()).isEqualTo(-90.0);
        assertThat(north.latitude()).isEqualTo(90.0);
    }

    @Test
    @DisplayName("경계값(경도 -180, 180)으로 생성할 수 있다")
    void should_createCoordinate_when_boundaryLongitude() {
        Coordinate west = Coordinate.of(0.0, -180.0);
        Coordinate east = Coordinate.of(0.0, 180.0);

        assertThat(west.longitude()).isEqualTo(-180.0);
        assertThat(east.longitude()).isEqualTo(180.0);
    }

    @Test
    @DisplayName("위도가 -90 미만이면 IllegalArgumentException을 던진다")
    void should_throwException_when_latitudeBelowMinimum() {
        assertThatThrownBy(() -> Coordinate.of(-90.1, 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Latitude must be between -90 and 90");
    }

    @Test
    @DisplayName("위도가 90 초과이면 IllegalArgumentException을 던진다")
    void should_throwException_when_latitudeAboveMaximum() {
        assertThatThrownBy(() -> Coordinate.of(90.1, 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Latitude must be between -90 and 90");
    }

    @Test
    @DisplayName("경도가 -180 미만이면 IllegalArgumentException을 던진다")
    void should_throwException_when_longitudeBelowMinimum() {
        assertThatThrownBy(() -> Coordinate.of(0.0, -180.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Longitude must be between -180 and 180");
    }

    @Test
    @DisplayName("경도가 180 초과이면 IllegalArgumentException을 던진다")
    void should_throwException_when_longitudeAboveMaximum() {
        assertThatThrownBy(() -> Coordinate.of(0.0, 180.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Longitude must be between -180 and 180");
    }

    @Test
    @DisplayName("Haversine 공식으로 부산-도쿄 간 거리를 계산할 수 있다 (약 880km)")
    void should_calculateDistance_when_busanToTokyo() {
        // Given
        Coordinate busan = Coordinate.of(35.1796, 129.0756);
        Coordinate tokyo = Coordinate.of(35.6762, 139.6503);

        // When
        double distanceKm = busan.distanceKmTo(tokyo);

        // Then (부산-도쿄 직선 약 880km)
        assertThat(distanceKm).isCloseTo(880.0, within(100.0));
    }

    @Test
    @DisplayName("같은 좌표 간 거리는 0이다")
    void should_returnZero_when_sameCoordinates() {
        Coordinate coord = Coordinate.of(35.1796, 129.0756);

        double distance = coord.distanceKmTo(coord);

        assertThat(distance).isEqualTo(0.0);
    }

    @Test
    @DisplayName("동일한 좌표는 동등하다")
    void should_beEqual_when_sameLatLon() {
        Coordinate coord1 = Coordinate.of(35.1796, 129.0756);
        Coordinate coord2 = Coordinate.of(35.1796, 129.0756);

        assertThat(coord1).isEqualTo(coord2);
        assertThat(coord1.hashCode()).isEqualTo(coord2.hashCode());
    }

    @Test
    @DisplayName("다른 좌표는 동등하지 않다")
    void should_notBeEqual_when_differentLatLon() {
        Coordinate coord1 = Coordinate.of(35.1796, 129.0756);
        Coordinate coord2 = Coordinate.of(37.5665, 126.9780);

        assertThat(coord1).isNotEqualTo(coord2);
    }
}

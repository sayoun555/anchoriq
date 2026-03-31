package com.anchoriq.core.common.vo;

import java.util.Objects;

/**
 * 위도/경도 좌표 Value Object.
 * 불변 객체로 좌표의 유효성을 보장한다.
 */
public class Coordinate {

    private final double latitude;
    private final double longitude;

    private Coordinate(double latitude, double longitude) {
        validateLatitude(latitude);
        validateLongitude(longitude);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public static Coordinate of(double latitude, double longitude) {
        return new Coordinate(latitude, longitude);
    }

    public double latitude() {
        return latitude;
    }

    public double longitude() {
        return longitude;
    }

    /**
     * 두 좌표 간 거리를 km 단위로 계산한다 (Haversine 공식).
     */
    public double distanceKmTo(Coordinate other) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(other.latitude - this.latitude);
        double dLon = Math.toRadians(other.longitude - this.longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(this.latitude)) * Math.cos(Math.toRadians(other.latitude))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    private void validateLatitude(double latitude) {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90, but was: " + latitude);
        }
    }

    private void validateLongitude(double longitude) {
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180, but was: " + longitude);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Coordinate that = (Coordinate) o;
        return Double.compare(latitude, that.latitude) == 0
                && Double.compare(longitude, that.longitude) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude);
    }

    @Override
    public String toString() {
        return String.format("Coordinate(%f, %f)", latitude, longitude);
    }
}

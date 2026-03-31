package com.anchoriq.core.domain.maritime.weather.model;

import com.anchoriq.core.common.vo.Coordinate;

import java.time.Instant;
import java.util.Objects;

/**
 * 기상 조건 엔티티.
 * 해역의 기상 상태 및 심각도 판단 로직을 보유한다.
 * 순수 POJO -- Neo4j 어노테이션 없음.
 */
public class WeatherCondition {

    private Long id;
    private WeatherType type;
    private String severity;
    private Coordinate coordinate;
    private Instant timestamp;
    private String description;

    protected WeatherCondition() {
    }

    private WeatherCondition(WeatherType type, String severity, Coordinate coordinate, String description) {
        this.type = Objects.requireNonNull(type, "Weather type must not be null");
        this.severity = Objects.requireNonNull(severity, "Severity must not be null");
        this.coordinate = Objects.requireNonNull(coordinate, "Coordinate must not be null");
        this.timestamp = Instant.now();
        this.description = description;
    }

    public static WeatherCondition create(WeatherType type, String severity,
                                          double latitude, double longitude, String description) {
        return new WeatherCondition(type, severity, Coordinate.of(latitude, longitude), description);
    }

    public boolean isSevere() {
        return "HIGH".equalsIgnoreCase(severity) || "CRITICAL".equalsIgnoreCase(severity);
    }

    public boolean isDangerous() {
        return type == WeatherType.TYPHOON || type == WeatherType.STORM;
    }

    public boolean affectsNavigation() {
        return isSevere() || isDangerous();
    }

    public static WeatherCondition reconstitute(Long id, WeatherType type, String severity,
                                                   Coordinate coordinate, Instant timestamp,
                                                   String description) {
        WeatherCondition wc = new WeatherCondition();
        wc.id = id;
        wc.type = type;
        wc.severity = severity;
        wc.coordinate = coordinate;
        wc.timestamp = timestamp;
        wc.description = description;
        return wc;
    }

    public Long getId() {
        return id;
    }

    public WeatherType getType() {
        return type;
    }

    public String getSeverity() {
        return severity;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public double getLatitude() {
        return coordinate != null ? coordinate.latitude() : 0.0;
    }

    public double getLongitude() {
        return coordinate != null ? coordinate.longitude() : 0.0;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WeatherCondition that = (WeatherCondition) o;
        return type == that.type && coordinate.equals(that.coordinate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, coordinate);
    }

    @Override
    public String toString() {
        return String.format("WeatherCondition{type=%s, severity='%s', coord=%s}",
                type, severity, coordinate);
    }
}

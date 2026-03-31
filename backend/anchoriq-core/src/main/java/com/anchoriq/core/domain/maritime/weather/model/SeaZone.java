package com.anchoriq.core.domain.maritime.weather.model;

import com.anchoriq.core.common.vo.Coordinate;

import java.util.Objects;

/**
 * 해역(Sea Zone) 엔티티.
 * EEZ, 영해 등 해양 구역을 나타낸다.
 * 순수 POJO -- Neo4j 어노테이션 없음.
 */
public class SeaZone {

    private Long id;
    private String name;
    private String type;
    private String country;
    private Coordinate coordinate;

    protected SeaZone() {
    }

    private SeaZone(String name, String type, String country, Coordinate coordinate) {
        this.name = Objects.requireNonNull(name, "SeaZone name must not be null");
        this.type = Objects.requireNonNull(type, "SeaZone type must not be null");
        this.country = country;
        this.coordinate = coordinate;
    }

    public static SeaZone create(String name, String type, String country,
                                 double latitude, double longitude) {
        return new SeaZone(name, type, country, Coordinate.of(latitude, longitude));
    }

    public boolean isEez() {
        return "EEZ".equalsIgnoreCase(type);
    }

    public boolean isTerritorial() {
        return "TERRITORIAL".equalsIgnoreCase(type);
    }

    public static SeaZone reconstitute(Long id, String name, String type, String country,
                                        Coordinate coordinate) {
        SeaZone sz = new SeaZone(name, type, country, coordinate);
        sz.id = id;
        return sz;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getCountry() {
        return country;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SeaZone seaZone = (SeaZone) o;
        return name.equals(seaZone.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return String.format("SeaZone{name='%s', type='%s'}", name, type);
    }
}

package com.anchoriq.core.domain.maritime.port.model;

import com.anchoriq.core.common.vo.Coordinate;
import com.anchoriq.core.domain.common.model.AggregateRoot;

import java.time.Instant;
import java.util.Objects;

/**
 * 항만 엔티티 (Aggregate Root).
 * 혼잡도 관리, 거리 계산, 대기 시간 추정 비즈니스 로직을 보유한다.
 * 순수 POJO -- Neo4j 어노테이션 없음. 매핑은 infrastructure 레이어에서 수행.
 */
public class Port extends AggregateRoot {

    private Long id;
    private Locode locode;
    private String name;
    private String country;
    private Coordinate coordinate;
    private CongestionLevel congestionLevel;
    private int vesselCount;
    private Instant lastUpdated;

    protected Port() {
    }

    private Port(Builder builder) {
        this.locode = Objects.requireNonNull(builder.locode, "Locode must not be null");
        this.name = Objects.requireNonNull(builder.name, "Name must not be null");
        this.country = Objects.requireNonNull(builder.country, "Country must not be null");
        this.coordinate = Objects.requireNonNull(builder.coordinate, "Coordinate must not be null");
        this.congestionLevel = builder.congestionLevel != null ? builder.congestionLevel : CongestionLevel.zero();
        this.vesselCount = builder.vesselCount;
        this.lastUpdated = Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Port create(String locode, String name, String country,
                              double latitude, double longitude) {
        return builder()
                .locode(Locode.of(locode))
                .name(name)
                .country(country)
                .coordinate(Coordinate.of(latitude, longitude))
                .build();
    }

    // --- 비즈니스 로직 ---

    public void updateCongestion(double newLevel) {
        this.congestionLevel = CongestionLevel.of(newLevel);
        this.lastUpdated = Instant.now();
    }

    /**
     * 실시간 혼잡도 보고서 결과를 반영한다.
     * AIS 기반 계산 결과로 혼잡도와 선박 수를 갱신한다.
     */
    public void updateCongestionFromReport(CongestionReport report) {
        Objects.requireNonNull(report, "CongestionReport must not be null");
        this.congestionLevel = report.getCongestionIndex();
        this.vesselCount = report.getVesselCount().total();
        this.lastUpdated = Instant.now();
    }

    public boolean isCongested() {
        return congestionLevel.isHigh();
    }

    public boolean isCriticalCongestion() {
        return congestionLevel.isCritical();
    }

    /**
     * 혼잡도 기반 예상 대기 시간을 시간 단위로 계산한다.
     * 혼잡도 0~30: 0시간, 30~60: 최대 4시간, 60~80: 최대 12시간, 80~100: 최대 48시간.
     */
    public double calculateExpectedWaitTime() {
        double level = congestionLevel.value();
        if (level < 30.0) {
            return 0.0;
        } else if (level < 60.0) {
            return (level - 30.0) / 30.0 * 4.0;
        } else if (level < 80.0) {
            return 4.0 + (level - 60.0) / 20.0 * 8.0;
        } else {
            return 12.0 + (level - 80.0) / 20.0 * 36.0;
        }
    }

    /**
     * 선박 입항을 처리한다. 혼잡도를 갱신한다.
     */
    public void acceptVessel() {
        this.vesselCount++;
        double newCongestion = Math.min(100.0, congestionLevel.value() + 2.0);
        this.congestionLevel = CongestionLevel.of(newCongestion);
        this.lastUpdated = Instant.now();
    }

    /**
     * 선박 출항을 처리한다. 혼잡도를 갱신한다.
     */
    public void releaseVessel() {
        if (this.vesselCount > 0) {
            this.vesselCount--;
        }
        double newCongestion = Math.max(0.0, congestionLevel.value() - 2.0);
        this.congestionLevel = CongestionLevel.of(newCongestion);
        this.lastUpdated = Instant.now();
    }

    /**
     * 다른 항만까지의 거리를 km 단위로 계산한다 (Haversine).
     */
    public double distanceTo(Port other) {
        return this.coordinate.distanceKmTo(other.coordinate);
    }

    // --- 재구성용 ---

    public static Port reconstitute(Long id, Locode locode, String name, String country,
                                     Coordinate coordinate, CongestionLevel congestionLevel,
                                     int vesselCount, Instant lastUpdated) {
        Port port = new Port();
        port.id = id;
        port.locode = locode;
        port.name = name;
        port.country = country;
        port.coordinate = coordinate;
        port.congestionLevel = congestionLevel;
        port.vesselCount = vesselCount;
        port.lastUpdated = lastUpdated;
        return port;
    }

    // --- Getters ---

    public Long getId() {
        return id;
    }

    public Locode getLocode() {
        return locode;
    }

    public String getLocodeValue() {
        return locode != null ? locode.value() : null;
    }

    public String getName() {
        return name;
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

    public CongestionLevel getCongestionLevel() {
        return congestionLevel;
    }

    public double getCongestionValue() {
        return congestionLevel != null ? congestionLevel.value() : 0.0;
    }

    public int getVesselCount() {
        return vesselCount;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Port port = (Port) o;
        return locode.equals(port.locode);
    }

    @Override
    public int hashCode() {
        return locode.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Port{locode='%s', name='%s', congestion=%s}", locode, name, congestionLevel);
    }

    // --- Builder ---

    public static class Builder {
        private Locode locode;
        private String name;
        private String country;
        private Coordinate coordinate;
        private CongestionLevel congestionLevel;
        private int vesselCount;

        private Builder() {
        }

        public Builder locode(Locode locode) {
            this.locode = locode;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder country(String country) {
            this.country = country;
            return this;
        }

        public Builder coordinate(Coordinate coordinate) {
            this.coordinate = coordinate;
            return this;
        }

        public Builder congestionLevel(CongestionLevel congestionLevel) {
            this.congestionLevel = congestionLevel;
            return this;
        }

        public Builder vesselCount(int vesselCount) {
            this.vesselCount = vesselCount;
            return this;
        }

        public Port build() {
            return new Port(this);
        }
    }
}

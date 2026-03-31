package com.anchoriq.core.domain.maritime.route.model;

import com.anchoriq.core.common.vo.Coordinate;

import java.util.Objects;

/**
 * 초크포인트 엔티티.
 * 6개: 호르무즈, 말라카, 바브엘만데브, 수에즈, 대만해협, 파나마.
 * 순수 POJO -- Neo4j 어노테이션 없음.
 */
public class Chokepoint {

    private Long id;
    private String name;
    private String displayName;
    private Coordinate coordinate;
    private String riskLevel;
    private String description;
    private int transitVolume;

    protected Chokepoint() {
    }

    private Chokepoint(Builder builder) {
        this.name = Objects.requireNonNull(builder.name, "Chokepoint name must not be null");
        this.displayName = builder.displayName;
        this.coordinate = builder.coordinate;
        this.riskLevel = Objects.requireNonNull(builder.riskLevel, "Risk level must not be null");
        this.description = builder.description;
        this.transitVolume = builder.transitVolume;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Chokepoint create(String name, String displayName, double latitude,
                                    double longitude, String riskLevel, String description) {
        return builder()
                .name(name)
                .displayName(displayName)
                .coordinate(Coordinate.of(latitude, longitude))
                .riskLevel(riskLevel)
                .description(description)
                .build();
    }

    // --- 비즈니스 로직 ---

    public boolean isHighRisk() {
        return "HIGH".equalsIgnoreCase(riskLevel);
    }

    public boolean isMediumOrHighRisk() {
        return "MEDIUM".equalsIgnoreCase(riskLevel) || "HIGH".equalsIgnoreCase(riskLevel);
    }

    public void updateTransitVolume(int volume) {
        if (volume < 0) {
            throw new IllegalArgumentException("Transit volume must not be negative");
        }
        this.transitVolume = volume;
    }

    // --- 재구성용 ---

    public static Chokepoint reconstitute(Long id, String name, String displayName,
                                           Coordinate coordinate, String riskLevel,
                                           String description, int transitVolume) {
        Chokepoint cp = new Chokepoint();
        cp.id = id;
        cp.name = name;
        cp.displayName = displayName;
        cp.coordinate = coordinate;
        cp.riskLevel = riskLevel;
        cp.description = description;
        cp.transitVolume = transitVolume;
        return cp;
    }

    // --- Getters ---

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
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

    public String getRiskLevel() {
        return riskLevel;
    }

    public String getDescription() {
        return description;
    }

    public int getTransitVolume() {
        return transitVolume;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Chokepoint that = (Chokepoint) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Chokepoint{name='%s', riskLevel='%s'}", name, riskLevel);
    }

    // --- Builder ---

    public static class Builder {
        private String name;
        private String displayName;
        private Coordinate coordinate;
        private String riskLevel;
        private String description;
        private int transitVolume;

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder coordinate(Coordinate coordinate) {
            this.coordinate = coordinate;
            return this;
        }

        public Builder riskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder transitVolume(int transitVolume) {
            this.transitVolume = transitVolume;
            return this;
        }

        public Chokepoint build() {
            return new Chokepoint(this);
        }
    }
}

package com.anchoriq.core.domain.maritime.route.model;

import com.anchoriq.core.domain.common.model.AggregateRoot;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 항로 엔티티 (Aggregate Root).
 * 초크포인트 경유 여부, 리스크 평가, 대체 경로 탐색 비즈니스 로직을 보유한다.
 * 순수 POJO -- Neo4j 어노테이션 없음.
 */
public class Route extends AggregateRoot {

    private Long id;
    private String name;
    private String displayName;
    private int distanceNm;
    private String unit;
    private Chokepoints chokepoints;

    protected Route() {
        this.chokepoints = Chokepoints.empty();
    }

    private Route(Builder builder) {
        this.name = Objects.requireNonNull(builder.name, "Route name must not be null");
        this.displayName = builder.displayName;
        this.distanceNm = builder.distanceNm;
        this.unit = "nm";
        this.chokepoints = builder.chokepoints != null ? builder.chokepoints : Chokepoints.empty();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Route create(String name, String displayName, int distanceNm) {
        return builder().name(name).displayName(displayName).distanceNm(distanceNm).build();
    }

    // --- 비즈니스 로직 (Chokepoints에 위임) ---

    public boolean passesThrough(String chokepointName) {
        return chokepoints.passesThrough(chokepointName);
    }

    public boolean isHighRisk() {
        return chokepoints.hasHighRisk();
    }

    public int countHighRiskChokepoints() {
        return chokepoints.countHighRisk();
    }

    /**
     * 항로 리스크 점수를 계산한다 (0~100).
     * 고위험 초크포인트 개수, 항로 거리, 제재국 경유 여부 종합.
     */
    public int calculateRiskScore(Set<String> sanctionedCountryCodes) {
        int score = 0;

        // 고위험 초크포인트당 +25
        score += countHighRiskChokepoints() * 25;

        // 중위험 이상 초크포인트당 +10
        score += (int) (chokepoints.countMediumOrHighRisk() * 10);

        // 장거리 항로 (5000nm 이상) +10
        if (distanceNm >= 5000) {
            score += 10;
        }

        return Math.min(score, 100);
    }

    /**
     * 차단된 초크포인트를 우회하는 대체 경로를 추천할 수 있는지 판단한다.
     * 실제 대체 경로 데이터는 도메인 서비스에서 조회한다.
     */
    public boolean requiresAlternative(Set<String> blockedChokepoints) {
        return chokepoints.containsAny(blockedChokepoints);
    }

    public void addChokepoint(Chokepoint chokepoint) {
        this.chokepoints = chokepoints.add(chokepoint);
    }

    // --- 재구성용 ---

    public static Route reconstitute(Long id, String name, String displayName,
                                      int distanceNm, Chokepoints chokepoints) {
        Route route = new Route();
        route.id = id;
        route.name = name;
        route.displayName = displayName;
        route.distanceNm = distanceNm;
        route.unit = "nm";
        route.chokepoints = chokepoints != null ? chokepoints : Chokepoints.empty();
        return route;
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

    public int getDistanceNm() {
        return distanceNm;
    }

    public String getUnit() {
        return unit;
    }

    public Chokepoints getChokepoints() {
        return chokepoints;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Route route = (Route) o;
        return name.equals(route.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Route{name='%s', distance=%d nm}", name, distanceNm);
    }

    // --- Builder ---

    public static class Builder {
        private String name;
        private String displayName;
        private int distanceNm;
        private Chokepoints chokepoints;

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

        public Builder distanceNm(int distanceNm) {
            this.distanceNm = distanceNm;
            return this;
        }

        public Builder chokepoints(List<Chokepoint> chokepoints) {
            this.chokepoints = Chokepoints.of(chokepoints);
            return this;
        }

        public Builder chokepoints(Chokepoints chokepoints) {
            this.chokepoints = chokepoints;
            return this;
        }

        public Route build() {
            return new Route(this);
        }
    }
}

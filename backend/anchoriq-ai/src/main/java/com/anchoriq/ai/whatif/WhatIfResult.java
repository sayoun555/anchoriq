package com.anchoriq.ai.whatif;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * What-if 시뮬레이션 결과.
 */
public class WhatIfResult {

    private final String id;
    private final String scenario;
    private final String duration;
    private final int affectedVessels;
    private final List<String> affectedRoutes;
    private final String estimatedDelay;
    private final String additionalCost;
    private final List<Map<String, Object>> alternativeRoutes;
    private final List<String> recommendations;
    private final String aiAnalysis;
    private final Instant simulatedAt;

    private WhatIfResult(Builder builder) {
        this.id = UUID.randomUUID().toString();
        this.scenario = Objects.requireNonNull(builder.scenario);
        this.duration = builder.duration;
        this.affectedVessels = builder.affectedVessels;
        this.affectedRoutes = builder.affectedRoutes != null ? builder.affectedRoutes : List.of();
        this.estimatedDelay = builder.estimatedDelay;
        this.additionalCost = builder.additionalCost;
        this.alternativeRoutes = builder.alternativeRoutes != null ? builder.alternativeRoutes : List.of();
        this.recommendations = builder.recommendations != null ? builder.recommendations : List.of();
        this.aiAnalysis = builder.aiAnalysis;
        this.simulatedAt = Instant.now();
    }

    public static Builder builder(String scenario) {
        return new Builder(scenario);
    }

    public String getId() { return id; }
    public String getScenario() { return scenario; }
    public String getDuration() { return duration; }
    public int getAffectedVessels() { return affectedVessels; }
    public List<String> getAffectedRoutes() { return Collections.unmodifiableList(affectedRoutes); }
    public String getEstimatedDelay() { return estimatedDelay; }
    public String getAdditionalCost() { return additionalCost; }
    public List<Map<String, Object>> getAlternativeRoutes() { return Collections.unmodifiableList(alternativeRoutes); }
    public List<String> getRecommendations() { return Collections.unmodifiableList(recommendations); }
    public String getAiAnalysis() { return aiAnalysis; }
    public Instant getSimulatedAt() { return simulatedAt; }

    public static class Builder {
        private final String scenario;
        private String duration;
        private int affectedVessels;
        private List<String> affectedRoutes;
        private String estimatedDelay;
        private String additionalCost;
        private List<Map<String, Object>> alternativeRoutes;
        private List<String> recommendations;
        private String aiAnalysis;

        private Builder(String scenario) {
            this.scenario = scenario;
        }

        public Builder duration(String duration) { this.duration = duration; return this; }
        public Builder affectedVessels(int count) { this.affectedVessels = count; return this; }
        public Builder affectedRoutes(List<String> routes) { this.affectedRoutes = routes; return this; }
        public Builder estimatedDelay(String delay) { this.estimatedDelay = delay; return this; }
        public Builder additionalCost(String cost) { this.additionalCost = cost; return this; }
        public Builder alternativeRoutes(List<Map<String, Object>> routes) { this.alternativeRoutes = routes; return this; }
        public Builder recommendations(List<String> recs) { this.recommendations = recs; return this; }
        public Builder aiAnalysis(String analysis) { this.aiAnalysis = analysis; return this; }

        public WhatIfResult build() {
            return new WhatIfResult(this);
        }
    }
}

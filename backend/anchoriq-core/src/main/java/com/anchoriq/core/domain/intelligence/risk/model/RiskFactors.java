package com.anchoriq.core.domain.intelligence.risk.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * RiskType별 점수를 담는 일급 컬렉션.
 * 리스크 요인 맵에 대한 비즈니스 로직을 캡슐화한다.
 */
public class RiskFactors {

    private final Map<RiskType, Integer> values;

    public RiskFactors(Map<RiskType, Integer> factors) {
        this.values = factors != null
                ? Collections.unmodifiableMap(new HashMap<>(factors))
                : Collections.emptyMap();
    }

    public static RiskFactors empty() {
        return new RiskFactors(Collections.emptyMap());
    }

    public static RiskFactors of(Map<RiskType, Integer> factors) {
        return new RiskFactors(factors);
    }

    public int getScore(RiskType type) {
        return values.getOrDefault(type, 0);
    }

    public int calculateWeightedTotal() {
        if (values.isEmpty()) {
            return 0;
        }
        return values.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    public RiskType highestRiskType() {
        return values.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public boolean hasHighRisk() {
        return values.values().stream().anyMatch(score -> score >= 70);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public int size() {
        return values.size();
    }

    public Map<RiskType, Integer> getValues() {
        return values; // already unmodifiable
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RiskFactors that = (RiskFactors) o;
        return values.equals(that.values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public String toString() {
        return String.format("RiskFactors{count=%d}", values.size());
    }
}

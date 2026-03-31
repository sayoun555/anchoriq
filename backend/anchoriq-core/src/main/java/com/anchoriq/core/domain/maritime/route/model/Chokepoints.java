package com.anchoriq.core.domain.maritime.route.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Chokepoint 일급 컬렉션.
 * 초크포인트 목록에 대한 비즈니스 로직을 캡슐화한다.
 */
public class Chokepoints {

    private final List<Chokepoint> values;

    public Chokepoints(List<Chokepoint> chokepoints) {
        this.values = chokepoints != null
                ? Collections.unmodifiableList(new ArrayList<>(chokepoints))
                : Collections.emptyList();
    }

    public static Chokepoints empty() {
        return new Chokepoints(Collections.emptyList());
    }

    public static Chokepoints of(List<Chokepoint> chokepoints) {
        return new Chokepoints(chokepoints);
    }

    public boolean passesThrough(String chokepointName) {
        return values.stream()
                .anyMatch(cp -> cp.getName().equalsIgnoreCase(chokepointName));
    }

    public int countHighRisk() {
        return (int) values.stream().filter(Chokepoint::isHighRisk).count();
    }

    public boolean hasHighRisk() {
        return values.stream().anyMatch(Chokepoint::isHighRisk);
    }

    public long countMediumOrHighRisk() {
        return values.stream().filter(Chokepoint::isMediumOrHighRisk).count();
    }

    public boolean containsAny(java.util.Set<String> chokepointNames) {
        return values.stream()
                .anyMatch(cp -> chokepointNames.contains(cp.getName()));
    }

    public boolean contains(Chokepoint chokepoint) {
        return values.contains(chokepoint);
    }

    public Chokepoints add(Chokepoint chokepoint) {
        Objects.requireNonNull(chokepoint, "Chokepoint must not be null");
        if (values.contains(chokepoint)) {
            return this;
        }
        List<Chokepoint> newValues = new ArrayList<>(values);
        newValues.add(chokepoint);
        return new Chokepoints(newValues);
    }

    public int size() {
        return values.size();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public List<Chokepoint> getValues() {
        return values; // already unmodifiable
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Chokepoints that = (Chokepoints) o;
        return values.equals(that.values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Chokepoints{count=%d}", values.size());
    }
}

package com.anchoriq.core.domain.account.subscription.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Feature 일급 컬렉션.
 * 플랜이 지원하는 기능 목록에 대한 비즈니스 로직을 캡슐화한다.
 */
public class Features {

    private final Set<Feature> values;

    public Features(Set<Feature> features) {
        this.values = features != null
                ? Collections.unmodifiableSet(new HashSet<>(features))
                : Collections.emptySet();
    }

    public static Features of(Feature... features) {
        return new Features(Set.of(features));
    }

    public static Features of(Set<Feature> features) {
        return new Features(features);
    }

    public boolean supports(Feature feature) {
        return values.contains(feature);
    }

    public boolean supportsAll(Set<Feature> required) {
        return values.containsAll(required);
    }

    public int count() {
        return values.size();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public Set<Feature> getValues() {
        return values; // already unmodifiable
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Features that = (Features) o;
        return values.equals(that.values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Features{count=%d}", values.size());
    }
}

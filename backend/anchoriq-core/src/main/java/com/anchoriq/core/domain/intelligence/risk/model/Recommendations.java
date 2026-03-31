package com.anchoriq.core.domain.intelligence.risk.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 추천 액션 일급 컬렉션.
 * 리스크 평가 결과의 추천 목록에 대한 비즈니스 로직을 캡슐화한다.
 */
public class Recommendations {

    private final List<String> values;

    public Recommendations(List<String> recommendations) {
        this.values = recommendations != null
                ? Collections.unmodifiableList(new ArrayList<>(recommendations))
                : Collections.emptyList();
    }

    public static Recommendations empty() {
        return new Recommendations(Collections.emptyList());
    }

    public static Recommendations of(String... recommendations) {
        return new Recommendations(List.of(recommendations));
    }

    public static Recommendations of(List<String> recommendations) {
        return new Recommendations(recommendations);
    }

    public boolean hasAny() {
        return !values.isEmpty();
    }

    public int count() {
        return values.size();
    }

    public String first() {
        return values.isEmpty() ? null : values.get(0);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public List<String> getValues() {
        return values; // already unmodifiable
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Recommendations that = (Recommendations) o;
        return values.equals(that.values);
    }

    @Override
    public int hashCode() {
        return values.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Recommendations{count=%d}", values.size());
    }
}

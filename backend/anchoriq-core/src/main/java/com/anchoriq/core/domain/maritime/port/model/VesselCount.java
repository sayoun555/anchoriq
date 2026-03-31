package com.anchoriq.core.domain.maritime.port.model;

import java.util.Objects;

/**
 * 항만 인근 선박 수 Value Object.
 * 정박(ANCHORED)과 접안(MOORED) 선박 수를 캡슐화한다.
 */
public class VesselCount {

    private final int anchored;
    private final int moored;

    private VesselCount(int anchored, int moored) {
        if (anchored < 0) {
            throw new IllegalArgumentException("Anchored vessel count must be non-negative, but was: " + anchored);
        }
        if (moored < 0) {
            throw new IllegalArgumentException("Moored vessel count must be non-negative, but was: " + moored);
        }
        this.anchored = anchored;
        this.moored = moored;
    }

    public static VesselCount of(int anchored, int moored) {
        return new VesselCount(anchored, moored);
    }

    public static VesselCount zero() {
        return new VesselCount(0, 0);
    }

    public int anchored() {
        return anchored;
    }

    public int moored() {
        return moored;
    }

    /**
     * 전체 선박 수 (정박 + 접안).
     */
    public int total() {
        return anchored + moored;
    }

    /**
     * 정박 대기 비율을 계산한다 (0.0~1.0).
     * 정박 비율이 높을수록 항만 처리 능력 대비 대기가 많음을 의미한다.
     */
    public double anchoredRatio() {
        if (total() == 0) return 0.0;
        return (double) anchored / total();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VesselCount that = (VesselCount) o;
        return anchored == that.anchored && moored == that.moored;
    }

    @Override
    public int hashCode() {
        return Objects.hash(anchored, moored);
    }

    @Override
    public String toString() {
        return String.format("VesselCount{anchored=%d, moored=%d, total=%d}", anchored, moored, total());
    }
}

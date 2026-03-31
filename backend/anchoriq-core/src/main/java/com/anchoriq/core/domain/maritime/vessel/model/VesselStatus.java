package com.anchoriq.core.domain.maritime.vessel.model;

import java.util.Set;

/**
 * 선박 운항 상태.
 * 상태 머신 패턴: 유효한 상태 전이만 허용한다.
 *
 * 상태 전이 규칙:
 * UNKNOWN -> SAILING, MOORED, ANCHORED, DECOMMISSIONED
 * SAILING -> MOORED, ANCHORED, DECOMMISSIONED
 * MOORED -> SAILING, DECOMMISSIONED
 * ANCHORED -> SAILING, DECOMMISSIONED
 * DECOMMISSIONED -> (종료 상태, 전이 불가)
 */
public enum VesselStatus {

    SAILING(Set.of("MOORED", "ANCHORED", "DECOMMISSIONED")),
    ANCHORED(Set.of("SAILING", "DECOMMISSIONED")),
    MOORED(Set.of("SAILING", "DECOMMISSIONED")),
    NOT_UNDER_COMMAND(Set.of("SAILING", "MOORED", "ANCHORED", "DECOMMISSIONED")),
    RESTRICTED_MANEUVERABILITY(Set.of("SAILING", "MOORED", "ANCHORED", "DECOMMISSIONED")),
    AGROUND(Set.of("SAILING", "MOORED", "DECOMMISSIONED")),
    FISHING_ENGAGED(Set.of("SAILING", "MOORED", "ANCHORED", "DECOMMISSIONED")),
    UNKNOWN(Set.of("SAILING", "MOORED", "ANCHORED", "DECOMMISSIONED")),
    DECOMMISSIONED(Set.of());

    private final Set<String> allowedTransitions;

    VesselStatus(Set<String> allowedTransitions) {
        this.allowedTransitions = allowedTransitions;
    }

    /**
     * 지정된 상태로 전이 가능한지 검증한다.
     * 불가능하면 IllegalStateException을 던진다.
     */
    public void validateTransitionTo(VesselStatus target) {
        if (!allowedTransitions.contains(target.name())) {
            throw new IllegalStateException(
                    String.format("Invalid vessel status transition: %s -> %s", this.name(), target.name()));
        }
    }

    public boolean canTransitionTo(VesselStatus target) {
        return allowedTransitions.contains(target.name());
    }
}

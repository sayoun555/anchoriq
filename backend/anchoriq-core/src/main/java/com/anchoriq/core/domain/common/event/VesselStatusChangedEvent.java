package com.anchoriq.core.domain.common.event;

/**
 * 선박 상태 변경 도메인 이벤트.
 */
public class VesselStatusChangedEvent extends DomainEvent {

    private final String vesselImo;
    private final String previousStatus;
    private final String newStatus;

    public VesselStatusChangedEvent(String vesselImo, String previousStatus, String newStatus) {
        super();
        this.vesselImo = vesselImo;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
    }

    public String getVesselImo() {
        return vesselImo;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public String getNewStatus() {
        return newStatus;
    }

    @Override
    public String toString() {
        return String.format("VesselStatusChanged{imo='%s', %s -> %s}", vesselImo, previousStatus, newStatus);
    }
}

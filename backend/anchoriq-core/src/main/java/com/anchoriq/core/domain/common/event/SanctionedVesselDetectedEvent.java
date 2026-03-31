package com.anchoriq.core.domain.common.event;

import com.anchoriq.core.domain.maritime.vessel.model.Imo;

/**
 * 제재국 연관 선박 탐지 이벤트.
 * 선박 등록 시 제재국 소유 선박으로 판별된 경우 발행된다.
 */
public class SanctionedVesselDetectedEvent extends DomainEvent {

    private final String vesselImo;

    public SanctionedVesselDetectedEvent(Imo imo) {
        super();
        this.vesselImo = imo.value();
    }

    public String getVesselImo() {
        return vesselImo;
    }
}

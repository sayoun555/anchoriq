package com.anchoriq.api.dto.response.vessel;

import com.anchoriq.core.domain.maritime.vessel.model.Vessel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VesselResponse {

    private final Long id;
    private final String imo;
    private final String mmsi;
    private final String name;
    private final String flag;
    private final String type;
    private final String status;
    private final int deadweight;
    private final int buildYear;
    private final int age;
    private final String companyName;
    private final String lastUpdated;

    public static VesselResponse from(Vessel vessel) {
        return VesselResponse.builder()
                .id(vessel.getId())
                .imo(vessel.getImo() != null ? vessel.getImo().value() : null)
                .mmsi(vessel.getMmsi() != null ? vessel.getMmsi().value() : null)
                .name(vessel.getName())
                .flag(vessel.getFlag() != null ? vessel.getFlag().value() : null)
                .type(vessel.getType() != null ? vessel.getType().name() : null)
                .status(vessel.getStatus() != null ? vessel.getStatus().name() : null)
                .deadweight(vessel.getDeadweight())
                .buildYear(vessel.getBuildYear())
                .age(vessel.calculateAge())
                .companyName(vessel.getCompany() != null ? vessel.getCompany().getName() : null)
                .lastUpdated(vessel.getLastUpdated() != null ? vessel.getLastUpdated().toString() : null)
                .build();
    }
}

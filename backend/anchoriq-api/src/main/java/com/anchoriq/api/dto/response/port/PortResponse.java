package com.anchoriq.api.dto.response.port;

import com.anchoriq.core.domain.maritime.port.model.Port;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PortResponse {

    private final Long id;
    private final String locode;
    private final String name;
    private final String country;
    private final double latitude;
    private final double longitude;
    private final double congestionLevel;
    private final boolean congested;
    private final String lastUpdated;

    public static PortResponse from(Port port) {
        return PortResponse.builder()
                .id(port.getId())
                .locode(port.getLocode() != null ? port.getLocode().value() : null)
                .name(port.getName())
                .country(port.getCountry())
                .latitude(port.getLatitude())
                .longitude(port.getLongitude())
                .congestionLevel(port.getCongestionValue())
                .congested(port.isCongested())
                .lastUpdated(port.getLastUpdated() != null ? port.getLastUpdated().toString() : null)
                .build();
    }
}

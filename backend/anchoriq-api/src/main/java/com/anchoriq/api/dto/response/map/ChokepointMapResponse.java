package com.anchoriq.api.dto.response.map;

import com.anchoriq.core.domain.maritime.route.model.Chokepoint;
import lombok.Builder;
import lombok.Getter;

/**
 * 지도용 초크포인트 응답 DTO.
 */
@Getter
@Builder
public class ChokepointMapResponse {

    private final String name;
    private final String displayName;
    private final double latitude;
    private final double longitude;
    private final String riskLevel;
    private final int transitVolume;
    private final int vesselCount;

    public static ChokepointMapResponse from(Chokepoint chokepoint) {
        return ChokepointMapResponse.builder()
                .name(chokepoint.getName())
                .displayName(chokepoint.getDisplayName())
                .latitude(chokepoint.getLatitude())
                .longitude(chokepoint.getLongitude())
                .riskLevel(chokepoint.getRiskLevel())
                .transitVolume(chokepoint.getTransitVolume())
                .vesselCount(chokepoint.getTransitVolume())
                .build();
    }
}

package com.anchoriq.api.dto.response.map;

import lombok.Builder;
import lombok.Getter;

/**
 * 지도용 선박 위치 응답 DTO (경량 데이터).
 */
@Getter
@Builder
public class MapVesselResponse {

    private final String imo;
    private final String name;
    private final String type;
    private final double latitude;
    private final double longitude;
    private final String status;
    private final String riskLevel;
    private final double heading;
    private final double speed;
}

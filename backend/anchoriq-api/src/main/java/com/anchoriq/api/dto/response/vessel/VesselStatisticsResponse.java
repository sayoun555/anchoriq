package com.anchoriq.api.dto.response.vessel;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class VesselStatisticsResponse {

    private final long totalVessels;
    private final Map<String, Long> byType;
    private final Map<String, Long> byFlag;
    private final long sanctionedCount;
}

package com.anchoriq.api.dto.response.weather;

import com.anchoriq.core.domain.maritime.weather.model.WeatherCondition;
import lombok.Builder;
import lombok.Getter;

/**
 * 기상 응답 DTO.
 */
@Getter
@Builder
public class WeatherResponse {

    private final Long id;
    private final String type;
    private final String severity;
    private final double latitude;
    private final double longitude;
    private final String description;
    private final String timestamp;
    private final boolean severe;

    public static WeatherResponse from(WeatherCondition condition) {
        return WeatherResponse.builder()
                .id(condition.getId())
                .type(condition.getType() != null ? condition.getType().name() : null)
                .severity(condition.getSeverity())
                .latitude(condition.getLatitude())
                .longitude(condition.getLongitude())
                .description(condition.getDescription())
                .timestamp(condition.getTimestamp() != null ? condition.getTimestamp().toString() : null)
                .severe(condition.isSevere())
                .build();
    }
}

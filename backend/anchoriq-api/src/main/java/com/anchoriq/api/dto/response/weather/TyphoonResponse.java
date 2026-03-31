package com.anchoriq.api.dto.response.weather;

import com.anchoriq.core.domain.maritime.weather.model.WeatherCondition;
import lombok.Builder;
import lombok.Getter;

/**
 * 태풍 응답 DTO.
 */
@Getter
@Builder
public class TyphoonResponse {

    private final Long id;
    private final String severity;
    private final double latitude;
    private final double longitude;
    private final String description;
    private final String timestamp;

    public static TyphoonResponse from(WeatherCondition condition) {
        return TyphoonResponse.builder()
                .id(condition.getId())
                .severity(condition.getSeverity())
                .latitude(condition.getLatitude())
                .longitude(condition.getLongitude())
                .description(condition.getDescription())
                .timestamp(condition.getTimestamp() != null ? condition.getTimestamp().toString() : null)
                .build();
    }
}

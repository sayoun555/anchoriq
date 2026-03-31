package com.anchoriq.api.dto.response.risk;

import com.anchoriq.core.domain.intelligence.anomaly.model.AnomalyDetection;

import java.time.Instant;

public record AnomalyResponse(
        String id,
        String type,
        String vesselImo,
        String vesselName,
        String description,
        String severity,
        double latitude,
        double longitude,
        Instant detectedAt,
        boolean resolved
) {
    public static AnomalyResponse from(AnomalyDetection anomaly) {
        return new AnomalyResponse(
                anomaly.getId(),
                anomaly.getType().name(),
                anomaly.getVesselImo(),
                anomaly.getVesselName(),
                anomaly.getDescription(),
                anomaly.getSeverity(),
                anomaly.getLatitude(),
                anomaly.getLongitude(),
                anomaly.getDetectedAt(),
                anomaly.isResolved()
        );
    }
}

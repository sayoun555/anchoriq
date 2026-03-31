package com.anchoriq.core.domain.intelligence.anomaly.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 이상 탐지 기록 엔티티.
 * AIS 소실, 항로 이탈, 속도 이상, 다크 쉽 이벤트를 기록한다.
 */
public class AnomalyDetection {

    private final String id;
    private final AnomalyType type;
    private final String vesselImo;
    private final String vesselName;
    private final String description;
    private final String severity;
    private final double latitude;
    private final double longitude;
    private final Instant detectedAt;
    private boolean resolved;
    private Instant resolvedAt;

    private AnomalyDetection(String id, AnomalyType type, String vesselImo,
                              String vesselName, String description, String severity,
                              double latitude, double longitude) {
        this.id = Objects.requireNonNull(id);
        this.type = Objects.requireNonNull(type);
        this.vesselImo = Objects.requireNonNull(vesselImo);
        this.vesselName = vesselName;
        this.description = Objects.requireNonNull(description);
        this.severity = Objects.requireNonNull(severity);
        this.latitude = latitude;
        this.longitude = longitude;
        this.detectedAt = Instant.now();
        this.resolved = false;
    }

    private AnomalyDetection(String id, AnomalyType type, String vesselImo,
                              String vesselName, String description, String severity,
                              double latitude, double longitude,
                              Instant detectedAt, boolean resolved, Instant resolvedAt) {
        this.id = Objects.requireNonNull(id);
        this.type = Objects.requireNonNull(type);
        this.vesselImo = Objects.requireNonNull(vesselImo);
        this.vesselName = vesselName;
        this.description = Objects.requireNonNull(description);
        this.severity = Objects.requireNonNull(severity);
        this.latitude = latitude;
        this.longitude = longitude;
        this.detectedAt = detectedAt;
        this.resolved = resolved;
        this.resolvedAt = resolvedAt;
    }

    public static AnomalyDetection create(AnomalyType type, String vesselImo,
                                            String vesselName, String description,
                                            String severity, double latitude, double longitude) {
        return new AnomalyDetection(
                UUID.randomUUID().toString(), type, vesselImo, vesselName,
                description, severity, latitude, longitude);
    }

    /**
     * 영속화된 데이터로부터 도메인 객체를 복원한다.
     */
    public static AnomalyDetection reconstitute(String id, AnomalyType type, String vesselImo,
                                                  String vesselName, String description,
                                                  String severity, double latitude, double longitude,
                                                  Instant detectedAt, boolean resolved, Instant resolvedAt) {
        return new AnomalyDetection(id, type, vesselImo, vesselName,
                description, severity, latitude, longitude,
                detectedAt, resolved, resolvedAt);
    }

    public void resolve() {
        this.resolved = true;
        this.resolvedAt = Instant.now();
    }

    public boolean isSevere() {
        return "HIGH".equalsIgnoreCase(severity) || "CRITICAL".equalsIgnoreCase(severity);
    }

    public boolean isAisOff() {
        return type == AnomalyType.AIS_OFF;
    }

    public boolean isDarkShip() {
        return type == AnomalyType.DARK_SHIP;
    }

    public String getId() {
        return id;
    }

    public AnomalyType getType() {
        return type;
    }

    public String getVesselImo() {
        return vesselImo;
    }

    public String getVesselName() {
        return vesselName;
    }

    public String getDescription() {
        return description;
    }

    public String getSeverity() {
        return severity;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public boolean isResolved() {
        return resolved;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnomalyDetection that = (AnomalyDetection) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("AnomalyDetection{id='%s', type=%s, vessel='%s', severity='%s'}",
                id, type, vesselImo, severity);
    }
}

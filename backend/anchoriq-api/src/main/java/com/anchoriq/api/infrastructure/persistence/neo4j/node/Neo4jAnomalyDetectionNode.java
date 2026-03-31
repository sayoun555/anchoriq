package com.anchoriq.api.infrastructure.persistence.neo4j.node;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

import java.time.Instant;

/**
 * Neo4j 노드: AnomalyDetection.
 * 이상 탐지 기록을 Neo4j 그래프에 저장하기 위한 매핑 객체.
 */
@Node("AnomalyDetection")
public class Neo4jAnomalyDetectionNode {

    @Id
    @Property("anomalyId")
    private String anomalyId;

    @Property("type")
    private String type;

    @Property("vesselImo")
    private String vesselImo;

    @Property("vesselName")
    private String vesselName;

    @Property("description")
    private String description;

    @Property("severity")
    private String severity;

    @Property("lat")
    private double latitude;

    @Property("lon")
    private double longitude;

    @Property("detectedAt")
    private Instant detectedAt;

    @Property("resolved")
    private boolean resolved;

    @Property("resolvedAt")
    private Instant resolvedAt;

    public Neo4jAnomalyDetectionNode() {
    }

    public String getAnomalyId() { return anomalyId; }
    public void setAnomalyId(String anomalyId) { this.anomalyId = anomalyId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getVesselImo() { return vesselImo; }
    public void setVesselImo(String vesselImo) { this.vesselImo = vesselImo; }
    public String getVesselName() { return vesselName; }
    public void setVesselName(String vesselName) { this.vesselName = vesselName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public Instant getDetectedAt() { return detectedAt; }
    public void setDetectedAt(Instant detectedAt) { this.detectedAt = detectedAt; }
    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}

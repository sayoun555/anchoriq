package com.anchoriq.api.infrastructure.persistence.neo4j.node;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("Chokepoint")
public class Neo4jChokepointNode {

    @Id
    @GeneratedValue
    private Long id;

    @Property("name")
    private String name;

    @Property("displayName")
    private String displayName;

    @Property("lat")
    private double latitude;

    @Property("lon")
    private double longitude;

    @Property("riskLevel")
    private String riskLevel;

    @Property("description")
    private String description;

    @Property("transitVolume")
    private int transitVolume;

    public Neo4jChokepointNode() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getTransitVolume() { return transitVolume; }
    public void setTransitVolume(int transitVolume) { this.transitVolume = transitVolume; }
}

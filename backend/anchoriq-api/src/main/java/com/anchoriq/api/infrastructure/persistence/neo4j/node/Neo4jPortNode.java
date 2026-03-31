package com.anchoriq.api.infrastructure.persistence.neo4j.node;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

import java.time.Instant;

@Node("Port")
public class Neo4jPortNode {

    @Id
    @GeneratedValue
    private Long id;

    @Property("locode")
    private String locode;

    @Property("name")
    private String name;

    @Property("country")
    private String country;

    @Property("latitude")
    private double latitude;

    @Property("longitude")
    private double longitude;

    @Property("congestionLevel")
    private double congestionLevel;

    @Property("vesselCount")
    private int vesselCount;

    @Property("lastUpdated")
    private Instant lastUpdated;

    public Neo4jPortNode() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLocode() { return locode; }
    public void setLocode(String locode) { this.locode = locode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public double getCongestionLevel() { return congestionLevel; }
    public void setCongestionLevel(double congestionLevel) { this.congestionLevel = congestionLevel; }
    public int getVesselCount() { return vesselCount; }
    public void setVesselCount(int vesselCount) { this.vesselCount = vesselCount; }
    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
}

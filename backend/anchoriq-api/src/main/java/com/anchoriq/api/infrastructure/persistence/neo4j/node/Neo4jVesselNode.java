package com.anchoriq.api.infrastructure.persistence.neo4j.node;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.time.Instant;

/**
 * Neo4j 매핑 엔티티 - Vessel.
 * 도메인 Vessel 엔티티와 Neo4j 노드 간 매핑을 담당한다.
 */
@Node("Vessel")
public class Neo4jVesselNode {

    @Id
    @GeneratedValue
    private Long id;

    @Property("imo")
    private String imo;

    @Property("mmsi")
    private String mmsi;

    @Property("name")
    private String name;

    @Property("flag")
    private String flag;

    @Property("type")
    private String type;

    @Property("status")
    private String status;

    @Property("deadweight")
    private int deadweight;

    @Property("buildYear")
    private int buildYear;

    @Property("riskScore")
    private int riskScore;

    @Property("lastUpdated")
    private Instant lastUpdated;

    @Relationship(type = "OWNED_BY", direction = Relationship.Direction.OUTGOING)
    private Neo4jCompanyNode company;

    public Neo4jVesselNode() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getImo() { return imo; }
    public void setImo(String imo) { this.imo = imo; }
    public String getMmsi() { return mmsi; }
    public void setMmsi(String mmsi) { this.mmsi = mmsi; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFlag() { return flag; }
    public void setFlag(String flag) { this.flag = flag; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getDeadweight() { return deadweight; }
    public void setDeadweight(int deadweight) { this.deadweight = deadweight; }
    public int getBuildYear() { return buildYear; }
    public void setBuildYear(int buildYear) { this.buildYear = buildYear; }
    public int getRiskScore() { return riskScore; }
    public void setRiskScore(int riskScore) { this.riskScore = riskScore; }
    public Instant getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Instant lastUpdated) { this.lastUpdated = lastUpdated; }
    public Neo4jCompanyNode getCompany() { return company; }
    public void setCompany(Neo4jCompanyNode company) { this.company = company; }
}

package com.anchoriq.api.infrastructure.persistence.neo4j.node;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

import java.time.LocalDate;

@Node("Sanction")
public class Neo4jSanctionNode {

    @Id
    @GeneratedValue
    private Long id;

    @Property("referenceNumber")
    private String referenceNumber;

    @Property("targetName")
    private String targetName;

    @Property("type")
    private String type;

    @Property("source")
    private String source;

    @Property("active")
    private boolean active;

    @Property("startDate")
    private LocalDate startDate;

    @Property("endDate")
    private LocalDate endDate;

    @Property("description")
    private String description;

    public Neo4jSanctionNode() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }
    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

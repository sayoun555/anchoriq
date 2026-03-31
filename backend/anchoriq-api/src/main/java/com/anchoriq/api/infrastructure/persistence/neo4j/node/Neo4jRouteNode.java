package com.anchoriq.api.infrastructure.persistence.neo4j.node;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

@Node("Route")
public class Neo4jRouteNode {

    @Id
    @GeneratedValue
    private Long id;

    @Property("name")
    private String name;

    @Property("displayName")
    private String displayName;

    @Property("distance")
    private int distanceNm;

    @Property("unit")
    private String unit;

    @Relationship(type = "PASSES_THROUGH", direction = Relationship.Direction.OUTGOING)
    private List<Neo4jChokepointNode> chokepoints = new ArrayList<>();

    public Neo4jRouteNode() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public int getDistanceNm() { return distanceNm; }
    public void setDistanceNm(int distanceNm) { this.distanceNm = distanceNm; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public List<Neo4jChokepointNode> getChokepoints() { return chokepoints; }
    public void setChokepoints(List<Neo4jChokepointNode> chokepoints) { this.chokepoints = chokepoints; }
}

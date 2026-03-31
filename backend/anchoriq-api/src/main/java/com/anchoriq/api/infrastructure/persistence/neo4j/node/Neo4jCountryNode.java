package com.anchoriq.api.infrastructure.persistence.neo4j.node;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

@Node("Country")
public class Neo4jCountryNode {

    @Id
    @GeneratedValue
    private Long id;

    @Property("isoCode")
    private String isoCode;

    @Property("name")
    private String name;

    @Property("region")
    private String region;

    @Property("sanctioned")
    private boolean sanctioned;

    public Neo4jCountryNode() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIsoCode() { return isoCode; }
    public void setIsoCode(String isoCode) { this.isoCode = isoCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public boolean isSanctioned() { return sanctioned; }
    public void setSanctioned(boolean sanctioned) { this.sanctioned = sanctioned; }
}

package com.anchoriq.api.infrastructure.persistence.neo4j.node;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node("Company")
public class Neo4jCompanyNode {

    @Id
    @GeneratedValue
    private Long id;

    @Property("name")
    private String name;

    @Property("registrationNumber")
    private String registrationNumber;

    @Relationship(type = "HEADQUARTERED_IN", direction = Relationship.Direction.OUTGOING)
    private Neo4jCountryNode country;

    public Neo4jCompanyNode() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    public Neo4jCountryNode getCountry() { return country; }
    public void setCountry(Neo4jCountryNode country) { this.country = country; }
}

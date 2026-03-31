package com.anchoriq.api.infrastructure.persistence.neo4j;

import com.anchoriq.api.infrastructure.persistence.neo4j.node.Neo4jChokepointNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;
import java.util.Optional;

public interface Neo4jChokepointRepository extends Neo4jRepository<Neo4jChokepointNode, Long> {

    Optional<Neo4jChokepointNode> findByName(String name);

    @Query("MATCH (cp:Chokepoint) WHERE cp.riskLevel = 'HIGH' RETURN cp")
    List<Neo4jChokepointNode> findHighRisk();
}

package com.anchoriq.api.infrastructure.persistence.neo4j;

import com.anchoriq.api.infrastructure.persistence.neo4j.node.Neo4jRouteNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.Optional;

public interface Neo4jRouteRepository extends Neo4jRepository<Neo4jRouteNode, Long> {

    Optional<Neo4jRouteNode> findByName(String name);
}

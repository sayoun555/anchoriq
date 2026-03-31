package com.anchoriq.api.infrastructure.persistence.neo4j;

import com.anchoriq.api.infrastructure.persistence.neo4j.node.Neo4jSeaZoneNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.List;
import java.util.Optional;

public interface Neo4jSeaZoneRepository extends Neo4jRepository<Neo4jSeaZoneNode, Long> {

    Optional<Neo4jSeaZoneNode> findByName(String name);

    List<Neo4jSeaZoneNode> findByCountry(String country);

    List<Neo4jSeaZoneNode> findByType(String type);
}

package com.anchoriq.api.infrastructure.persistence.neo4j;

import com.anchoriq.api.infrastructure.persistence.neo4j.node.Neo4jPortNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;
import java.util.Optional;

public interface Neo4jPortRepository extends Neo4jRepository<Neo4jPortNode, Long> {

    Optional<Neo4jPortNode> findByLocode(String locode);

    List<Neo4jPortNode> findByCountry(String country);

    @Query("MATCH (p:Port) WHERE p.congestionLevel >= $minCongestion RETURN p ORDER BY p.congestionLevel DESC")
    List<Neo4jPortNode> findCongestedPorts(double minCongestion);

    @Query("MATCH (p:Port) RETURN p ORDER BY p.congestionLevel DESC LIMIT $limit")
    List<Neo4jPortNode> findTopCongestedPorts(int limit);

    boolean existsByLocode(String locode);

    @Query("MATCH (p:Port) WHERE toLower(p.name) CONTAINS toLower($query) RETURN p LIMIT 20")
    List<Neo4jPortNode> searchByName(String query);
}

package com.anchoriq.api.infrastructure.persistence.neo4j;

import com.anchoriq.api.infrastructure.persistence.neo4j.node.Neo4jSanctionNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;
import java.util.Optional;

public interface Neo4jSanctionRepository extends Neo4jRepository<Neo4jSanctionNode, Long> {

    Optional<Neo4jSanctionNode> findByReferenceNumber(String referenceNumber);

    @Query("MATCH (s:Sanction) WHERE s.active = true RETURN s")
    List<Neo4jSanctionNode> findActiveSanctions();

    @Query("MATCH (s:Sanction) WHERE toLower(s.targetName) CONTAINS toLower($query) RETURN s")
    List<Neo4jSanctionNode> findByTargetNameContaining(String query);
}

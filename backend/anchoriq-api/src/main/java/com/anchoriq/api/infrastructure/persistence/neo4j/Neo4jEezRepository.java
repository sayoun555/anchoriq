package com.anchoriq.api.infrastructure.persistence.neo4j;

import com.anchoriq.api.infrastructure.persistence.neo4j.node.Neo4jEezNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data Neo4j Repository: EEZ(배타적 경제수역) 노드 접근.
 */
public interface Neo4jEezRepository extends Neo4jRepository<Neo4jEezNode, Long> {

    Optional<Neo4jEezNode> findByName(String name);

    List<Neo4jEezNode> findByIsoCode(String isoCode);
}

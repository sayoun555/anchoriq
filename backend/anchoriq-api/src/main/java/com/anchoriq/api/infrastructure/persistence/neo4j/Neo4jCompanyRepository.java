package com.anchoriq.api.infrastructure.persistence.neo4j;

import com.anchoriq.api.infrastructure.persistence.neo4j.node.Neo4jCompanyNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;
import java.util.Optional;

public interface Neo4jCompanyRepository extends Neo4jRepository<Neo4jCompanyNode, Long> {

    Optional<Neo4jCompanyNode> findByName(String name);

    boolean existsByName(String name);

    @Query("MATCH (c:Company)-[:HEADQUARTERED_IN]->(co:Country {isoCode: $countryCode}) RETURN c")
    List<Neo4jCompanyNode> findByCountryCode(String countryCode);
}

package com.anchoriq.api.infrastructure.persistence.neo4j;

import com.anchoriq.api.infrastructure.persistence.neo4j.node.Neo4jCountryNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface Neo4jCountryRepository extends Neo4jRepository<Neo4jCountryNode, Long> {

    Optional<Neo4jCountryNode> findByIsoCode(String isoCode);

    boolean existsByIsoCode(String isoCode);

    @Query("MATCH (c:Country) WHERE c.sanctioned = true RETURN c")
    List<Neo4jCountryNode> findSanctionedCountries();

    @Query("MATCH (c:Country) WHERE c.sanctioned = true RETURN c.isoCode")
    Set<String> findSanctionedCountryCodes();
}

package com.anchoriq.api.infrastructure.persistence.neo4j;

import com.anchoriq.api.infrastructure.persistence.neo4j.node.Neo4jVesselNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data Neo4j 기반 Vessel Repository.
 * Neo4j Node 타입을 사용한다. 도메인 변환은 Mapper에서 수행.
 */
public interface Neo4jVesselRepository extends Neo4jRepository<Neo4jVesselNode, Long> {

    Optional<Neo4jVesselNode> findByImo(String imo);

    Optional<Neo4jVesselNode> findByMmsi(String mmsi);

    List<Neo4jVesselNode> findByFlag(String flag);

    @Query("MATCH (v:Vessel) WHERE v.type = $type RETURN v")
    List<Neo4jVesselNode> findByType(String type);

    @Query("MATCH (v:Vessel)-[:OWNED_BY]->(c:Company)-[:HEADQUARTERED_IN]->(co:Country {sanctioned: true}) RETURN v")
    List<Neo4jVesselNode> findSanctionedVessels();

    @Query("MATCH (v:Vessel)-[:OWNED_BY]->(c:Company {name: $companyName}) RETURN v")
    List<Neo4jVesselNode> findByCompanyName(String companyName);

    @Query("MATCH (v:Vessel)-[:OWNED_BY]->(c:Company)-[:HEADQUARTERED_IN]->(co:Country {isoCode: $countryCode}) RETURN v")
    List<Neo4jVesselNode> findByCountryCode(String countryCode);

    @Query("MATCH (v:Vessel) WHERE toLower(v.name) CONTAINS toLower($query) RETURN v LIMIT 20")
    List<Neo4jVesselNode> searchByName(String query);

    boolean existsByImo(String imo);

    void deleteByImo(String imo);
}

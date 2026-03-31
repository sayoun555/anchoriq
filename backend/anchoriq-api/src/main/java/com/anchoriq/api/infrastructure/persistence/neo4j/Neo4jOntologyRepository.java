package com.anchoriq.api.infrastructure.persistence.neo4j;

import com.anchoriq.core.domain.maritime.ontology.repository.OntologyRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * Neo4j 기반 온톨로지 Repository 구현체.
 * SRP를 위해 기능별로 분리된 하위 Repository에 위임한다.
 */
@Repository
@RequiredArgsConstructor
public class Neo4jOntologyRepository implements OntologyRepository {

    private final Neo4jGraphExpansionRepository graphExpansion;
    private final Neo4jPathFindingRepository pathFinding;
    private final Neo4jSanctionNetworkRepository sanctionNetwork;
    private final Neo4jOntologySearchRepository search;
    private final Neo4jOntologyStatisticsRepository statistics;

    @Override
    public Map<String, Object> expandNode(Long nodeId, int depth) {
        return graphExpansion.expandNode(nodeId, depth);
    }

    @Override
    public List<Map<String, Object>> findShortestPath(Long fromNodeId, Long toNodeId) {
        return pathFinding.findShortestPath(fromNodeId, toNodeId);
    }

    @Override
    public Map<String, Object> findSanctionNetwork() {
        return sanctionNetwork.findSanctionNetwork();
    }

    @Override
    public Map<String, Long> getStatistics() {
        return statistics.getStatistics();
    }

    @Override
    public List<Map<String, Object>> searchEntities(String query, int limit) {
        return search.searchEntities(query, limit);
    }

    @Override
    public List<Map<String, Object>> findVesselsByCompany(String companyName) {
        return sanctionNetwork.findVesselsByCompany(companyName);
    }

    @Override
    public List<Map<String, Object>> findVesselsByCountry(String countryCode) {
        return sanctionNetwork.findVesselsByCountry(countryCode);
    }

    @Override
    public Map<String, Object> getGraphOverview(int nodeLimit) {
        return graphExpansion.getGraphOverview(nodeLimit);
    }
}

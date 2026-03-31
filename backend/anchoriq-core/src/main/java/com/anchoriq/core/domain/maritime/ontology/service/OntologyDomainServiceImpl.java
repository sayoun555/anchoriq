package com.anchoriq.core.domain.maritime.ontology.service;

import com.anchoriq.core.domain.maritime.ontology.repository.OntologyRepository;

import java.util.List;
import java.util.Map;

/**
 * 온톨로지 Domain Service 구현체.
 * Repository에 그래프 탐색을 위임하고, 도메인 로직을 조합한다.
 */
public class OntologyDomainServiceImpl implements OntologyDomainService {

    private final OntologyRepository ontologyRepository;

    public OntologyDomainServiceImpl(OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }

    @Override
    public Map<String, Object> expandNode(Long nodeId, int depth) {
        if (depth < 1 || depth > 4) {
            throw new IllegalArgumentException("Expansion depth must be between 1 and 4, but was: " + depth);
        }
        return ontologyRepository.expandNode(nodeId, depth);
    }

    @Override
    public List<Map<String, Object>> findShortestPath(Long fromNodeId, Long toNodeId) {
        if (fromNodeId.equals(toNodeId)) {
            throw new IllegalArgumentException("From and To nodes must be different");
        }
        return ontologyRepository.findShortestPath(fromNodeId, toNodeId);
    }

    @Override
    public Map<String, Object> findSanctionNetwork() {
        return ontologyRepository.findSanctionNetwork();
    }

    @Override
    public Map<String, Long> getStatistics() {
        return ontologyRepository.getStatistics();
    }

    @Override
    public List<Map<String, Object>> searchEntities(String query, int limit) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query must not be blank");
        }
        int effectiveLimit = Math.min(Math.max(limit, 1), 50);
        return ontologyRepository.searchEntities(query.trim(), effectiveLimit);
    }

    @Override
    public Map<String, Object> getGraphOverview(int nodeLimit) {
        int effectiveLimit = Math.min(Math.max(nodeLimit, 10), 200);
        return ontologyRepository.getGraphOverview(effectiveLimit);
    }
}

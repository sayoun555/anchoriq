package com.anchoriq.api.application.ontology;

import com.anchoriq.api.dto.response.ontology.OntologyGraphResponse;
import com.anchoriq.api.dto.response.ontology.OntologyPathResponse;
import com.anchoriq.api.dto.response.ontology.OntologySearchResponse;
import com.anchoriq.api.dto.response.ontology.OntologyStatisticsResponse;
import com.anchoriq.core.domain.maritime.company.repository.CompanyRepository;
import com.anchoriq.core.domain.maritime.country.repository.CountryRepository;
import com.anchoriq.core.domain.maritime.ontology.repository.OntologyRepository;
import com.anchoriq.core.domain.maritime.ontology.service.OntologyDomainService;
import com.anchoriq.core.domain.maritime.port.repository.PortRepository;
import com.anchoriq.core.domain.maritime.route.repository.ChokepointRepository;
import com.anchoriq.core.domain.maritime.route.repository.RouteRepository;
import com.anchoriq.core.domain.maritime.sanction.repository.SanctionRepository;
import com.anchoriq.core.domain.maritime.vessel.repository.VesselRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 온톨로지 Application Service 구현체.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OntologyApplicationServiceImpl implements OntologyApplicationService {

    private final OntologyDomainService ontologyDomainService;
    private final OntologyRepository ontologyRepository;
    private final VesselRepository vesselRepository;
    private final PortRepository portRepository;
    private final CompanyRepository companyRepository;
    private final CountryRepository countryRepository;
    private final RouteRepository routeRepository;
    private final ChokepointRepository chokepointRepository;
    private final SanctionRepository sanctionRepository;

    @Override
    @SuppressWarnings("unchecked")
    public OntologyGraphResponse getGraphOverview(int nodeLimit) {
        try {
            Map<String, Object> data = ontologyDomainService.getGraphOverview(nodeLimit);
            return OntologyGraphResponse.builder()
                    .nodes((List<Map<String, Object>>) data.get("nodes"))
                    .relationships((List<Map<String, Object>>) data.get("relationships"))
                    .build();
        } catch (Exception e) {
            log.warn("Failed to fetch graph overview: {}", e.getMessage());
            return OntologyGraphResponse.builder()
                    .nodes(List.of())
                    .relationships(List.of())
                    .build();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public OntologyGraphResponse expandNode(Long nodeId, int depth) {
        try {
            Map<String, Object> data = ontologyDomainService.expandNode(nodeId, depth);
            return OntologyGraphResponse.builder()
                    .nodes((List<Map<String, Object>>) data.get("nodes"))
                    .relationships((List<Map<String, Object>>) data.get("relationships"))
                    .build();
        } catch (Exception e) {
            log.warn("Failed to expand node {}: {}", nodeId, e.getMessage());
            return OntologyGraphResponse.builder()
                    .nodes(List.of())
                    .relationships(List.of())
                    .build();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public OntologyPathResponse findShortestPath(Long fromNodeId, Long toNodeId) {
        try {
            List<Map<String, Object>> paths = ontologyDomainService.findShortestPath(fromNodeId, toNodeId);
            if (paths.isEmpty()) {
                return OntologyPathResponse.builder()
                        .fromNodeId(fromNodeId)
                        .toNodeId(toNodeId)
                        .pathLength(0)
                        .nodes(List.of())
                        .relationships(List.of())
                        .build();
            }

            Map<String, Object> firstPath = paths.get(0);
            return OntologyPathResponse.builder()
                    .fromNodeId(fromNodeId)
                    .toNodeId(toNodeId)
                    .pathLength(((Number) firstPath.get("length")).intValue())
                    .nodes((List<Map<String, Object>>) firstPath.get("nodes"))
                    .relationships((List<Map<String, Object>>) firstPath.get("relationships"))
                    .build();
        } catch (Exception e) {
            log.warn("Failed to find shortest path from {} to {}: {}", fromNodeId, toNodeId, e.getMessage());
            return OntologyPathResponse.builder()
                    .fromNodeId(fromNodeId)
                    .toNodeId(toNodeId)
                    .pathLength(0)
                    .nodes(List.of())
                    .relationships(List.of())
                    .build();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public OntologyGraphResponse findSanctionNetwork() {
        try {
            Map<String, Object> data = ontologyDomainService.findSanctionNetwork();
            return OntologyGraphResponse.builder()
                    .nodes((List<Map<String, Object>>) data.get("nodes"))
                    .relationships((List<Map<String, Object>>) data.get("relationships"))
                    .build();
        } catch (Exception e) {
            log.warn("Failed to fetch sanction network: {}", e.getMessage());
            return OntologyGraphResponse.builder()
                    .nodes(List.of())
                    .relationships(List.of())
                    .build();
        }
    }

    @Override
    public OntologySearchResponse searchEntities(String query, int limit) {
        try {
            List<Map<String, Object>> results = ontologyDomainService.searchEntities(query, limit);
            return OntologySearchResponse.builder()
                    .query(query)
                    .totalResults(results.size())
                    .results(results)
                    .build();
        } catch (Exception e) {
            log.warn("Failed to search entities: {}", e.getMessage());
            return OntologySearchResponse.builder()
                    .query(query)
                    .totalResults(0)
                    .results(List.of())
                    .build();
        }
    }

    @Override
    public OntologyStatisticsResponse getStatistics() {
        try {
            Map<String, Long> stats = ontologyDomainService.getStatistics();
            return OntologyStatisticsResponse.builder()
                    .totalNodes(stats.getOrDefault("totalNodes", 0L))
                    .totalRelationships(stats.getOrDefault("totalRelationships", 0L))
                    .vesselCount(vesselRepository.count())
                    .portCount(portRepository.count())
                    .companyCount(companyRepository.count())
                    .countryCount(countryRepository.count())
                    .routeCount(routeRepository.count())
                    .chokepointCount(chokepointRepository.count())
                    .sanctionCount(sanctionRepository.count())
                    .build();
        } catch (Exception e) {
            log.warn("Failed to fetch ontology statistics: {}", e.getMessage());
            return OntologyStatisticsResponse.builder()
                    .totalNodes(0L)
                    .totalRelationships(0L)
                    .vesselCount(0L)
                    .portCount(0L)
                    .companyCount(0L)
                    .countryCount(0L)
                    .routeCount(0L)
                    .chokepointCount(0L)
                    .sanctionCount(0L)
                    .build();
        }
    }

    @Override
    public List<Map<String, Object>> findVesselsByCompany(String companyName) {
        try {
            return ontologyRepository.findVesselsByCompany(companyName);
        } catch (Exception e) {
            log.warn("Failed to find vessels by company {}: {}", companyName, e.getMessage());
            return List.of();
        }
    }

    @Override
    public List<Map<String, Object>> findVesselsByCountry(String countryCode) {
        try {
            return ontologyRepository.findVesselsByCountry(countryCode);
        } catch (Exception e) {
            log.warn("Failed to find vessels by country {}: {}", countryCode, e.getMessage());
            return List.of();
        }
    }
}

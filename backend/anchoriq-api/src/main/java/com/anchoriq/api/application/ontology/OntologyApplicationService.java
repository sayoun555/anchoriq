package com.anchoriq.api.application.ontology;

import com.anchoriq.api.dto.response.ontology.OntologyGraphResponse;
import com.anchoriq.api.dto.response.ontology.OntologyPathResponse;
import com.anchoriq.api.dto.response.ontology.OntologySearchResponse;
import com.anchoriq.api.dto.response.ontology.OntologyStatisticsResponse;

import java.util.List;
import java.util.Map;

/**
 * 온톨로지 Application Service 인터페이스.
 */
public interface OntologyApplicationService {

    OntologyGraphResponse getGraphOverview(int nodeLimit);

    OntologyGraphResponse expandNode(Long nodeId, int depth);

    OntologyPathResponse findShortestPath(Long fromNodeId, Long toNodeId);

    OntologyGraphResponse findSanctionNetwork();

    OntologySearchResponse searchEntities(String query, int limit);

    OntologyStatisticsResponse getStatistics();

    List<Map<String, Object>> findVesselsByCompany(String companyName);

    List<Map<String, Object>> findVesselsByCountry(String countryCode);
}

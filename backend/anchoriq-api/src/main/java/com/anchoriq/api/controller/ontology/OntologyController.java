package com.anchoriq.api.controller.ontology;

import com.anchoriq.api.application.ontology.OntologyApplicationService;
import com.anchoriq.api.dto.response.ontology.OntologyGraphResponse;
import com.anchoriq.api.dto.response.ontology.OntologyPathResponse;
import com.anchoriq.api.dto.response.ontology.OntologySearchResponse;
import com.anchoriq.api.dto.response.ontology.OntologyStatisticsResponse;
import com.anchoriq.api.global.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ontology")
@RequiredArgsConstructor
public class OntologyController {

    private final OntologyApplicationService ontologyService;

    @GetMapping("/graph")
    public ApiResponse<OntologyGraphResponse> getGraph(
            @RequestParam(defaultValue = "50") int nodeLimit) {
        return ApiResponse.success(ontologyService.getGraphOverview(nodeLimit));
    }

    @GetMapping("/graph/{nodeId}/expand")
    public ApiResponse<OntologyGraphResponse> expandNode(
            @PathVariable Long nodeId,
            @RequestParam(defaultValue = "1") int depth) {
        return ApiResponse.success(ontologyService.expandNode(nodeId, depth));
    }

    @GetMapping("/search")
    public ApiResponse<OntologySearchResponse> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(ontologyService.searchEntities(q, limit));
    }

    @GetMapping("/path")
    public ApiResponse<OntologyPathResponse> findShortestPath(
            @RequestParam Long from,
            @RequestParam Long to) {
        return ApiResponse.success(ontologyService.findShortestPath(from, to));
    }

    @GetMapping("/sanctions/network")
    public ApiResponse<OntologyGraphResponse> getSanctionNetwork() {
        return ApiResponse.success(ontologyService.findSanctionNetwork());
    }

    @GetMapping("/company/{name}/vessels")
    public ApiResponse<List<Map<String, Object>>> getVesselsByCompany(
            @PathVariable String name) {
        return ApiResponse.success(ontologyService.findVesselsByCompany(name));
    }

    @GetMapping("/country/{code}/vessels")
    public ApiResponse<List<Map<String, Object>>> getVesselsByCountry(
            @PathVariable String code) {
        return ApiResponse.success(ontologyService.findVesselsByCountry(code));
    }

    @GetMapping("/statistics")
    public ApiResponse<OntologyStatisticsResponse> getStatistics() {
        return ApiResponse.success(ontologyService.getStatistics());
    }
}

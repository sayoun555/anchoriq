package com.anchoriq.api.dto.response.ontology;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class OntologySearchResponse {

    private final String query;
    private final int totalResults;
    private final List<Map<String, Object>> results;
}

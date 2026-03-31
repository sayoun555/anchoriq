package com.anchoriq.api.dto.response.ontology;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class OntologyPathResponse {

    private final Long fromNodeId;
    private final Long toNodeId;
    private final int pathLength;
    private final List<Map<String, Object>> nodes;
    private final List<Map<String, Object>> relationships;
}

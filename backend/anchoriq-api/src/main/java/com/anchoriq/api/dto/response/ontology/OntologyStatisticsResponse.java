package com.anchoriq.api.dto.response.ontology;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OntologyStatisticsResponse {

    private final long totalNodes;
    private final long totalRelationships;
    private final long vesselCount;
    private final long portCount;
    private final long companyCount;
    private final long countryCount;
    private final long routeCount;
    private final long chokepointCount;
    private final long sanctionCount;
}

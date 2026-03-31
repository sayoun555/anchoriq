package com.anchoriq.api.dto.response.ai;

import java.util.List;
import java.util.Map;

public record AiQueryResponse(
        String answer,
        List<Map<String, Object>> entities,
        String cypher,
        int remainingQueries
) {
    public static AiQueryResponse of(Map<String, Object> result, int remainingQueries) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entities = (List<Map<String, Object>>) result.get("entities");
        return new AiQueryResponse(
                (String) result.get("answer"),
                entities,
                (String) result.get("cypher"),
                remainingQueries
        );
    }
}

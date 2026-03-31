package com.anchoriq.api.dto.response.ai;

import java.util.List;
import java.util.Map;

public record AiBriefingResponse(
        String date,
        String summary,
        List<Map<String, Object>> highlights,
        Map<String, Object> statistics
) {
    @SuppressWarnings("unchecked")
    public static AiBriefingResponse from(Map<String, Object> briefing) {
        return new AiBriefingResponse(
                (String) briefing.get("date"),
                (String) briefing.get("summary"),
                (List<Map<String, Object>>) briefing.get("highlights"),
                (Map<String, Object>) briefing.get("statistics")
        );
    }
}

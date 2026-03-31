package com.anchoriq.api.application.ai;

import com.anchoriq.ai.whatif.WhatIfResult;
import com.anchoriq.ai.whatif.WhatIfTemplate;

import java.util.List;
import java.util.Map;

/**
 * AI 질의 Application Service 인터페이스.
 */
public interface AiQueryApplicationService {

    Map<String, Object> handleQuery(String query, Long userId);

    Map<String, Object> getDailyBriefing();

    WhatIfResult simulateWhatIf(String scenario, String duration);

    List<WhatIfResult> getWhatIfHistory(Long userId);

    List<WhatIfTemplate> getWhatIfTemplates();

    List<Map<String, Object>> getRecommendations();

    Map<String, Object> getUsage(Long userId);
}

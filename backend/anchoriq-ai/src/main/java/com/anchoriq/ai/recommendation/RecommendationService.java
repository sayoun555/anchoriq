package com.anchoriq.ai.recommendation;

import java.util.List;
import java.util.Map;

/**
 * AI 추천 액션 서비스 인터페이스.
 */
public interface RecommendationService {

    List<Map<String, Object>> getRecommendations();

    Map<String, Object> applyRecommendation(String recommendationId);
}

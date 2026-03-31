package com.anchoriq.core.domain.intelligence.risk.service;

import com.anchoriq.core.domain.maritime.route.model.Route;

import java.util.List;
import java.util.Map;

/**
 * 항로 최적화 도메인 서비스.
 * 대체 항로 추천 및 비용 분석을 담당한다.
 */
public interface RouteOptimizationService {

    List<Route> findAlternativeRoutes(String chokepointName);

    Map<String, Object> analyzeRouteCost(Route route);

    Route recommendSafestRoute(List<Route> candidates);
}

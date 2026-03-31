package com.anchoriq.core.domain.intelligence.risk.service;

import com.anchoriq.core.domain.maritime.port.model.Port;
import com.anchoriq.core.domain.maritime.route.model.Route;

import java.util.List;
import java.util.Map;

/**
 * 항로/항만 비교 도메인 서비스.
 */
public interface RouteComparisonService {

    Map<String, Object> compareRoutes(List<Long> routeIds);

    Map<String, Object> comparePorts(List<String> locodes);
}

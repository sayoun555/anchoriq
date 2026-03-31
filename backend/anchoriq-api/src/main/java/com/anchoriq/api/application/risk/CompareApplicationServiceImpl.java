package com.anchoriq.api.application.risk;

import com.anchoriq.core.domain.intelligence.risk.service.RouteComparisonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 비교 Application Service 구현체.
 */
@Service
@RequiredArgsConstructor
public class CompareApplicationServiceImpl implements CompareApplicationService {

    private final RouteComparisonService routeComparisonService;

    @Override
    public Map<String, Object> compareRoutes(List<Long> routeIds) {
        return routeComparisonService.compareRoutes(routeIds);
    }

    @Override
    public Map<String, Object> comparePorts(List<String> locodes) {
        return routeComparisonService.comparePorts(locodes);
    }
}

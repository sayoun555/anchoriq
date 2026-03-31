package com.anchoriq.core.domain.intelligence.risk.service;

import com.anchoriq.core.domain.maritime.port.model.Port;
import com.anchoriq.core.domain.maritime.port.repository.PortRepository;
import com.anchoriq.core.domain.maritime.route.model.Route;
import com.anchoriq.core.domain.maritime.route.repository.RouteRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 항로/항만 비교 도메인 서비스 구현체.
 * Bean 등록은 DomainServiceConfig에서 수행한다.
 */
public class RouteComparisonServiceImpl implements RouteComparisonService {

    private final RouteRepository routeRepository;
    private final PortRepository portRepository;
    private final SupplyChainRiskService riskService;

    public RouteComparisonServiceImpl(RouteRepository routeRepository,
                                       PortRepository portRepository,
                                       SupplyChainRiskService riskService) {
        this.routeRepository = routeRepository;
        this.portRepository = portRepository;
        this.riskService = riskService;
    }

    @Override
    public Map<String, Object> compareRoutes(List<Long> routeIds) {
        List<Map<String, Object>> comparisons = new ArrayList<>();

        for (Long routeId : routeIds) {
            Optional<Route> optionalRoute = routeRepository.findById(routeId);
            optionalRoute.ifPresent(route -> {
                Map<String, Object> routeData = new HashMap<>();
                routeData.put("routeId", route.getId());
                routeData.put("name", route.getName());
                routeData.put("distanceNm", route.getDistanceNm());
                routeData.put("chokepointCount", route.getChokepoints().size());
                routeData.put("highRiskChokepointCount", route.countHighRiskChokepoints());
                routeData.put("riskScore", riskService.assessRouteRisk(route));
                comparisons.add(routeData);
            });
        }

        Map<String, Object> result = new HashMap<>();
        result.put("routes", comparisons);
        result.put("comparedCount", comparisons.size());
        return result;
    }

    @Override
    public Map<String, Object> comparePorts(List<String> locodes) {
        List<Map<String, Object>> comparisons = new ArrayList<>();

        for (String locode : locodes) {
            Optional<Port> optionalPort = portRepository.findByLocode(locode);
            optionalPort.ifPresent(port -> {
                Map<String, Object> portData = new HashMap<>();
                portData.put("locode", port.getLocode());
                portData.put("name", port.getName());
                portData.put("country", port.getCountry());
                portData.put("congestionLevel", port.getCongestionLevel());
                portData.put("isCongested", port.isCongested());
                portData.put("riskScore", riskService.assessPortRisk(port));
                comparisons.add(portData);
            });
        }

        Map<String, Object> result = new HashMap<>();
        result.put("ports", comparisons);
        result.put("comparedCount", comparisons.size());
        return result;
    }
}

package com.anchoriq.ai.whatif;

import com.anchoriq.ai.client.AiClient;
import com.anchoriq.core.domain.intelligence.risk.service.RouteOptimizationService;
import com.anchoriq.core.domain.maritime.route.model.Route;
import com.anchoriq.core.domain.maritime.route.repository.RouteRepository;
import com.anchoriq.core.domain.maritime.vessel.repository.VesselRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * What-if 시뮬레이션 서비스 구현체.
 */
@Slf4j
@Service
public class WhatIfServiceImpl implements WhatIfService {

    private static final String SIMULATION_PROMPT = """
            You are a maritime logistics simulation expert. Analyze the following scenario
            and provide:
            1. Impact assessment (affected vessels, routes, estimated delays)
            2. Alternative routes with cost/time estimates
            3. Actionable recommendations (3-5 items)

            Be specific with numbers and estimates. Keep under 500 words.
            """;

    private static final List<WhatIfTemplate> TEMPLATES = List.of(
            WhatIfTemplate.of("suez-closure", "Suez Canal Closure",
                    "Simulate impact of Suez Canal closure",
                    "Suez Canal is blocked for the specified duration", "3 days"),
            WhatIfTemplate.of("hormuz-closure", "Strait of Hormuz Closure",
                    "Simulate impact of Hormuz Strait closure",
                    "Strait of Hormuz is blocked for the specified duration", "7 days"),
            WhatIfTemplate.of("malacca-closure", "Malacca Strait Closure",
                    "Simulate impact of Malacca Strait closure",
                    "Malacca Strait is blocked for the specified duration", "5 days"),
            WhatIfTemplate.of("typhoon-impact", "Major Typhoon Impact",
                    "Simulate impact of a major typhoon on shipping lanes",
                    "Category 5 typhoon hits major shipping lane", "5 days"),
            WhatIfTemplate.of("sanctions-expansion", "Sanctions Expansion",
                    "Simulate impact of expanded sanctions on shipping",
                    "New sanctions imposed on major shipping nation", "30 days")
    );

    private final AiClient aiClient;
    private final RouteRepository routeRepository;
    private final VesselRepository vesselRepository;
    private final RouteOptimizationService routeOptimizationService;
    private final Map<Long, List<WhatIfResult>> userHistory = new ConcurrentHashMap<>();

    public WhatIfServiceImpl(AiClient aiClient,
                              RouteRepository routeRepository,
                              VesselRepository vesselRepository,
                              RouteOptimizationService routeOptimizationService) {
        this.aiClient = aiClient;
        this.routeRepository = routeRepository;
        this.vesselRepository = vesselRepository;
        this.routeOptimizationService = routeOptimizationService;
    }

    @Override
    public WhatIfResult simulate(String scenario, String duration) {
        Optional<String> chokepointName = extractChokepointFromScenario(scenario);

        List<Route> affectedRoutes = chokepointName
                .map(this::findAffectedRoutes)
                .orElse(List.of());
        int affectedVesselCount = estimateAffectedVessels(affectedRoutes);

        List<Route> alternatives = chokepointName
                .map(routeOptimizationService::findAlternativeRoutes)
                .orElse(List.of());

        List<Map<String, Object>> alternativeDetails = alternatives.stream()
                .map(routeOptimizationService::analyzeRouteCost)
                .toList();

        String dataForAi = formatSimulationData(scenario, duration,
                affectedRoutes.size(), affectedVesselCount, alternativeDetails);
        String aiAnalysis = aiClient.chat(SIMULATION_PROMPT, dataForAi).block();

        return WhatIfResult.builder(scenario)
                .duration(duration)
                .affectedVessels(affectedVesselCount)
                .affectedRoutes(affectedRoutes.stream().map(Route::getName).toList())
                .estimatedDelay(estimateDelay(duration))
                .additionalCost(estimateCost(affectedVesselCount))
                .alternativeRoutes(alternativeDetails)
                .recommendations(extractRecommendations(aiAnalysis))
                .aiAnalysis(aiAnalysis)
                .build();
    }

    @Override
    public List<WhatIfResult> getHistory(Long userId, int limit) {
        List<WhatIfResult> history = userHistory.getOrDefault(userId, List.of());
        int toIndex = Math.min(history.size(), limit);
        return history.subList(0, toIndex);
    }

    @Override
    public List<WhatIfTemplate> getTemplates() {
        return TEMPLATES;
    }

    private Optional<String> extractChokepointFromScenario(String scenario) {
        String lower = scenario.toLowerCase();
        if (lower.contains("suez")) return Optional.of("Suez");
        if (lower.contains("hormuz")) return Optional.of("Hormuz");
        if (lower.contains("malacca")) return Optional.of("Malacca");
        if (lower.contains("bab") || lower.contains("mandeb")) return Optional.of("BabElMandeb");
        if (lower.contains("taiwan")) return Optional.of("Taiwan");
        if (lower.contains("panama")) return Optional.of("Panama");
        return Optional.empty();
    }

    private List<Route> findAffectedRoutes(String chokepointName) {
        return routeRepository.findAll().stream()
                .filter(route -> route.passesThrough(chokepointName))
                .toList();
    }

    private int estimateAffectedVessels(List<Route> affectedRoutes) {
        return affectedRoutes.size() * 25;
    }

    private String estimateDelay(String duration) {
        return "5~14 days additional transit time";
    }

    private String estimateCost(int vesselCount) {
        long costPerVessel = 1_200_000L;
        return String.format("$%.1fM total estimated additional cost",
                (vesselCount * costPerVessel) / 1_000_000.0);
    }

    private List<String> extractRecommendations(String aiAnalysis) {
        if (aiAnalysis == null || aiAnalysis.isBlank()) {
            return List.of("Monitor situation closely", "Prepare alternative routes");
        }
        List<String> recommendations = new ArrayList<>();
        recommendations.add("Monitor affected chokepoint status");
        recommendations.add("Pre-approve alternative routing plans");
        recommendations.add("Notify affected vessel operators");
        return recommendations;
    }

    private String formatSimulationData(String scenario, String duration,
                                         int affectedRoutes, int affectedVessels,
                                         List<Map<String, Object>> alternatives) {
        return String.format("""
                Scenario: %s
                Duration: %s
                Affected routes: %d
                Estimated affected vessels: %d
                Available alternative routes: %d
                Alternative route details: %s
                """, scenario, duration, affectedRoutes, affectedVessels,
                alternatives.size(), alternatives.toString());
    }
}

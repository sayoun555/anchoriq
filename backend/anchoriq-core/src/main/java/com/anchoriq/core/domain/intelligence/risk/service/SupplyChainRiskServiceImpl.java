package com.anchoriq.core.domain.intelligence.risk.service;

import com.anchoriq.core.domain.intelligence.risk.model.RiskAssessment;
import com.anchoriq.core.domain.intelligence.risk.model.RiskScore;
import com.anchoriq.core.domain.intelligence.risk.model.RiskType;
import com.anchoriq.core.domain.maritime.port.model.Port;
import com.anchoriq.core.domain.maritime.route.model.Chokepoint;
import com.anchoriq.core.domain.maritime.route.model.Route;
import com.anchoriq.core.domain.maritime.sanction.model.Sanction;
import com.anchoriq.core.domain.maritime.sanction.repository.SanctionRepository;
import com.anchoriq.core.domain.maritime.vessel.model.Vessel;
import com.anchoriq.core.domain.maritime.weather.model.WeatherCondition;
import com.anchoriq.core.domain.maritime.weather.repository.WeatherRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 공급망 리스크 도메인 서비스 구현체.
 * Vessel + Route + Weather + Sanction 여러 Aggregate를 조합하여 리스크를 판단한다.
 * Bean 등록은 DomainServiceConfig에서 수행한다.
 */
public class SupplyChainRiskServiceImpl implements SupplyChainRiskService {

    private final SanctionRepository sanctionRepository;
    private final WeatherRepository weatherRepository;

    public SupplyChainRiskServiceImpl(SanctionRepository sanctionRepository,
                                      WeatherRepository weatherRepository) {
        this.sanctionRepository = sanctionRepository;
        this.weatherRepository = weatherRepository;
    }

    @Override
    public RiskScore assessVesselRisk(Vessel vessel) {
        Map<RiskType, Integer> factors = new HashMap<>();
        List<String> explanations = new ArrayList<>();

        int sanctionScore = evaluateVesselSanctionRisk(vessel, explanations);
        factors.put(RiskType.SANCTION, sanctionScore);

        int weatherScore = evaluateWeatherRisk(explanations);
        factors.put(RiskType.WEATHER, weatherScore);

        int ageScore = evaluateVesselAgeRisk(vessel, explanations);
        factors.put(RiskType.COMPOSITE, ageScore);

        int totalScore = calculateWeightedScore(factors);
        String explanation = String.join("; ", explanations);

        return RiskScore.of(vessel.getImo().value(), "VESSEL", totalScore, factors, explanation);
    }

    @Override
    public RiskScore assessRouteRisk(Route route) {
        Map<RiskType, Integer> factors = new HashMap<>();
        List<String> explanations = new ArrayList<>();

        int chokepointScore = evaluateRouteChokepointRisk(route, explanations);
        factors.put(RiskType.CHOKEPOINT, chokepointScore);

        int weatherScore = evaluateWeatherRisk(explanations);
        factors.put(RiskType.WEATHER, weatherScore);

        int totalScore = calculateWeightedScore(factors);
        String explanation = String.join("; ", explanations);

        return RiskScore.of(String.valueOf(route.getId()), "ROUTE", totalScore, factors, explanation);
    }

    @Override
    public RiskScore assessPortRisk(Port port) {
        Map<RiskType, Integer> factors = new HashMap<>();
        List<String> explanations = new ArrayList<>();

        int congestionScore = evaluatePortCongestionRisk(port, explanations);
        factors.put(RiskType.CONGESTION, congestionScore);

        int weatherScore = evaluateWeatherRisk(explanations);
        factors.put(RiskType.WEATHER, weatherScore);

        int totalScore = calculateWeightedScore(factors);
        String explanation = String.join("; ", explanations);

        return RiskScore.of(port.getLocode().value(), "PORT", totalScore, factors, explanation);
    }

    @Override
    public RiskScore assessChokepointRisk(Chokepoint chokepoint) {
        Map<RiskType, Integer> factors = new HashMap<>();
        List<String> explanations = new ArrayList<>();

        int geopoliticalScore = evaluateChokepointGeopoliticalRisk(chokepoint, explanations);
        factors.put(RiskType.GEOPOLITICAL, geopoliticalScore);

        int weatherScore = evaluateWeatherRisk(explanations);
        factors.put(RiskType.WEATHER, weatherScore);

        int trafficScore = evaluateChokepointTrafficRisk(chokepoint, explanations);
        factors.put(RiskType.CONGESTION, trafficScore);

        int totalScore = calculateWeightedScore(factors);
        String explanation = String.join("; ", explanations);

        return RiskScore.of(chokepoint.getName(), "CHOKEPOINT", totalScore, factors, explanation);
    }

    @Override
    public RiskAssessment createFullAssessment(String targetId, String targetType) {
        RiskScore score = RiskScore.zero(targetId, targetType);
        return RiskAssessment.create(
                UUID.randomUUID().toString(), targetId, targetType,
                score, List.of(), "Full assessment pending AI analysis");
    }

    private int evaluateVesselSanctionRisk(Vessel vessel, List<String> explanations) {
        Set<String> sanctionedCountries = sanctionRepository.findActiveSanctions().stream()
                .map(Sanction::getTargetName)
                .collect(Collectors.toSet());

        if (vessel.isRegisteredInSanctionedCountry(sanctionedCountries)) {
            explanations.add("Vessel is registered in a sanctioned country");
            return 80;
        }
        return 0;
    }

    private int evaluateWeatherRisk(List<String> explanations) {
        List<WeatherCondition> severeConditions = weatherRepository.findSevereConditions();
        if (!severeConditions.isEmpty()) {
            explanations.add("Severe weather conditions detected in operational area");
            return Math.min(severeConditions.size() * 20, 60);
        }
        return 0;
    }

    private int evaluateVesselAgeRisk(Vessel vessel, List<String> explanations) {
        int age = vessel.calculateAge();
        if (age > 25) {
            explanations.add("Vessel age exceeds 25 years (high maintenance risk)");
            return 40;
        }
        if (age > 15) {
            explanations.add("Vessel age exceeds 15 years (moderate maintenance risk)");
            return 20;
        }
        return 0;
    }

    private int evaluateRouteChokepointRisk(Route route, List<String> explanations) {
        int highRiskCount = route.countHighRiskChokepoints();
        if (highRiskCount > 0) {
            explanations.add(String.format("Route passes through %d high-risk chokepoint(s)", highRiskCount));
            return Math.min(highRiskCount * 30, 80);
        }
        return 0;
    }

    private int evaluatePortCongestionRisk(Port port, List<String> explanations) {
        if (port.isCriticalCongestion()) {
            explanations.add("Port congestion is at critical level");
            return 70;
        }
        if (port.isCongested()) {
            explanations.add("Port is congested");
            return 40;
        }
        return 0;
    }

    private int evaluateChokepointGeopoliticalRisk(Chokepoint chokepoint, List<String> explanations) {
        if (chokepoint.isHighRisk()) {
            explanations.add("Chokepoint has high geopolitical risk");
            return 70;
        }
        if (chokepoint.isMediumOrHighRisk()) {
            explanations.add("Chokepoint has medium geopolitical risk");
            return 40;
        }
        return 0;
    }

    private int evaluateChokepointTrafficRisk(Chokepoint chokepoint, List<String> explanations) {
        int volume = chokepoint.getTransitVolume();
        if (volume > 100) {
            explanations.add("High transit volume at chokepoint");
            return 40;
        }
        if (volume > 50) {
            explanations.add("Moderate transit volume at chokepoint");
            return 20;
        }
        return 0;
    }

    private int calculateWeightedScore(Map<RiskType, Integer> factors) {
        if (factors.isEmpty()) {
            return 0;
        }
        int total = factors.values().stream().mapToInt(Integer::intValue).sum();
        int count = (int) factors.values().stream().filter(v -> v > 0).count();
        if (count == 0) {
            return 0;
        }
        return Math.min(total / count + (count - 1) * 5, 100);
    }
}

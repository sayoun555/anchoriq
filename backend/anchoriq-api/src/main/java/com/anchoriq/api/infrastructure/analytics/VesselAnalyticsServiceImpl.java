package com.anchoriq.api.infrastructure.analytics;

import com.anchoriq.core.domain.analytics.model.Distribution;
import com.anchoriq.core.domain.analytics.service.VesselAnalyticsService;
import com.anchoriq.core.domain.maritime.vessel.model.Vessel;
import com.anchoriq.core.domain.maritime.vessel.repository.VesselRepository;

import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 선박 분석 서비스 구현체.
 * VesselRepository에서 전체 선박을 조회하여 분포를 계산한다.
 */
public class VesselAnalyticsServiceImpl implements VesselAnalyticsService {

    private final VesselRepository vesselRepository;

    public VesselAnalyticsServiceImpl(VesselRepository vesselRepository) {
        this.vesselRepository = vesselRepository;
    }

    @Override
    public List<Distribution> getDistributionByFlag() {
        List<Vessel> vessels = vesselRepository.findAll();
        long total = vessels.size();

        Map<String, Long> counts = vessels.stream()
                .filter(v -> v.getFlag() != null)
                .collect(Collectors.groupingBy(v -> v.getFlag().value(), Collectors.counting()));

        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> Distribution.ofTotal(e.getKey(), e.getValue(), total))
                .toList();
    }

    @Override
    public List<Distribution> getDistributionByType() {
        List<Vessel> vessels = vesselRepository.findAll();
        long total = vessels.size();

        Map<String, Long> counts = vessels.stream()
                .filter(v -> v.getType() != null)
                .collect(Collectors.groupingBy(v -> v.getType().name(), Collectors.counting()));

        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> Distribution.ofTotal(e.getKey(), e.getValue(), total))
                .toList();
    }

    @Override
    public List<Distribution> getDistributionByAgeRange() {
        List<Vessel> vessels = vesselRepository.findAll();
        long total = vessels.size();
        int currentYear = Year.now().getValue();

        Map<String, Long> ageBuckets = new LinkedHashMap<>();
        ageBuckets.put("0-5", 0L);
        ageBuckets.put("5-10", 0L);
        ageBuckets.put("10-15", 0L);
        ageBuckets.put("15-20", 0L);
        ageBuckets.put("20+", 0L);

        for (Vessel v : vessels) {
            int age = v.getBuildYear() > 0 ? currentYear - v.getBuildYear() : 0;
            String bucket = classifyAge(age);
            ageBuckets.merge(bucket, 1L, Long::sum);
        }

        return ageBuckets.entrySet().stream()
                .map(e -> Distribution.ofTotal(e.getKey(), e.getValue(), total))
                .toList();
    }

    @Override
    public double getSanctionedRatio() {
        long total = vesselRepository.count();
        if (total == 0) {
            return 0.0;
        }
        long sanctioned = vesselRepository.findSanctionedVessels().size();
        return Math.round((double) sanctioned / total * 10000.0) / 100.0;
    }

    @Override
    public List<Distribution> getRiskScoreDistribution() {
        List<Vessel> vessels = vesselRepository.findAll();
        long total = vessels.size();

        Map<String, Long> riskBuckets = new LinkedHashMap<>();
        riskBuckets.put("0-20", 0L);
        riskBuckets.put("20-40", 0L);
        riskBuckets.put("40-60", 0L);
        riskBuckets.put("60-80", 0L);
        riskBuckets.put("80-100", 0L);

        for (Vessel v : vessels) {
            String bucket = classifyRiskScore(v.getRiskScore());
            riskBuckets.merge(bucket, 1L, Long::sum);
        }

        return riskBuckets.entrySet().stream()
                .map(e -> Distribution.ofTotal(e.getKey(), e.getValue(), total))
                .toList();
    }

    private String classifyAge(int age) {
        if (age < 5) return "0-5";
        if (age < 10) return "5-10";
        if (age < 15) return "10-15";
        if (age < 20) return "15-20";
        return "20+";
    }

    private String classifyRiskScore(int score) {
        if (score < 20) return "0-20";
        if (score < 40) return "20-40";
        if (score < 60) return "40-60";
        if (score < 80) return "60-80";
        return "80-100";
    }
}

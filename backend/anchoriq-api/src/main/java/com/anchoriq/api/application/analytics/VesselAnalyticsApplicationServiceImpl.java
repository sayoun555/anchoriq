package com.anchoriq.api.application.analytics;

import com.anchoriq.api.dto.response.analytics.AnalyticsResponse;
import com.anchoriq.api.dto.response.analytics.DistributionResponse;
import com.anchoriq.core.domain.analytics.service.VesselAnalyticsService;
import org.springframework.stereotype.Service;

import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * 선박 분석 Application Service 구현체.
 */
@Service
@RequiredArgsConstructor
public class VesselAnalyticsApplicationServiceImpl implements VesselAnalyticsApplicationService {

    private final VesselAnalyticsService vesselAnalyticsService;

    @Override
    public AnalyticsResponse<List<DistributionResponse>> getDistributionByFlag() {
        List<DistributionResponse> data = vesselAnalyticsService.getDistributionByFlag()
                .stream().map(DistributionResponse::from).toList();
        return AnalyticsResponse.snapshot("vessel_by_flag", data);
    }

    @Override
    public AnalyticsResponse<List<DistributionResponse>> getDistributionByType() {
        List<DistributionResponse> data = vesselAnalyticsService.getDistributionByType()
                .stream().map(DistributionResponse::from).toList();
        return AnalyticsResponse.snapshot("vessel_by_type", data);
    }

    @Override
    public AnalyticsResponse<List<DistributionResponse>> getDistributionByAgeRange() {
        List<DistributionResponse> data = vesselAnalyticsService.getDistributionByAgeRange()
                .stream().map(DistributionResponse::from).toList();
        return AnalyticsResponse.snapshot("vessel_by_age", data);
    }

    @Override
    public AnalyticsResponse<Double> getSanctionedRatio() {
        double ratio = vesselAnalyticsService.getSanctionedRatio();
        return AnalyticsResponse.snapshot("vessel_sanctioned_ratio", ratio);
    }

    @Override
    public AnalyticsResponse<List<DistributionResponse>> getRiskScoreDistribution() {
        List<DistributionResponse> data = vesselAnalyticsService.getRiskScoreDistribution()
                .stream().map(DistributionResponse::from).toList();
        return AnalyticsResponse.snapshot("vessel_risk_distribution", data);
    }
}

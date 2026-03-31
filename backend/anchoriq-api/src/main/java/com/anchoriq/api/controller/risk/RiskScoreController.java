package com.anchoriq.api.controller.risk;

import com.anchoriq.api.application.risk.RiskScoreApplicationService;
import com.anchoriq.api.dto.response.risk.RiskScoreResponse;
import com.anchoriq.api.global.response.ApiResponse;
import com.anchoriq.core.domain.intelligence.risk.model.RiskScore;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 리스크 스코어 Controller.
 * 선박/항로/항만/초크포인트별 리스크 스코어 (0~100) 조회.
 */
@RestController
@RequestMapping("/api/risk/score")
@RequiredArgsConstructor
public class RiskScoreController {

    private final RiskScoreApplicationService riskScoreApplicationService;

    @GetMapping("/vessel/{imo}")
    public ApiResponse<RiskScoreResponse> getVesselRiskScore(@PathVariable String imo) {
        RiskScore score = riskScoreApplicationService.getVesselRiskScore(imo);
        return ApiResponse.success(score != null ? RiskScoreResponse.from(score) : null);
    }

    @GetMapping("/route/{id}")
    public ApiResponse<RiskScoreResponse> getRouteRiskScore(@PathVariable String id) {
        RiskScore score = riskScoreApplicationService.getRouteRiskScore(id);
        return ApiResponse.success(score != null ? RiskScoreResponse.from(score) : null);
    }

    @GetMapping("/port/{locode}")
    public ApiResponse<RiskScoreResponse> getPortRiskScore(@PathVariable String locode) {
        RiskScore score = riskScoreApplicationService.getPortRiskScore(locode);
        return ApiResponse.success(score != null ? RiskScoreResponse.from(score) : null);
    }

    @GetMapping("/chokepoint/{name}")
    public ApiResponse<RiskScoreResponse> getChokepointRiskScore(@PathVariable String name) {
        RiskScore score = riskScoreApplicationService.getChokepointRiskScore(name);
        return ApiResponse.success(score != null ? RiskScoreResponse.from(score) : null);
    }
}

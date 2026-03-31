package com.anchoriq.api.controller.anomaly;

import com.anchoriq.api.application.risk.AnomalyApplicationService;
import com.anchoriq.api.dto.response.risk.AnomalyResponse;
import com.anchoriq.api.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 이상 탐지 Controller.
 * AIS 소실, 항로 이탈, 속도 이상, 다크 쉽.
 */
@RestController
@RequestMapping("/api/anomaly")
@RequiredArgsConstructor
public class AnomalyController {

    private final AnomalyApplicationService anomalyApplicationService;

    @GetMapping("/ais-off")
    public ApiResponse<List<AnomalyResponse>> getAisOffVessels() {
        List<AnomalyResponse> responses = anomalyApplicationService.getAisOffVessels().stream()
                .map(AnomalyResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/route-deviation")
    public ApiResponse<List<AnomalyResponse>> getRouteDeviations() {
        List<AnomalyResponse> responses = anomalyApplicationService.getRouteDeviations().stream()
                .map(AnomalyResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/speed-change")
    public ApiResponse<List<AnomalyResponse>> getSpeedAnomalies() {
        List<AnomalyResponse> responses = anomalyApplicationService.getSpeedAnomalies().stream()
                .map(AnomalyResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }

    @GetMapping("/dark-ships")
    public ApiResponse<List<AnomalyResponse>> getDarkShips() {
        List<AnomalyResponse> responses = anomalyApplicationService.getDarkShips().stream()
                .map(AnomalyResponse::from)
                .toList();
        return ApiResponse.success(responses);
    }
}

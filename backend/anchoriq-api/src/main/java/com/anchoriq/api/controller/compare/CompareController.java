package com.anchoriq.api.controller.compare;

import com.anchoriq.api.application.risk.CompareApplicationService;
import com.anchoriq.api.dto.request.ai.PortCompareRequest;
import com.anchoriq.api.dto.request.ai.RouteCompareRequest;
import com.anchoriq.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 항로/항만 비교 Controller.
 */
@RestController
@RequestMapping("/api/compare")
@RequiredArgsConstructor
public class CompareController {

    private final CompareApplicationService compareApplicationService;

    @PostMapping("/routes")
    public ApiResponse<Map<String, Object>> compareRoutes(
            @Valid @RequestBody RouteCompareRequest request) {
        return ApiResponse.success(compareApplicationService.compareRoutes(request.routeIds()));
    }

    @PostMapping("/ports")
    public ApiResponse<Map<String, Object>> comparePorts(
            @Valid @RequestBody PortCompareRequest request) {
        return ApiResponse.success(compareApplicationService.comparePorts(request.locodes()));
    }
}

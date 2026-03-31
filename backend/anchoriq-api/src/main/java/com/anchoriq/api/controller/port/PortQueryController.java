package com.anchoriq.api.controller.port;

import com.anchoriq.api.application.port.PortApplicationService;
import com.anchoriq.api.dto.response.port.PortResponse;
import com.anchoriq.api.global.response.ApiResponse;
import com.anchoriq.api.global.response.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ports")
@RequiredArgsConstructor
public class PortQueryController {

    private final PortApplicationService portService;

    @GetMapping
    public ApiResponse<PageResponse<PortResponse>> getPorts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<PortResponse> allPorts = portService.findAll();
        return ApiResponse.success(PageResponse.of(allPorts, page, size));
    }

    @GetMapping("/{locode}")
    public ApiResponse<PortResponse> getPortByLocode(@PathVariable String locode) {
        return ApiResponse.success(portService.findByLocode(locode));
    }

    @GetMapping("/{locode}/congestion")
    public ApiResponse<Map<String, Object>> getCongestion(@PathVariable String locode) {
        return ApiResponse.success(portService.getCongestionDetail(locode));
    }

    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> getStatistics() {
        return ApiResponse.success(Map.of("totalPorts", portService.count()));
    }

    @GetMapping("/ranking")
    public ApiResponse<List<PortResponse>> getCongestionRanking(
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(portService.findTopCongestedPorts(limit));
    }
}

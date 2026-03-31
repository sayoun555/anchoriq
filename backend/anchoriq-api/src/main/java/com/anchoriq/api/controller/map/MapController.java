package com.anchoriq.api.controller.map;

import com.anchoriq.api.application.map.MapApplicationService;
import com.anchoriq.api.dto.response.map.ChokepointMapResponse;
import com.anchoriq.api.dto.response.map.MapVesselResponse;
import com.anchoriq.api.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 지도 데이터 Controller.
 */
@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class MapController {

    private final MapApplicationService mapApplicationService;

    @GetMapping("/vessels")
    public ApiResponse<List<MapVesselResponse>> getVesselPositions() {
        return ApiResponse.success(mapApplicationService.getVesselPositions());
    }

    @GetMapping("/chokepoints")
    public ApiResponse<List<ChokepointMapResponse>> getChokepoints() {
        return ApiResponse.success(mapApplicationService.getChokepoints());
    }

    @GetMapping("/heatmap")
    public ApiResponse<List<Map<String, Object>>> getHeatmap() {
        return ApiResponse.success(mapApplicationService.getHeatmapData());
    }
}

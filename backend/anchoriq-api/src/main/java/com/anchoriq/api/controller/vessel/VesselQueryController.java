package com.anchoriq.api.controller.vessel;

import com.anchoriq.api.application.vessel.VesselQueryApplicationService;
import com.anchoriq.api.dto.response.vessel.VesselResponse;
import com.anchoriq.api.dto.response.vessel.VesselStatisticsResponse;
import com.anchoriq.api.global.response.ApiResponse;
import com.anchoriq.api.global.response.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/vessels")
@RequiredArgsConstructor
public class VesselQueryController {

    private final VesselQueryApplicationService vesselQueryService;

    @GetMapping
    public ApiResponse<PageResponse<VesselResponse>> getVessels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<VesselResponse> allVessels = vesselQueryService.findAll();
        return ApiResponse.success(PageResponse.of(allVessels, page, size));
    }

    @GetMapping("/{imo}")
    public ApiResponse<VesselResponse> getVesselByImo(@PathVariable String imo) {
        return ApiResponse.success(vesselQueryService.findByImo(imo));
    }

    @GetMapping("/sanctioned")
    public ApiResponse<List<VesselResponse>> getSanctionedVessels() {
        return ApiResponse.success(vesselQueryService.findSanctionedVessels());
    }

    @GetMapping("/statistics")
    public ApiResponse<VesselStatisticsResponse> getStatistics() {
        return ApiResponse.success(vesselQueryService.getStatistics());
    }
}

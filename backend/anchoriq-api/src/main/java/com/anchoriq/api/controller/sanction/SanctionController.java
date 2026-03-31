package com.anchoriq.api.controller.sanction;

import com.anchoriq.api.application.sanction.SanctionApplicationService;
import com.anchoriq.api.dto.response.sanction.SanctionResponse;
import com.anchoriq.api.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 제재 조회 Controller.
 */
@RestController
@RequestMapping("/api/sanctions")
@RequiredArgsConstructor
public class SanctionController {

    private final SanctionApplicationService sanctionApplicationService;

    @GetMapping("/list")
    public ApiResponse<List<SanctionResponse>> getSanctions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(sanctionApplicationService.findAll(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<SanctionResponse> getSanction(@PathVariable Long id) {
        return ApiResponse.success(sanctionApplicationService.findById(id));
    }

    @GetMapping("/search")
    public ApiResponse<List<SanctionResponse>> searchSanctions(@RequestParam String q) {
        return ApiResponse.success(sanctionApplicationService.search(q));
    }
}

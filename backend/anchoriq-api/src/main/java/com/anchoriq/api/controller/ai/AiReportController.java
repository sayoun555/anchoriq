package com.anchoriq.api.controller.ai;

import com.anchoriq.api.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AI 리포트 생성/조회 + 추천 액션 적용 Controller.
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiReportController {

    @PostMapping("/report/generate")
    public ApiResponse<Map<String, Object>> generateReport() {
        Map<String, Object> result = Map.of(
                "id", java.util.UUID.randomUUID().toString(),
                "status", "GENERATING",
                "message", "Report generation started. Check back shortly."
        );
        return ApiResponse.success(result);
    }

    @GetMapping("/report/{id}")
    public ApiResponse<Map<String, Object>> getReport(@PathVariable String id) {
        Map<String, Object> report = Map.of(
                "id", id,
                "status", "COMPLETED",
                "message", "Report is ready for download."
        );
        return ApiResponse.success(report);
    }

    @PostMapping("/recommendations/{id}/apply")
    public ApiResponse<Map<String, Object>> applyRecommendation(@PathVariable String id) {
        Map<String, Object> result = Map.of(
                "recommendationId", id,
                "status", "APPLIED",
                "message", "Recommendation has been applied."
        );
        return ApiResponse.success(result);
    }
}

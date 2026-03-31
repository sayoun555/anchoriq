package com.anchoriq.api.controller.ai;

import com.anchoriq.api.application.ai.AiQueryApplicationService;
import com.anchoriq.api.dto.response.ai.AiBriefingResponse;
import com.anchoriq.api.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 일일 브리핑 + AI 판단 이력 Controller.
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiBriefingController {

    private final AiQueryApplicationService aiQueryApplicationService;

    @GetMapping("/briefing")
    public ApiResponse<AiBriefingResponse> getDailyBriefing() {
        Map<String, Object> briefing = aiQueryApplicationService.getDailyBriefing();
        if (briefing == null || briefing.isEmpty()) {
            return ApiResponse.success(null);
        }
        return ApiResponse.success(AiBriefingResponse.from(briefing));
    }

    @GetMapping("/recommendations")
    public ApiResponse<?> getRecommendations() {
        return ApiResponse.success(aiQueryApplicationService.getRecommendations());
    }
}

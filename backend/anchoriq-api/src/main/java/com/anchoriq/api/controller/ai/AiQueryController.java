package com.anchoriq.api.controller.ai;

import com.anchoriq.api.application.ai.AiQueryApplicationService;
import com.anchoriq.api.dto.request.ai.AiQueryRequest;
import com.anchoriq.api.dto.response.ai.AiQueryResponse;
import com.anchoriq.api.global.response.ApiResponse;
import com.anchoriq.api.infrastructure.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AI 자연어 질의 + 사용량 조회 Controller.
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiQueryController {

    private final AiQueryApplicationService aiQueryApplicationService;

    @PostMapping("/query")
    public ApiResponse<AiQueryResponse> query(
            @Valid @RequestBody AiQueryRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        Map<String, Object> result = aiQueryApplicationService.handleQuery(
                request.query(), principal.userId());

        Map<String, Object> usage = aiQueryApplicationService.getUsage(principal.userId());
        boolean hasQuota = (Boolean) usage.get("hasRemainingQuota");

        AiQueryResponse response = AiQueryResponse.of(result, hasQuota ? -1 : 0);
        return ApiResponse.success(response);
    }

    @GetMapping("/usage")
    public ApiResponse<Map<String, Object>> getUsage(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(aiQueryApplicationService.getUsage(principal.userId()));
    }
}

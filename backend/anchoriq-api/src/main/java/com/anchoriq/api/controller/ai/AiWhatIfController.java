package com.anchoriq.api.controller.ai;

import com.anchoriq.ai.whatif.WhatIfResult;
import com.anchoriq.ai.whatif.WhatIfTemplate;
import com.anchoriq.api.application.ai.AiQueryApplicationService;
import com.anchoriq.api.dto.request.ai.WhatIfRequest;
import com.anchoriq.api.dto.response.ai.WhatIfResponse;
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

import java.util.List;

/**
 * What-if 시뮬레이션 Controller.
 */
@RestController
@RequestMapping("/api/ai/whatif")
@RequiredArgsConstructor
public class AiWhatIfController {

    private final AiQueryApplicationService aiQueryApplicationService;

    @PostMapping
    public ApiResponse<WhatIfResponse> simulate(
            @Valid @RequestBody WhatIfRequest request) {
        WhatIfResult result = aiQueryApplicationService.simulateWhatIf(
                request.scenario(), request.duration());
        return ApiResponse.success(WhatIfResponse.from(result));
    }

    @GetMapping("/history")
    public ApiResponse<List<WhatIfResult>> getHistory(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(aiQueryApplicationService.getWhatIfHistory(principal.userId()));
    }

    @GetMapping("/templates")
    public ApiResponse<List<WhatIfTemplate>> getTemplates() {
        return ApiResponse.success(aiQueryApplicationService.getWhatIfTemplates());
    }
}

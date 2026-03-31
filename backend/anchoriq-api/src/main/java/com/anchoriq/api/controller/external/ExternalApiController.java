package com.anchoriq.api.controller.external;

import com.anchoriq.api.annotation.RequiresPlan;
import com.anchoriq.api.dto.response.apikey.ApiKeyCreatedResponse;
import com.anchoriq.api.dto.response.apikey.ApiKeyResponse;
import com.anchoriq.api.global.response.ApiResponse;
import com.anchoriq.api.infrastructure.security.UserPrincipal;
import com.anchoriq.core.common.exception.EntityNotFoundException;
import com.anchoriq.core.domain.account.apikey.model.ApiKey;
import com.anchoriq.core.domain.account.apikey.repository.ApiKeyRepository;
import com.anchoriq.core.domain.account.subscription.model.Plan;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/external")
@RequiredArgsConstructor
public class ExternalApiController {

    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/api-key")
    @RequiresPlan(Plan.ENTERPRISE)
    public ApiResponse<ApiKeyResponse> getApiKey(
            @AuthenticationPrincipal UserPrincipal principal) {
        ApiKey apiKey = apiKeyRepository.findByUserId(principal.userId())
                .orElseThrow(() -> new EntityNotFoundException("ApiKey", String.valueOf(principal.userId())));

        return ApiResponse.success(new ApiKeyResponse(
                apiKey.getId(),
                apiKey.getName(),
                "aq_****",
                apiKey.getLastUsedAt(),
                apiKey.getCreatedAt()
        ));
    }

    @PostMapping("/api-key/regenerate")
    @RequiresPlan(Plan.ENTERPRISE)
    public ApiResponse<ApiKeyCreatedResponse> regenerateApiKey(
            @AuthenticationPrincipal UserPrincipal principal) {
        String rawKey = "aq_" + UUID.randomUUID().toString().replace("-", "");
        String keyHash = passwordEncoder.encode(rawKey);

        ApiKey apiKey = apiKeyRepository.findByUserId(principal.userId())
                .map(existing -> {
                    existing.updateKeyHash(keyHash);
                    return existing;
                })
                .orElseGet(() -> ApiKey.create(principal.userId(), keyHash, "Default API Key"));

        apiKeyRepository.save(apiKey);
        return ApiResponse.success(ApiKeyCreatedResponse.of(rawKey));
    }
}

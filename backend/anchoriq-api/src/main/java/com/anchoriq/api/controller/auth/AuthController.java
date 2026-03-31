package com.anchoriq.api.controller.auth;

import com.anchoriq.api.application.audit.AuditEvent;
import com.anchoriq.api.application.auth.AuthApplicationService;
import com.anchoriq.api.dto.request.auth.LoginRequest;
import com.anchoriq.api.dto.request.auth.SignupRequest;
import com.anchoriq.api.dto.response.auth.AuthTokenResponse;
import com.anchoriq.api.dto.response.auth.UserResponse;
import com.anchoriq.api.global.response.ApiResponse;
import com.anchoriq.api.infrastructure.security.CookieProvider;
import com.anchoriq.api.infrastructure.security.UserPrincipal;
import com.anchoriq.core.domain.operation.audit.model.AuditAction;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthApplicationService authApplicationService;
    private final ApplicationEventPublisher eventPublisher;
    private final CookieProvider cookieProvider;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserResponse>> signup(
            @Valid @RequestBody SignupRequest request,
            HttpServletRequest httpRequest) {
        UserResponse response = authApplicationService.signup(request);
        eventPublisher.publishEvent(new AuditEvent(
                response.id(), AuditAction.SIGNUP, "auth", null, getClientIp(httpRequest)));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse httpResponse) {
        AuthTokenResponse tokens = authApplicationService.login(request);
        cookieProvider.addAccessTokenCookie(httpResponse, tokens.accessToken());
        cookieProvider.addRefreshTokenCookie(httpResponse, tokens.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "tokenType", "Cookie",
                "expiresIn", tokens.expiresIn()
        )));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refresh(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String refreshToken = extractCookieValue(httpRequest, CookieProvider.REFRESH_TOKEN_COOKIE);
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", "Refresh token not found"));
        }
        AuthTokenResponse tokens = authApplicationService.refresh(refreshToken);
        cookieProvider.addAccessTokenCookie(httpResponse, tokens.accessToken());
        cookieProvider.addRefreshTokenCookie(httpResponse, tokens.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "tokenType", "Cookie",
                "expiresIn", tokens.expiresIn()
        )));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletResponse httpResponse) {
        authApplicationService.logout(principal.userId());
        cookieProvider.clearTokenCookies(httpResponse);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(
            @AuthenticationPrincipal UserPrincipal principal) {
        UserResponse response = authApplicationService.getMe(principal.userId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private String extractCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

package com.anchoriq.api.controller.user;

import com.anchoriq.api.application.audit.AuditEvent;
import com.anchoriq.api.application.user.UserApplicationService;
import com.anchoriq.api.dto.request.auth.PasswordChangeRequest;
import com.anchoriq.api.dto.request.auth.ProfileUpdateRequest;
import com.anchoriq.api.dto.response.auth.UserResponse;
import com.anchoriq.api.global.response.ApiResponse;
import com.anchoriq.api.infrastructure.security.UserPrincipal;
import com.anchoriq.core.domain.operation.audit.model.AuditAction;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserApplicationService userApplicationService;
    private final ApplicationEventPublisher eventPublisher;

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ProfileUpdateRequest request,
            HttpServletRequest httpRequest) {
        UserResponse response = userApplicationService.updateProfile(principal.userId(), request);
        eventPublisher.publishEvent(new AuditEvent(
                principal.userId(), AuditAction.PROFILE_UPDATE, "user", null, httpRequest.getRemoteAddr()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody PasswordChangeRequest request,
            HttpServletRequest httpRequest) {
        userApplicationService.changePassword(principal.userId(), request);
        eventPublisher.publishEvent(new AuditEvent(
                principal.userId(), AuditAction.PASSWORD_CHANGE, "user", null, httpRequest.getRemoteAddr()));
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/account")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            HttpServletRequest httpRequest) {
        eventPublisher.publishEvent(new AuditEvent(
                principal.userId(), AuditAction.ACCOUNT_DELETE, "user", null, httpRequest.getRemoteAddr()));
        userApplicationService.deleteAccount(principal.userId());
        return ResponseEntity.ok(ApiResponse.success());
    }
}

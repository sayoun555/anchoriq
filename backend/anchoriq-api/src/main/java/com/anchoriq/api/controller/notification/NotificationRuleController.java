package com.anchoriq.api.controller.notification;

import com.anchoriq.api.application.notification.NotificationApplicationService;
import com.anchoriq.api.dto.request.notification.NotificationRuleRequest;
import com.anchoriq.api.dto.response.notification.NotificationRuleResponse;
import com.anchoriq.api.global.response.ApiResponse;
import com.anchoriq.api.global.response.PageResponse;
import com.anchoriq.api.infrastructure.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 알림 규칙 Controller — 규칙 CRUD.
 */
@RestController
@RequestMapping("/api/notifications/rules")
@RequiredArgsConstructor
public class NotificationRuleController {

    private final NotificationApplicationService notificationService;

    @PostMapping
    public ResponseEntity<ApiResponse<NotificationRuleResponse>> createRule(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody NotificationRuleRequest request) {
        NotificationRuleResponse response = notificationService.createRule(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NotificationRuleResponse>>> getRules(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<NotificationRuleResponse> page = notificationService.getRules(principal.userId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationRuleResponse>> updateRule(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody NotificationRuleRequest request) {
        NotificationRuleResponse response = notificationService.updateRule(principal.userId(), id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRule(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        notificationService.deleteRule(principal.userId(), id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}

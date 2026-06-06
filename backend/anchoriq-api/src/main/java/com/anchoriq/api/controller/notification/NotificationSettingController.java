package com.anchoriq.api.controller.notification;

import com.anchoriq.api.application.notification.NotificationApplicationService;
import com.anchoriq.api.dto.request.notification.NotificationSettingsRequest;
import com.anchoriq.api.dto.request.notification.NotificationTestRequest;
import com.anchoriq.api.dto.response.notification.NotificationHistoryResponse;
import com.anchoriq.api.dto.response.notification.NotificationSettingsResponse;
import com.anchoriq.api.global.response.ApiResponse;
import com.anchoriq.api.global.response.PageResponse;
import com.anchoriq.api.infrastructure.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 알림 설정 Controller — 설정 조회/변경, 테스트 발송, 이력 조회.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationSettingController {

    private final NotificationApplicationService notificationService;

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<NotificationSettingsResponse>> getSettings(
            @AuthenticationPrincipal UserPrincipal principal) {
        NotificationSettingsResponse response = notificationService.getSettings(principal.userId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/settings")
    public ResponseEntity<ApiResponse<NotificationSettingsResponse>> updateSettings(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody NotificationSettingsRequest request) {
        NotificationSettingsResponse response = notificationService.updateSettings(principal.userId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<PageResponse<NotificationHistoryResponse>>> getHistory(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<NotificationHistoryResponse> page = notificationService.getHistory(pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)));
    }

    @PostMapping("/test")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> sendTest(
            @Valid @RequestBody NotificationTestRequest request) {
        boolean success = notificationService.sendTestNotification(request);
        return ResponseEntity.ok(ApiResponse.success(Map.of("sent", success)));
    }
}

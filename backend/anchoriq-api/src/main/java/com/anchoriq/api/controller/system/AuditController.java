package com.anchoriq.api.controller.system;

import com.anchoriq.api.annotation.RequiresPlan;
import com.anchoriq.api.global.response.ApiResponse;
import com.anchoriq.api.global.response.PageResponse;
import com.anchoriq.api.infrastructure.security.UserPrincipal;
import com.anchoriq.core.domain.account.subscription.model.Plan;
import com.anchoriq.core.domain.operation.audit.model.AuditLog;
import com.anchoriq.core.domain.operation.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping("/logs")
    @RequiresPlan(Plan.ENTERPRISE)
    public ResponseEntity<ApiResponse<PageResponse<AuditLog>>> getAuditLogs(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AuditLog> page = auditLogRepository.findByUserId(principal.userId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)));
    }
}

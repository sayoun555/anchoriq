package com.anchoriq.api.application.audit;

import com.anchoriq.core.domain.operation.audit.model.AuditAction;

public record AuditEvent(
        Long userId,
        AuditAction action,
        String resource,
        String detail,
        String ipAddress
) {}

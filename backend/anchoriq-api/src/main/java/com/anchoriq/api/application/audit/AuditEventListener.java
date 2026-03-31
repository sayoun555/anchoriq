package com.anchoriq.api.application.audit;

import com.anchoriq.core.domain.operation.audit.model.AuditLog;
import com.anchoriq.core.domain.operation.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditLogRepository auditLogRepository;

    @Async
    @EventListener
    public void handleAuditEvent(AuditEvent event) {
        try {
            AuditLog auditLog = AuditLog.create(
                    event.userId(),
                    event.action(),
                    event.resource(),
                    event.detail(),
                    event.ipAddress()
            );
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Failed to save audit log, ignoring: {}", e.getMessage());
        }
    }
}

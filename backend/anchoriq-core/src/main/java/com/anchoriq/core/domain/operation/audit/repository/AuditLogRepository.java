package com.anchoriq.core.domain.operation.audit.repository;

import com.anchoriq.core.domain.operation.audit.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogRepository {

    AuditLog save(AuditLog auditLog);

    Page<AuditLog> findByUserId(Long userId, Pageable pageable);
}

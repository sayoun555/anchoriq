package com.anchoriq.api.infrastructure.persistence.audit;

import com.anchoriq.core.domain.operation.audit.model.AuditLog;
import com.anchoriq.core.domain.operation.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AuditLogRepositoryImpl implements AuditLogRepository {

    private final JpaAuditLogRepository jpaAuditLogRepository;

    @Override
    public AuditLog save(AuditLog auditLog) {
        return jpaAuditLogRepository.save(auditLog);
    }

    @Override
    public Page<AuditLog> findByUserId(Long userId, Pageable pageable) {
        return jpaAuditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
}

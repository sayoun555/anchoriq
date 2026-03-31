package com.anchoriq.core.domain.operation.audit.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditAction action;

    @Column(length = 100)
    private String resource;

    @Column(columnDefinition = "jsonb")
    private String detail;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private AuditLog(Long userId, AuditAction action, String resource,
                     String detail, String ipAddress) {
        this.userId = userId;
        this.action = action;
        this.resource = resource;
        this.detail = detail;
        this.ipAddress = ipAddress;
        this.createdAt = LocalDateTime.now();
    }

    public static AuditLog create(Long userId, AuditAction action, String resource,
                                  String detail, String ipAddress) {
        return new AuditLog(userId, action, resource, detail, ipAddress);
    }
}

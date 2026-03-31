package com.anchoriq.core.domain.operation.workflow.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 워크플로우 — 리스크 이벤트에 따른 자동 액션 정의.
 * 비즈니스 로직: activate, deactivate, matchesEvent, isOwnedBy
 */
@Entity
@Table(name = "workflows")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Workflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "n8n_workflow_id", length = 50)
    private String n8nWorkflowId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trigger_condition", columnDefinition = "jsonb")
    private String triggerCondition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkflowStatus status;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Workflow(Long userId, String name, String triggerCondition, String n8nWorkflowId) {
        this.userId = userId;
        this.name = name;
        this.triggerCondition = triggerCondition;
        this.n8nWorkflowId = n8nWorkflowId;
        this.status = WorkflowStatus.INACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static Workflow create(Long userId, String name, String triggerCondition, String n8nWorkflowId) {
        return new Workflow(userId, name, triggerCondition, n8nWorkflowId);
    }

    /**
     * 워크플로우 활성화.
     */
    public void activate() {
        if (this.status == WorkflowStatus.ACTIVE) {
            throw new IllegalStateException("Workflow is already active");
        }
        this.status = WorkflowStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 워크플로우 비활성화.
     */
    public void deactivate() {
        if (this.status == WorkflowStatus.INACTIVE) {
            throw new IllegalStateException("Workflow is already inactive");
        }
        this.status = WorkflowStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 리스크 이벤트의 타입이 트리거 조건과 일치하는지 판단.
     * triggerCondition JSON에 riskLevel, alertType 등이 포함되어 있다.
     */
    public boolean matchesEvent(String eventType, String riskLevel) {
        if (!this.status.isActive()) {
            return false;
        }
        if (this.triggerCondition == null || this.triggerCondition.isBlank()) {
            return false;
        }
        return this.triggerCondition.contains(eventType)
                || this.triggerCondition.contains(riskLevel);
    }

    /**
     * 소유자 확인.
     */
    public boolean isOwnedBy(Long userId) {
        return this.userId.equals(userId);
    }

    /**
     * 워크플로우 정보 수정.
     */
    public void update(String name, String triggerCondition, String n8nWorkflowId) {
        this.name = name;
        this.triggerCondition = triggerCondition;
        this.n8nWorkflowId = n8nWorkflowId;
        this.updatedAt = LocalDateTime.now();
    }
}

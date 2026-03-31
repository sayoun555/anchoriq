package com.anchoriq.core.domain.operation.workflow.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 워크플로우 실행 기록 (불변 이력).
 */
@Entity
@Table(name = "workflow_executions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkflowExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_id", nullable = false)
    private Long workflowId;

    @Column(name = "trigger_event", columnDefinition = "jsonb")
    private String triggerEvent;

    @Column(nullable = false, length = 20)
    private String result;

    @Column(name = "executed_at", nullable = false, updatable = false)
    private LocalDateTime executedAt;

    private WorkflowExecution(Long workflowId, String triggerEvent, String result) {
        this.workflowId = workflowId;
        this.triggerEvent = triggerEvent;
        this.result = result;
        this.executedAt = LocalDateTime.now();
    }

    public static WorkflowExecution recordSuccess(Long workflowId, String triggerEvent) {
        return new WorkflowExecution(workflowId, triggerEvent, "SUCCESS");
    }

    public static WorkflowExecution recordFailure(Long workflowId, String triggerEvent) {
        return new WorkflowExecution(workflowId, triggerEvent, "FAILURE");
    }

    public boolean isSuccess() {
        return "SUCCESS".equals(this.result);
    }
}

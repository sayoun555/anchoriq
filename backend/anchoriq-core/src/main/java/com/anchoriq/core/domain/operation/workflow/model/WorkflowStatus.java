package com.anchoriq.core.domain.operation.workflow.model;

/**
 * 워크플로우 활성 상태.
 */
public enum WorkflowStatus {
    ACTIVE,
    INACTIVE;

    public boolean isActive() {
        return this == ACTIVE;
    }
}

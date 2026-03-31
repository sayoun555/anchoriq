package com.anchoriq.core.domain.operation.workflow.factory;

import com.anchoriq.core.domain.operation.workflow.model.Workflow;

/**
 * DDD Factory: 워크플로우 생성 팩토리.
 * 트리거 조건 검증 등 복잡한 생성 로직을 캡슐화한다.
 * Bean 등록은 DomainFactoryConfig에서 수행한다.
 */
public class WorkflowFactory {

    /**
     * 워크플로우를 생성한다.
     * 트리거 조건의 유효성을 검증한 후 Workflow 엔티티를 생성한다.
     */
    public Workflow createWorkflow(Long userId, String name,
                                    String triggerCondition, String n8nWorkflowId) {
        validateTriggerCondition(triggerCondition);

        return Workflow.create(userId, name, triggerCondition, n8nWorkflowId);
    }

    private void validateTriggerCondition(String condition) {
        if (condition == null || condition.isBlank()) {
            throw new IllegalArgumentException("Trigger condition must not be empty");
        }
        // 조건 문법 검증: JSON 형식이어야 한다
        if (!condition.trim().startsWith("{") && !condition.trim().startsWith("[")) {
            throw new IllegalArgumentException(
                    "Trigger condition must be a valid JSON object or array");
        }
    }
}

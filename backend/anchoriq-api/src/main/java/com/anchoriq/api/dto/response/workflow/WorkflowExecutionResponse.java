package com.anchoriq.api.dto.response.workflow;

import com.anchoriq.core.domain.operation.workflow.model.WorkflowExecution;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 워크플로우 실행 이력 응답 DTO.
 */
@Getter
@Builder
public class WorkflowExecutionResponse {

    private Long id;
    private Long workflowId;
    private String triggerEvent;
    private String result;
    private LocalDateTime executedAt;

    public static WorkflowExecutionResponse from(WorkflowExecution execution) {
        return WorkflowExecutionResponse.builder()
                .id(execution.getId())
                .workflowId(execution.getWorkflowId())
                .triggerEvent(execution.getTriggerEvent())
                .result(execution.getResult())
                .executedAt(execution.getExecutedAt())
                .build();
    }
}

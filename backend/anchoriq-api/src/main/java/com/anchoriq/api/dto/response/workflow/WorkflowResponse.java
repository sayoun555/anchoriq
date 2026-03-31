package com.anchoriq.api.dto.response.workflow;

import com.anchoriq.core.domain.operation.workflow.model.Workflow;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 워크플로우 응답 DTO.
 */
@Getter
@Builder
public class WorkflowResponse {

    private Long id;
    private String name;
    private String triggerCondition;
    private String n8nWorkflowId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static WorkflowResponse from(Workflow workflow) {
        return WorkflowResponse.builder()
                .id(workflow.getId())
                .name(workflow.getName())
                .triggerCondition(workflow.getTriggerCondition())
                .n8nWorkflowId(workflow.getN8nWorkflowId())
                .status(workflow.getStatus().name())
                .createdAt(workflow.getCreatedAt())
                .updatedAt(workflow.getUpdatedAt())
                .build();
    }
}

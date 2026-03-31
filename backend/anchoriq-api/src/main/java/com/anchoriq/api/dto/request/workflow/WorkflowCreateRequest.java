package com.anchoriq.api.dto.request.workflow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 워크플로우 생성 요청 DTO.
 */
@Getter
@NoArgsConstructor
public class WorkflowCreateRequest {

    @NotBlank(message = "Workflow name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    private String description;

    private String triggerType;

    private String triggerCondition;

    private String n8nWorkflowId;
}

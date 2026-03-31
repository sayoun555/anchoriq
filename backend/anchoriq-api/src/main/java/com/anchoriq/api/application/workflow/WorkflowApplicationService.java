package com.anchoriq.api.application.workflow;

import com.anchoriq.api.dto.request.workflow.WorkflowCreateRequest;
import com.anchoriq.api.dto.request.workflow.WorkflowUpdateRequest;
import com.anchoriq.api.dto.response.workflow.WorkflowExecutionResponse;
import com.anchoriq.api.dto.response.workflow.WorkflowResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 워크플로우 Application Service 인터페이스.
 */
public interface WorkflowApplicationService {

    WorkflowResponse createWorkflow(Long userId, WorkflowCreateRequest request);

    Page<WorkflowResponse> getWorkflows(Long userId, Pageable pageable);

    WorkflowResponse getWorkflow(Long userId, Long workflowId);

    WorkflowResponse updateWorkflow(Long userId, Long workflowId, WorkflowUpdateRequest request);

    void deleteWorkflow(Long userId, Long workflowId);

    WorkflowResponse activateWorkflow(Long userId, Long workflowId);

    WorkflowResponse deactivateWorkflow(Long userId, Long workflowId);

    Page<WorkflowExecutionResponse> getExecutionHistory(Long userId, Long workflowId, Pageable pageable);
}

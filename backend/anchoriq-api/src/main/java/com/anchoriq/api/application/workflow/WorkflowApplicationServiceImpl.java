package com.anchoriq.api.application.workflow;

import com.anchoriq.api.dto.request.workflow.WorkflowCreateRequest;
import com.anchoriq.api.dto.request.workflow.WorkflowUpdateRequest;
import com.anchoriq.api.dto.response.workflow.WorkflowExecutionResponse;
import com.anchoriq.api.dto.response.workflow.WorkflowResponse;
import com.anchoriq.core.common.exception.EntityNotFoundException;
import com.anchoriq.core.domain.operation.workflow.factory.WorkflowFactory;
import com.anchoriq.core.domain.operation.workflow.model.Workflow;
import com.anchoriq.core.domain.operation.workflow.repository.WorkflowExecutionRepository;
import com.anchoriq.core.domain.operation.workflow.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 워크플로우 Application Service 구현체.
 */
@Service
@RequiredArgsConstructor
public class WorkflowApplicationServiceImpl implements WorkflowApplicationService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowExecutionRepository executionRepository;
    private final WorkflowFactory workflowFactory;

    @Override
    @Transactional
    public WorkflowResponse createWorkflow(Long userId, WorkflowCreateRequest request) {
        Workflow workflow = workflowFactory.createWorkflow(
                userId,
                request.getName(),
                request.getTriggerCondition(),
                request.getN8nWorkflowId()
        );
        Workflow saved = workflowRepository.save(workflow);
        return WorkflowResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WorkflowResponse> getWorkflows(Long userId, Pageable pageable) {
        return workflowRepository.findByUserId(userId, pageable)
                .map(WorkflowResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowResponse getWorkflow(Long userId, Long workflowId) {
        Workflow workflow = findWorkflowOwnedBy(userId, workflowId);
        return WorkflowResponse.from(workflow);
    }

    @Override
    @Transactional
    public WorkflowResponse updateWorkflow(Long userId, Long workflowId, WorkflowUpdateRequest request) {
        Workflow workflow = findWorkflowOwnedBy(userId, workflowId);
        workflow.update(request.getName(), request.getTriggerCondition(), request.getN8nWorkflowId());
        Workflow saved = workflowRepository.save(workflow);
        return WorkflowResponse.from(saved);
    }

    @Override
    @Transactional
    public void deleteWorkflow(Long userId, Long workflowId) {
        Workflow workflow = findWorkflowOwnedBy(userId, workflowId);
        workflowRepository.deleteById(workflow.getId());
    }

    @Override
    @Transactional
    public WorkflowResponse activateWorkflow(Long userId, Long workflowId) {
        Workflow workflow = findWorkflowOwnedBy(userId, workflowId);
        workflow.activate();
        Workflow saved = workflowRepository.save(workflow);
        return WorkflowResponse.from(saved);
    }

    @Override
    @Transactional
    public WorkflowResponse deactivateWorkflow(Long userId, Long workflowId) {
        Workflow workflow = findWorkflowOwnedBy(userId, workflowId);
        workflow.deactivate();
        Workflow saved = workflowRepository.save(workflow);
        return WorkflowResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WorkflowExecutionResponse> getExecutionHistory(Long userId, Long workflowId, Pageable pageable) {
        findWorkflowOwnedBy(userId, workflowId);
        return executionRepository.findByWorkflowId(workflowId, pageable)
                .map(WorkflowExecutionResponse::from);
    }

    private Workflow findWorkflowOwnedBy(Long userId, Long workflowId) {
        Workflow workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new EntityNotFoundException("Workflow", workflowId.toString()));
        if (!workflow.isOwnedBy(userId)) {
            throw new EntityNotFoundException("Workflow", workflowId.toString());
        }
        return workflow;
    }
}

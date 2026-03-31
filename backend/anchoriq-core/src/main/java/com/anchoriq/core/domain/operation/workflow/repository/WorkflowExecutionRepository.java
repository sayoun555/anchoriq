package com.anchoriq.core.domain.operation.workflow.repository;

import com.anchoriq.core.domain.operation.workflow.model.WorkflowExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 워크플로우 실행 이력 저장소 인터페이스.
 */
public interface WorkflowExecutionRepository {

    WorkflowExecution save(WorkflowExecution execution);

    Page<WorkflowExecution> findByWorkflowId(Long workflowId, Pageable pageable);
}

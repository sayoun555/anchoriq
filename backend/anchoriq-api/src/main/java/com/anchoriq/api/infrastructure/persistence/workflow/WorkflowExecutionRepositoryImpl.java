package com.anchoriq.api.infrastructure.persistence.workflow;

import com.anchoriq.core.domain.operation.workflow.model.WorkflowExecution;
import com.anchoriq.core.domain.operation.workflow.repository.WorkflowExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * WorkflowExecutionRepository 구현체 — JPA 어댑터.
 */
@Repository
@RequiredArgsConstructor
public class WorkflowExecutionRepositoryImpl implements WorkflowExecutionRepository {

    private final JpaWorkflowExecutionRepository jpaRepository;

    @Override
    public WorkflowExecution save(WorkflowExecution execution) {
        return jpaRepository.save(execution);
    }

    @Override
    public Page<WorkflowExecution> findByWorkflowId(Long workflowId, Pageable pageable) {
        return jpaRepository.findByWorkflowIdOrderByExecutedAtDesc(workflowId, pageable);
    }
}

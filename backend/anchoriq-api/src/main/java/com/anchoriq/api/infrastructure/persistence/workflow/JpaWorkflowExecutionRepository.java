package com.anchoriq.api.infrastructure.persistence.workflow;

import com.anchoriq.core.domain.operation.workflow.model.WorkflowExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA 인터페이스 — workflow_executions 테이블.
 */
public interface JpaWorkflowExecutionRepository extends JpaRepository<WorkflowExecution, Long> {

    Page<WorkflowExecution> findByWorkflowIdOrderByExecutedAtDesc(Long workflowId, Pageable pageable);
}

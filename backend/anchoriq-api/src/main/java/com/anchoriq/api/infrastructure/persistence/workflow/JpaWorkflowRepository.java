package com.anchoriq.api.infrastructure.persistence.workflow;

import com.anchoriq.core.domain.operation.workflow.model.Workflow;
import com.anchoriq.core.domain.operation.workflow.model.WorkflowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA 인터페이스 — workflows 테이블.
 */
public interface JpaWorkflowRepository extends JpaRepository<Workflow, Long> {

    Page<Workflow> findByUserId(Long userId, Pageable pageable);

    List<Workflow> findByUserIdAndStatus(Long userId, WorkflowStatus status);

    List<Workflow> findByStatus(WorkflowStatus status);
}

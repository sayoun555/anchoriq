package com.anchoriq.core.domain.operation.workflow.repository;

import com.anchoriq.core.domain.operation.workflow.model.Workflow;
import com.anchoriq.core.domain.operation.workflow.model.WorkflowStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * 워크플로우 저장소 인터페이스.
 */
public interface WorkflowRepository {

    Workflow save(Workflow workflow);

    Optional<Workflow> findById(Long id);

    Page<Workflow> findByUserId(Long userId, Pageable pageable);

    List<Workflow> findByUserIdAndStatus(Long userId, WorkflowStatus status);

    List<Workflow> findActiveWorkflows();

    void deleteById(Long id);
}

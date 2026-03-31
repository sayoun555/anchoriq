package com.anchoriq.api.infrastructure.persistence.workflow;

import com.anchoriq.core.domain.operation.workflow.model.Workflow;
import com.anchoriq.core.domain.operation.workflow.model.WorkflowStatus;
import com.anchoriq.core.domain.operation.workflow.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * WorkflowRepository 구현체 — JPA 어댑터.
 */
@Repository
@RequiredArgsConstructor
public class WorkflowRepositoryImpl implements WorkflowRepository {

    private final JpaWorkflowRepository jpaRepository;

    @Override
    public Workflow save(Workflow workflow) {
        return jpaRepository.save(workflow);
    }

    @Override
    public Optional<Workflow> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Page<Workflow> findByUserId(Long userId, Pageable pageable) {
        return jpaRepository.findByUserId(userId, pageable);
    }

    @Override
    public List<Workflow> findByUserIdAndStatus(Long userId, WorkflowStatus status) {
        return jpaRepository.findByUserIdAndStatus(userId, status);
    }

    @Override
    public List<Workflow> findActiveWorkflows() {
        return jpaRepository.findByStatus(WorkflowStatus.ACTIVE);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}

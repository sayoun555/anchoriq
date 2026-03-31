package com.anchoriq.core.domain.operation.workflow.service;

import com.anchoriq.core.domain.operation.workflow.model.Workflow;

import java.util.List;

/**
 * 워크플로우 도메인 서비스 인터페이스 — 이벤트 매칭, 실행 결정.
 */
public interface WorkflowDomainService {

    /**
     * 활성 워크플로우 중 주어진 이벤트에 매칭되는 워크플로우 목록 반환.
     */
    List<Workflow> findMatchingWorkflows(List<Workflow> activeWorkflows,
                                         String eventType, String riskLevel);
}

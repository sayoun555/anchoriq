package com.anchoriq.core.domain.operation.workflow.service;

import com.anchoriq.core.domain.operation.workflow.model.Workflow;

import java.util.List;

/**
 * 워크플로우 도메인 서비스 — 이벤트 매칭 판단 로직.
 * Bean 등록은 DomainServiceConfig에서 수행한다.
 */
public class WorkflowDomainServiceImpl implements WorkflowDomainService {

    @Override
    public List<Workflow> findMatchingWorkflows(List<Workflow> activeWorkflows,
                                                String eventType, String riskLevel) {
        return activeWorkflows.stream()
                .filter(workflow -> workflow.matchesEvent(eventType, riskLevel))
                .toList();
    }
}

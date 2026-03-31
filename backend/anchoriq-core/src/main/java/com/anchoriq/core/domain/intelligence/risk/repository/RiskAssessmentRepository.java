package com.anchoriq.core.domain.intelligence.risk.repository;

import com.anchoriq.core.domain.intelligence.risk.model.RiskAssessment;

import java.util.List;
import java.util.Optional;

/**
 * RiskAssessment Repository 인터페이스.
 */
public interface RiskAssessmentRepository {

    RiskAssessment save(RiskAssessment assessment);

    Optional<RiskAssessment> findById(String id);

    List<RiskAssessment> findByTargetId(String targetId);

    List<RiskAssessment> findRecentAssessments(int limit);

    void deleteById(String id);
}

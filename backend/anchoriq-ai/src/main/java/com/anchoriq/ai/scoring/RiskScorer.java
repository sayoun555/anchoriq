package com.anchoriq.ai.scoring;

import com.anchoriq.core.domain.intelligence.risk.model.RiskScore;

/**
 * 리스크 스코어링 공통 인터페이스.
 */
public interface RiskScorer {

    RiskScore calculateScore(String targetId);

    String getTargetType();
}

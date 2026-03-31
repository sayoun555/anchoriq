package com.anchoriq.ai.scoring;

import com.anchoriq.core.common.exception.EntityNotFoundException;
import com.anchoriq.core.domain.intelligence.risk.model.RiskScore;
import com.anchoriq.core.domain.intelligence.risk.service.SupplyChainRiskService;
import com.anchoriq.core.domain.maritime.route.model.Chokepoint;
import com.anchoriq.core.domain.maritime.route.repository.ChokepointRepository;
import org.springframework.stereotype.Component;

/**
 * 초크포인트 리스크 스코어러.
 */
@Component
public class ChokepointRiskScorer implements RiskScorer {

    private final ChokepointRepository chokepointRepository;
    private final SupplyChainRiskService riskService;

    public ChokepointRiskScorer(ChokepointRepository chokepointRepository,
                                SupplyChainRiskService riskService) {
        this.chokepointRepository = chokepointRepository;
        this.riskService = riskService;
    }

    @Override
    public RiskScore calculateScore(String chokepointName) {
        Chokepoint chokepoint = chokepointRepository.findByName(chokepointName)
                .orElseThrow(() -> new EntityNotFoundException("Chokepoint", chokepointName));
        return riskService.assessChokepointRisk(chokepoint);
    }

    @Override
    public String getTargetType() {
        return "CHOKEPOINT";
    }
}

package com.anchoriq.ai.scoring;

import com.anchoriq.core.common.exception.EntityNotFoundException;
import com.anchoriq.core.domain.intelligence.risk.model.RiskScore;
import com.anchoriq.core.domain.intelligence.risk.service.SupplyChainRiskService;
import com.anchoriq.core.domain.maritime.port.model.Port;
import com.anchoriq.core.domain.maritime.port.repository.PortRepository;
import org.springframework.stereotype.Component;

/**
 * 항만 리스크 스코어러.
 */
@Component
public class PortRiskScorer implements RiskScorer {

    private final PortRepository portRepository;
    private final SupplyChainRiskService riskService;

    public PortRiskScorer(PortRepository portRepository,
                          SupplyChainRiskService riskService) {
        this.portRepository = portRepository;
        this.riskService = riskService;
    }

    @Override
    public RiskScore calculateScore(String locode) {
        Port port = portRepository.findByLocode(locode)
                .orElseThrow(() -> new EntityNotFoundException("Port", locode));
        return riskService.assessPortRisk(port);
    }

    @Override
    public String getTargetType() {
        return "PORT";
    }
}

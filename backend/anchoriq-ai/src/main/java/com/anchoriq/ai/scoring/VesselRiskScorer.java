package com.anchoriq.ai.scoring;

import com.anchoriq.core.common.exception.EntityNotFoundException;
import com.anchoriq.core.domain.intelligence.risk.model.RiskScore;
import com.anchoriq.core.domain.intelligence.risk.service.SupplyChainRiskService;
import com.anchoriq.core.domain.maritime.vessel.model.Vessel;
import com.anchoriq.core.domain.maritime.vessel.repository.VesselRepository;
import org.springframework.stereotype.Component;

/**
 * 선박 리스크 스코어러.
 * 국적, 제재, 항로, 초크포인트, 날씨를 종합하여 0~100 점수를 산출한다.
 */
@Component
public class VesselRiskScorer implements RiskScorer {

    private final VesselRepository vesselRepository;
    private final SupplyChainRiskService riskService;

    public VesselRiskScorer(VesselRepository vesselRepository,
                            SupplyChainRiskService riskService) {
        this.vesselRepository = vesselRepository;
        this.riskService = riskService;
    }

    @Override
    public RiskScore calculateScore(String vesselImo) {
        Vessel vessel = vesselRepository.findByImo(vesselImo)
                .orElseThrow(() -> new EntityNotFoundException("Vessel", vesselImo));
        return riskService.assessVesselRisk(vessel);
    }

    @Override
    public String getTargetType() {
        return "VESSEL";
    }
}

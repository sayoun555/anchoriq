package com.anchoriq.core.domain.intelligence.risk.service;

import com.anchoriq.core.domain.maritime.sanction.model.Sanction;
import com.anchoriq.core.domain.maritime.sanction.repository.SanctionRepository;
import com.anchoriq.core.domain.maritime.vessel.model.Vessel;
import com.anchoriq.core.domain.maritime.vessel.repository.VesselRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 제재 스크리닝 도메인 서비스 구현체.
 * Bean 등록은 DomainServiceConfig에서 수행한다.
 */
public class SanctionScreeningServiceImpl implements SanctionScreeningService {

    private final SanctionRepository sanctionRepository;
    private final VesselRepository vesselRepository;

    public SanctionScreeningServiceImpl(SanctionRepository sanctionRepository,
                                         VesselRepository vesselRepository) {
        this.sanctionRepository = sanctionRepository;
        this.vesselRepository = vesselRepository;
    }

    @Override
    public boolean isVesselSanctioned(String vesselImo) {
        Vessel vessel = vesselRepository.findByImo(vesselImo).orElse(null);
        if (vessel == null) {
            return false;
        }
        Set<String> sanctionedCountries = getSanctionedCountryCodes().stream()
                .collect(Collectors.toSet());
        return vessel.isRegisteredInSanctionedCountry(sanctionedCountries);
    }

    @Override
    public boolean isCompanySanctioned(String companyName) {
        return sanctionRepository.findActiveSanctions().stream()
                .anyMatch(sanction -> sanction.matches(companyName));
    }

    @Override
    public List<Vessel> findSanctionedVessels() {
        return vesselRepository.findSanctionedVessels();
    }

    @Override
    public List<String> getSanctionedCountryCodes() {
        return sanctionRepository.findActiveSanctions().stream()
                .map(Sanction::getTargetName)
                .distinct()
                .collect(Collectors.toList());
    }
}

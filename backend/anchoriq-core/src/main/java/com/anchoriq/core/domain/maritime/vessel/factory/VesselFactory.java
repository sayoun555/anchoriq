package com.anchoriq.core.domain.maritime.vessel.factory;

import com.anchoriq.core.common.exception.DuplicateException;
import com.anchoriq.core.domain.maritime.sanction.model.Sanction;
import com.anchoriq.core.domain.maritime.sanction.repository.SanctionRepository;
import com.anchoriq.core.domain.maritime.vessel.model.Flag;
import com.anchoriq.core.domain.maritime.vessel.model.Imo;
import com.anchoriq.core.domain.maritime.vessel.model.Mmsi;
import com.anchoriq.core.domain.maritime.vessel.model.Vessel;
import com.anchoriq.core.domain.maritime.vessel.model.VesselType;
import com.anchoriq.core.domain.maritime.vessel.repository.VesselRepository;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * DDD Factory: 선박 생성 팩토리.
 * IMO 중복 체크, 제재국 선박 탐지 등 복잡한 생성 로직을 캡슐화한다.
 * Bean 등록은 DomainFactoryConfig에서 수행한다.
 */
public class VesselFactory {

    private final VesselRepository vesselRepository;
    private final SanctionRepository sanctionRepository;

    public VesselFactory(VesselRepository vesselRepository,
                         SanctionRepository sanctionRepository) {
        this.vesselRepository = vesselRepository;
        this.sanctionRepository = sanctionRepository;
    }

    /**
     * 선박을 생성한다.
     * 1. IMO 중복 체크
     * 2. Entity 생성 (Builder 사용)
     * 3. 제재국 선박이면 SanctionedVesselDetectedEvent 발행
     */
    public Vessel createVessel(Imo imo, Mmsi mmsi, String name, Flag flag,
                                VesselType type, int deadweight, int buildYear) {
        // 1. IMO 중복 체크
        vesselRepository.findByImo(imo.value())
                .ifPresent(v -> {
                    throw new DuplicateException("Vessel with IMO " + imo.value() + " already exists");
                });

        // 2. Entity 생성
        Vessel vessel = Vessel.builder()
                .imo(imo)
                .mmsi(mmsi)
                .name(name)
                .flag(flag)
                .type(type)
                .deadweight(deadweight)
                .buildYear(buildYear)
                .build();

        // 3. 제재국 선박이면 이벤트 발행
        Set<String> sanctionedCodes = sanctionRepository.findActiveSanctions().stream()
                .map(Sanction::getTargetName)
                .collect(Collectors.toSet());

        if (vessel.isRegisteredInSanctionedCountry(sanctionedCodes)) {
            vessel.markAsSanctionedCountryVessel();
        }

        return vessel;
    }

    /**
     * 간단한 선박 생성 (최소 정보).
     */
    public Vessel createVessel(String imo, String mmsi, String name, String flag,
                                VesselType type) {
        return createVessel(
                Imo.of(imo), Mmsi.of(mmsi), name, Flag.of(flag),
                type, 0, 0);
    }
}

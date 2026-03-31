package com.anchoriq.core.domain.intelligence.risk.service;

import com.anchoriq.core.domain.maritime.vessel.model.Vessel;

import java.util.List;

/**
 * 제재 스크리닝 도메인 서비스.
 * 선박/회사를 제재 목록과 대조한다.
 */
public interface SanctionScreeningService {

    boolean isVesselSanctioned(String vesselImo);

    boolean isCompanySanctioned(String companyName);

    List<Vessel> findSanctionedVessels();

    List<String> getSanctionedCountryCodes();
}

package com.anchoriq.api.application.vessel;

import com.anchoriq.api.dto.response.vessel.VesselResponse;
import com.anchoriq.api.dto.response.vessel.VesselStatisticsResponse;
import com.anchoriq.core.domain.maritime.vessel.model.VesselType;

import java.util.List;

/**
 * 선박 조회 Application Service 인터페이스.
 */
public interface VesselQueryApplicationService {

    List<VesselResponse> findAll();

    List<VesselResponse> findAll(int page, int size);

    VesselResponse findByImo(String imo);

    List<VesselResponse> findByFlag(String flag);

    List<VesselResponse> findByType(VesselType type);

    List<VesselResponse> findSanctionedVessels();

    VesselStatisticsResponse getStatistics();

    List<VesselResponse> searchByName(String query);

    long count();
}

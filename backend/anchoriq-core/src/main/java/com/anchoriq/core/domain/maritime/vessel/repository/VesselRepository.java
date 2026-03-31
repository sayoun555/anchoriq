package com.anchoriq.core.domain.maritime.vessel.repository;

import com.anchoriq.core.domain.maritime.vessel.model.Vessel;
import com.anchoriq.core.domain.maritime.vessel.model.VesselType;

import java.util.List;
import java.util.Optional;

/**
 * Vessel Repository 인터페이스.
 * 구현체는 infrastructure 레이어에 위치한다.
 */
public interface VesselRepository {

    Optional<Vessel> findByImo(String imo);

    Optional<Vessel> findByMmsi(String mmsi);

    List<Vessel> findByType(VesselType type);

    List<Vessel> findByFlag(String flag);

    List<Vessel> findAll();

    List<Vessel> findAll(int page, int size);

    Vessel save(Vessel vessel);

    void deleteByImo(String imo);

    boolean existsByImo(String imo);

    long count();

    List<Vessel> findSanctionedVessels();

    List<Vessel> findByCompanyName(String companyName);

    List<Vessel> findByCountryCode(String countryCode);

    List<Vessel> searchByName(String query);
}

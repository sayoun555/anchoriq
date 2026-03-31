package com.anchoriq.core.domain.maritime.weather.repository;

import com.anchoriq.core.domain.maritime.weather.model.SeaZone;

import java.util.List;
import java.util.Optional;

/**
 * SeaZone Repository 인터페이스.
 */
public interface SeaZoneRepository {

    Optional<SeaZone> findByName(String name);

    List<SeaZone> findAll();

    List<SeaZone> findByCountry(String country);

    SeaZone save(SeaZone seaZone);

    long count();
}

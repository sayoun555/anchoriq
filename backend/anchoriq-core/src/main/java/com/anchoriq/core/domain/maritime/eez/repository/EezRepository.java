package com.anchoriq.core.domain.maritime.eez.repository;

import com.anchoriq.core.domain.maritime.eez.model.Eez;

import java.util.List;
import java.util.Optional;

/**
 * EEZ Repository 인터페이스.
 */
public interface EezRepository {

    Optional<Eez> findByName(String name);

    List<Eez> findAll();

    List<Eez> findByCountry(String isoCode);

    Eez save(Eez eez);

    long count();
}

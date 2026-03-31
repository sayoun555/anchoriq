package com.anchoriq.core.domain.maritime.route.repository;

import com.anchoriq.core.domain.maritime.route.model.Chokepoint;

import java.util.List;
import java.util.Optional;

/**
 * Chokepoint Repository 인터페이스.
 */
public interface ChokepointRepository {

    Optional<Chokepoint> findById(Long id);

    Optional<Chokepoint> findByName(String name);

    List<Chokepoint> findAll();

    List<Chokepoint> findHighRisk();

    Chokepoint save(Chokepoint chokepoint);

    long count();
}

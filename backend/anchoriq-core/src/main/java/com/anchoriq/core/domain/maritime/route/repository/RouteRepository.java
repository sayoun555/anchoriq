package com.anchoriq.core.domain.maritime.route.repository;

import com.anchoriq.core.domain.maritime.route.model.Route;

import java.util.List;
import java.util.Optional;

/**
 * Route Repository 인터페이스.
 */
public interface RouteRepository {

    Optional<Route> findById(Long id);

    Optional<Route> findByName(String name);

    List<Route> findAll();

    List<Route> findHighRiskRoutes();

    Route save(Route route);

    long count();
}

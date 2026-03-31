package com.anchoriq.api.infrastructure.persistence.neo4j;

import com.anchoriq.api.infrastructure.persistence.neo4j.mapper.Neo4jNodeMapper;
import com.anchoriq.core.domain.maritime.route.model.Route;
import com.anchoriq.core.domain.maritime.route.repository.RouteRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RouteRepositoryImpl implements RouteRepository {

    private final Neo4jRouteRepository neo4jRepository;
    private final Neo4jNodeMapper mapper;

    @Override
    public Route save(Route route) {
        var saved = neo4jRepository.save(mapper.toNode(route));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Route> findById(Long id) {
        return neo4jRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Route> findByName(String name) {
        return neo4jRepository.findByName(name).map(mapper::toDomain);
    }

    @Override
    public List<Route> findAll() {
        return neo4jRepository.findAll().stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<Route> findHighRiskRoutes() {
        return neo4jRepository.findAll().stream()
                .map(mapper::toDomain)
                .filter(Route::isHighRisk)
                .toList();
    }

    @Override
    public long count() {
        return neo4jRepository.count();
    }
}

package com.anchoriq.api.infrastructure.persistence.neo4j;

import com.anchoriq.api.infrastructure.persistence.neo4j.mapper.Neo4jNodeMapper;
import com.anchoriq.core.domain.maritime.route.model.Chokepoint;
import com.anchoriq.core.domain.maritime.route.repository.ChokepointRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ChokepointRepositoryImpl implements ChokepointRepository {

    private final Neo4jChokepointRepository neo4jRepository;
    private final Neo4jNodeMapper mapper;

    @Override
    public Chokepoint save(Chokepoint chokepoint) {
        var saved = neo4jRepository.save(mapper.toNode(chokepoint));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Chokepoint> findById(Long id) {
        return neo4jRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Chokepoint> findByName(String name) {
        return neo4jRepository.findByName(name).map(mapper::toDomain);
    }

    @Override
    public List<Chokepoint> findAll() {
        return neo4jRepository.findAll().stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<Chokepoint> findHighRisk() {
        return neo4jRepository.findHighRisk().stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public long count() {
        return neo4jRepository.count();
    }
}

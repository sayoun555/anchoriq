package com.anchoriq.api.infrastructure.persistence.neo4j;

import com.anchoriq.api.infrastructure.persistence.neo4j.mapper.Neo4jNodeMapper;
import com.anchoriq.core.domain.maritime.eez.model.Eez;
import com.anchoriq.core.domain.maritime.eez.repository.EezRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * EezRepository 구현체.
 * Neo4j를 통해 EEZ(배타적 경제수역) 데이터를 영속화한다.
 */
@Repository
@RequiredArgsConstructor
public class EezRepositoryImpl implements EezRepository {

    private final Neo4jEezRepository neo4jRepository;
    private final Neo4jNodeMapper mapper;

    @Override
    public Optional<Eez> findByName(String name) {
        return neo4jRepository.findByName(name).map(mapper::toDomain);
    }

    @Override
    public List<Eez> findAll() {
        return neo4jRepository.findAll().stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<Eez> findByCountry(String isoCode) {
        return neo4jRepository.findByIsoCode(isoCode).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public Eez save(Eez eez) {
        var saved = neo4jRepository.save(mapper.toNode(eez));
        return mapper.toDomain(saved);
    }

    @Override
    public long count() {
        return neo4jRepository.count();
    }
}

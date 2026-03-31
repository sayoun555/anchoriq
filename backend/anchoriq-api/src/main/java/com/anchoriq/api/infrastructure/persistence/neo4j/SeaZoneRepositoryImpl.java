package com.anchoriq.api.infrastructure.persistence.neo4j;

import com.anchoriq.api.infrastructure.persistence.neo4j.mapper.Neo4jNodeMapper;
import com.anchoriq.core.domain.maritime.weather.model.SeaZone;
import com.anchoriq.core.domain.maritime.weather.repository.SeaZoneRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SeaZoneRepositoryImpl implements SeaZoneRepository {

    private final Neo4jSeaZoneRepository neo4jRepository;
    private final Neo4jNodeMapper mapper;

    @Override
    public Optional<SeaZone> findByName(String name) {
        return neo4jRepository.findByName(name).map(mapper::toDomain);
    }

    @Override
    public List<SeaZone> findAll() {
        return neo4jRepository.findAll().stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<SeaZone> findByCountry(String country) {
        return neo4jRepository.findByCountry(country).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public SeaZone save(SeaZone seaZone) {
        var saved = neo4jRepository.save(mapper.toNode(seaZone));
        return mapper.toDomain(saved);
    }

    @Override
    public long count() {
        return neo4jRepository.count();
    }
}

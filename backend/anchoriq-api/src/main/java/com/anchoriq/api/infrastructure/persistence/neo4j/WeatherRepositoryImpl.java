package com.anchoriq.api.infrastructure.persistence.neo4j;

import com.anchoriq.api.infrastructure.persistence.neo4j.mapper.Neo4jNodeMapper;
import com.anchoriq.core.domain.maritime.weather.model.WeatherCondition;
import com.anchoriq.core.domain.maritime.weather.repository.WeatherRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class WeatherRepositoryImpl implements WeatherRepository {

    private final Neo4jWeatherRepository neo4jRepository;
    private final Neo4jNodeMapper mapper;

    @Override
    public List<WeatherCondition> findAll() {
        return neo4jRepository.findAll().stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<WeatherCondition> findSevereConditions() {
        return neo4jRepository.findSevereConditions().stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public Optional<WeatherCondition> findById(Long id) {
        return neo4jRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public WeatherCondition save(WeatherCondition condition) {
        var saved = neo4jRepository.save(mapper.toNode(condition));
        return mapper.toDomain(saved);
    }

    @Override
    public long count() {
        return neo4jRepository.count();
    }
}

package com.anchoriq.api.infrastructure.persistence.neo4j;

import com.anchoriq.api.infrastructure.persistence.neo4j.mapper.Neo4jNodeMapper;
import com.anchoriq.core.domain.intelligence.anomaly.model.AnomalyDetection;
import com.anchoriq.core.domain.intelligence.anomaly.model.AnomalyType;
import com.anchoriq.core.domain.intelligence.anomaly.repository.AnomalyRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * AnomalyRepository 구현체.
 * Neo4j를 통해 이상 탐지 기록을 영속화한다.
 */
@Repository
@RequiredArgsConstructor
public class AnomalyRepositoryImpl implements AnomalyRepository {

    private final Neo4jAnomalyDetectionRepository neo4jRepository;
    private final Neo4jNodeMapper mapper;

    @Override
    public AnomalyDetection save(AnomalyDetection anomaly) {
        var saved = neo4jRepository.save(mapper.toNode(anomaly));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<AnomalyDetection> findById(String id) {
        return neo4jRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<AnomalyDetection> findByType(AnomalyType type) {
        return neo4jRepository.findByType(type.name()).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<AnomalyDetection> findByVesselImo(String vesselImo) {
        return neo4jRepository.findByVesselImo(vesselImo).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<AnomalyDetection> findUnresolved() {
        return neo4jRepository.findUnresolved().stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<AnomalyDetection> findRecentByType(AnomalyType type, int limit) {
        return neo4jRepository.findRecentByType(type.name(), limit).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public long countByType(AnomalyType type) {
        return neo4jRepository.countByType(type.name());
    }
}

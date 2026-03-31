package com.anchoriq.api.infrastructure.persistence.neo4j;

import com.anchoriq.api.infrastructure.persistence.neo4j.mapper.Neo4jNodeMapper;
import com.anchoriq.core.domain.maritime.sanction.model.Sanction;
import com.anchoriq.core.domain.maritime.sanction.repository.SanctionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SanctionRepositoryImpl implements SanctionRepository {

    private final Neo4jSanctionRepository neo4jRepository;
    private final Neo4jNodeMapper mapper;

    @Override
    public Optional<Sanction> findByReferenceNumber(String referenceNumber) {
        return neo4jRepository.findByReferenceNumber(referenceNumber).map(mapper::toDomain);
    }

    @Override
    public List<Sanction> findAll() {
        return neo4jRepository.findAll().stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<Sanction> findActiveSanctions() {
        return neo4jRepository.findActiveSanctions().stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<Sanction> findByTargetNameContaining(String query) {
        return neo4jRepository.findByTargetNameContaining(query).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public Sanction save(Sanction sanction) {
        var saved = neo4jRepository.save(mapper.toNode(sanction));
        return mapper.toDomain(saved);
    }

    @Override
    public long count() {
        return neo4jRepository.count();
    }
}

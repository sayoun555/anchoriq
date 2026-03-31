package com.anchoriq.api.infrastructure.persistence.neo4j;

import com.anchoriq.api.infrastructure.persistence.neo4j.mapper.Neo4jNodeMapper;
import com.anchoriq.core.domain.maritime.port.model.Port;
import com.anchoriq.core.domain.maritime.port.repository.PortRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PortRepositoryImpl implements PortRepository {

    private final Neo4jPortRepository neo4jRepository;
    private final Neo4jNodeMapper mapper;

    @Override
    public Optional<Port> findByLocode(String locode) {
        return neo4jRepository.findByLocode(locode).map(mapper::toDomain);
    }

    @Override
    public List<Port> findByCountry(String country) {
        return neo4jRepository.findByCountry(country).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<Port> findAll() {
        return neo4jRepository.findAll().stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<Port> findAll(int page, int size) {
        return neo4jRepository.findAll(PageRequest.of(page, size)).getContent().stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<Port> findCongestedPorts(double minCongestion) {
        return neo4jRepository.findCongestedPorts(minCongestion).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<Port> findTopCongestedPorts(int limit) {
        return neo4jRepository.findTopCongestedPorts(limit).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public Port save(Port port) {
        var saved = neo4jRepository.save(mapper.toNode(port));
        return mapper.toDomain(saved);
    }

    @Override
    public boolean existsByLocode(String locode) {
        return neo4jRepository.existsByLocode(locode);
    }

    @Override
    public long count() {
        return neo4jRepository.count();
    }

    @Override
    public List<Port> searchByName(String query) {
        return neo4jRepository.searchByName(query).stream()
                .map(mapper::toDomain).toList();
    }
}

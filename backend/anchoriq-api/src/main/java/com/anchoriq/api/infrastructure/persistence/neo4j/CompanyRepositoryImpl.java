package com.anchoriq.api.infrastructure.persistence.neo4j;

import com.anchoriq.api.infrastructure.persistence.neo4j.mapper.Neo4jNodeMapper;
import com.anchoriq.core.domain.maritime.company.model.Company;
import com.anchoriq.core.domain.maritime.company.repository.CompanyRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CompanyRepositoryImpl implements CompanyRepository {

    private final Neo4jCompanyRepository neo4jRepository;
    private final Neo4jNodeMapper mapper;

    @Override
    public Optional<Company> findByName(String name) {
        return neo4jRepository.findByName(name).map(mapper::toDomain);
    }

    @Override
    public List<Company> findAll() {
        return neo4jRepository.findAll().stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public Company save(Company company) {
        var saved = neo4jRepository.save(mapper.toNode(company));
        return mapper.toDomain(saved);
    }

    @Override
    public boolean existsByName(String name) {
        return neo4jRepository.existsByName(name);
    }

    @Override
    public long count() {
        return neo4jRepository.count();
    }
}

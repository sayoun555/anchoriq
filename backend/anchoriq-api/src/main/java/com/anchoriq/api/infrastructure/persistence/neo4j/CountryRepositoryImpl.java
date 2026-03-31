package com.anchoriq.api.infrastructure.persistence.neo4j;

import com.anchoriq.api.infrastructure.persistence.neo4j.mapper.Neo4jNodeMapper;
import com.anchoriq.core.domain.maritime.country.model.Country;
import com.anchoriq.core.domain.maritime.country.repository.CountryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CountryRepositoryImpl implements CountryRepository {

    private final Neo4jCountryRepository neo4jRepository;
    private final Neo4jNodeMapper mapper;

    @Override
    public Optional<Country> findByIsoCode(String isoCode) {
        return neo4jRepository.findByIsoCode(isoCode).map(mapper::toDomain);
    }

    @Override
    public List<Country> findAll() {
        return neo4jRepository.findAll().stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<Country> findSanctionedCountries() {
        return neo4jRepository.findSanctionedCountries().stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public Set<String> findSanctionedCountryCodes() {
        return neo4jRepository.findSanctionedCountryCodes();
    }

    @Override
    public Country save(Country country) {
        var saved = neo4jRepository.save(mapper.toNode(country));
        return mapper.toDomain(saved);
    }

    @Override
    public boolean existsByIsoCode(String isoCode) {
        return neo4jRepository.existsByIsoCode(isoCode);
    }

    @Override
    public long count() {
        return neo4jRepository.count();
    }
}

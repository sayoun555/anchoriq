package com.anchoriq.api.infrastructure.persistence.neo4j;

import com.anchoriq.api.infrastructure.persistence.neo4j.mapper.Neo4jNodeMapper;
import com.anchoriq.core.domain.maritime.vessel.model.Vessel;
import com.anchoriq.core.domain.maritime.vessel.model.VesselType;
import com.anchoriq.core.domain.maritime.vessel.repository.VesselRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class VesselRepositoryImpl implements VesselRepository {

    private final Neo4jVesselRepository neo4jRepository;
    private final Neo4jNodeMapper mapper;

    @Override
    public Optional<Vessel> findByImo(String imo) {
        return neo4jRepository.findByImo(imo).map(mapper::toDomain);
    }

    @Override
    public Optional<Vessel> findByMmsi(String mmsi) {
        return neo4jRepository.findByMmsi(mmsi).map(mapper::toDomain);
    }

    @Override
    public List<Vessel> findByType(VesselType type) {
        return neo4jRepository.findByType(type.name()).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<Vessel> findByFlag(String flag) {
        return neo4jRepository.findByFlag(flag).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<Vessel> findAll() {
        return neo4jRepository.findAll().stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<Vessel> findAll(int page, int size) {
        return neo4jRepository.findAll(PageRequest.of(page, size)).getContent().stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public Vessel save(Vessel vessel) {
        var saved = neo4jRepository.save(mapper.toNode(vessel));
        return mapper.toDomain(saved);
    }

    @Override
    public void deleteByImo(String imo) {
        neo4jRepository.deleteByImo(imo);
    }

    @Override
    public boolean existsByImo(String imo) {
        return neo4jRepository.existsByImo(imo);
    }

    @Override
    public long count() {
        return neo4jRepository.count();
    }

    @Override
    public List<Vessel> findSanctionedVessels() {
        return neo4jRepository.findSanctionedVessels().stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<Vessel> findByCompanyName(String companyName) {
        return neo4jRepository.findByCompanyName(companyName).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<Vessel> findByCountryCode(String countryCode) {
        return neo4jRepository.findByCountryCode(countryCode).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<Vessel> searchByName(String query) {
        return neo4jRepository.searchByName(query).stream()
                .map(mapper::toDomain).toList();
    }
}

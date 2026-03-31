package com.anchoriq.api.infrastructure.persistence.neo4j.mapper;

import com.anchoriq.api.infrastructure.persistence.neo4j.node.*;
import com.anchoriq.core.common.vo.Coordinate;
import com.anchoriq.core.domain.intelligence.anomaly.model.AnomalyDetection;
import com.anchoriq.core.domain.intelligence.anomaly.model.AnomalyType;
import com.anchoriq.core.domain.maritime.company.model.Company;
import com.anchoriq.core.domain.maritime.country.model.Country;
import com.anchoriq.core.domain.maritime.eez.model.Eez;
import com.anchoriq.core.domain.maritime.port.model.CongestionLevel;
import com.anchoriq.core.domain.maritime.port.model.Locode;
import com.anchoriq.core.domain.maritime.port.model.Port;
import com.anchoriq.core.domain.maritime.route.model.Chokepoint;
import com.anchoriq.core.domain.maritime.route.model.Route;
import com.anchoriq.core.domain.maritime.sanction.model.Sanction;
import com.anchoriq.core.domain.maritime.country.model.IsoCountryCode;
import com.anchoriq.core.domain.maritime.route.model.Chokepoints;
import com.anchoriq.core.domain.maritime.vessel.model.*;
import com.anchoriq.core.domain.maritime.weather.model.SeaZone;
import com.anchoriq.core.domain.maritime.weather.model.WeatherCondition;
import com.anchoriq.core.domain.maritime.weather.model.WeatherType;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Domain Entity <-> Neo4j Node 변환 Mapper.
 * infrastructure 레이어에서 도메인 객체와 Neo4j 매핑 객체 간 변환을 수행한다.
 */
@Component
public class Neo4jNodeMapper {

    // === Vessel ===

    public Vessel toDomain(Neo4jVesselNode node) {
        if (node == null) return null;
        return Vessel.reconstitute(
                node.getId(),
                node.getImo() != null && !node.getImo().isBlank() ? Imo.of(node.getImo()) : null,
                Mmsi.of(node.getMmsi()),
                node.getName(),
                node.getFlag() != null && !node.getFlag().isBlank() ? Flag.of(node.getFlag()) : null,
                VesselType.valueOf(node.getType()),
                VesselStatus.valueOf(node.getStatus()),
                node.getDeadweight(),
                node.getBuildYear(),
                toDomain(node.getCompany()),
                node.getLastUpdated(),
                node.getRiskScore());
    }

    public Neo4jVesselNode toNode(Vessel vessel) {
        if (vessel == null) return null;
        Neo4jVesselNode node = new Neo4jVesselNode();
        node.setId(vessel.getId());
        node.setImo(vessel.getImo() != null ? vessel.getImo().value() : null);
        node.setMmsi(vessel.getMmsi().value());
        node.setName(vessel.getName());
        node.setFlag(vessel.getFlag() != null ? vessel.getFlag().value() : null);
        node.setType(vessel.getType().name());
        node.setStatus(vessel.getStatus().name());
        node.setDeadweight(vessel.getDeadweight());
        node.setBuildYear(vessel.getBuildYear());
        node.setRiskScore(vessel.getRiskScore());
        node.setLastUpdated(vessel.getLastUpdated());
        node.setCompany(toNode(vessel.getCompany()));
        return node;
    }

    // === Company ===

    public Company toDomain(Neo4jCompanyNode node) {
        if (node == null) return null;
        Country country = toDomain(node.getCountry());
        return Company.reconstitute(node.getId(), node.getName(), node.getRegistrationNumber(), country);
    }

    public Neo4jCompanyNode toNode(Company company) {
        if (company == null) return null;
        Neo4jCompanyNode node = new Neo4jCompanyNode();
        node.setId(company.getId());
        node.setName(company.getName());
        node.setRegistrationNumber(company.getRegistrationNumber());
        node.setCountry(toNode(company.getCountry()));
        return node;
    }

    // === Country ===

    public Country toDomain(Neo4jCountryNode node) {
        if (node == null) return null;
        return Country.reconstitute(
                node.getId(),
                IsoCountryCode.of(node.getIsoCode()),
                node.getName(),
                node.getRegion(),
                node.isSanctioned());
    }

    public Neo4jCountryNode toNode(Country country) {
        if (country == null) return null;
        Neo4jCountryNode node = new Neo4jCountryNode();
        node.setId(country.getId());
        node.setIsoCode(country.getIsoCodeValue());
        node.setName(country.getName());
        node.setRegion(country.getRegion());
        node.setSanctioned(country.isSanctioned());
        return node;
    }

    // === Port ===

    public Port toDomain(Neo4jPortNode node) {
        if (node == null) return null;
        return Port.reconstitute(
                node.getId(),
                Locode.of(node.getLocode()),
                node.getName(),
                node.getCountry(),
                Coordinate.of(node.getLatitude(), node.getLongitude()),
                CongestionLevel.of(node.getCongestionLevel()),
                node.getVesselCount(),
                node.getLastUpdated());
    }

    public Neo4jPortNode toNode(Port port) {
        if (port == null) return null;
        Neo4jPortNode node = new Neo4jPortNode();
        node.setId(port.getId());
        node.setLocode(port.getLocodeValue());
        node.setName(port.getName());
        node.setCountry(port.getCountry());
        node.setLatitude(port.getLatitude());
        node.setLongitude(port.getLongitude());
        node.setCongestionLevel(port.getCongestionValue());
        node.setVesselCount(port.getVesselCount());
        node.setLastUpdated(port.getLastUpdated());
        return node;
    }

    // === Route ===

    public Route toDomain(Neo4jRouteNode node) {
        if (node == null) return null;
        return Route.reconstitute(
                node.getId(),
                node.getName(),
                node.getDisplayName(),
                node.getDistanceNm(),
                Chokepoints.of(node.getChokepoints().stream()
                        .map(this::toDomain)
                        .toList()));
    }

    public Neo4jRouteNode toNode(Route route) {
        if (route == null) return null;
        Neo4jRouteNode node = new Neo4jRouteNode();
        node.setId(route.getId());
        node.setName(route.getName());
        node.setDisplayName(route.getDisplayName());
        node.setDistanceNm(route.getDistanceNm());
        node.setUnit(route.getUnit());
        node.setChokepoints(route.getChokepoints().getValues().stream()
                .map(this::toNode)
                .collect(Collectors.toList()));
        return node;
    }

    // === Chokepoint ===

    public Chokepoint toDomain(Neo4jChokepointNode node) {
        if (node == null) return null;
        return Chokepoint.reconstitute(
                node.getId(),
                node.getName(),
                node.getDisplayName(),
                Coordinate.of(node.getLatitude(), node.getLongitude()),
                node.getRiskLevel(),
                node.getDescription(),
                node.getTransitVolume());
    }

    public Neo4jChokepointNode toNode(Chokepoint cp) {
        if (cp == null) return null;
        Neo4jChokepointNode node = new Neo4jChokepointNode();
        node.setId(cp.getId());
        node.setName(cp.getName());
        node.setDisplayName(cp.getDisplayName());
        node.setLatitude(cp.getLatitude());
        node.setLongitude(cp.getLongitude());
        node.setRiskLevel(cp.getRiskLevel());
        node.setDescription(cp.getDescription());
        node.setTransitVolume(cp.getTransitVolume());
        return node;
    }

    // === Sanction ===

    public Sanction toDomain(Neo4jSanctionNode node) {
        if (node == null) return null;
        return Sanction.reconstitute(
                node.getId(),
                node.getReferenceNumber(),
                node.getTargetName(),
                node.getType(),
                node.getSource(),
                node.isActive(),
                node.getStartDate(),
                node.getEndDate(),
                node.getDescription());
    }

    public Neo4jSanctionNode toNode(Sanction sanction) {
        if (sanction == null) return null;
        Neo4jSanctionNode node = new Neo4jSanctionNode();
        node.setId(sanction.getId());
        node.setReferenceNumber(sanction.getReferenceNumber());
        node.setTargetName(sanction.getTargetName());
        node.setType(sanction.getType());
        node.setSource(sanction.getSource());
        node.setActive(sanction.isActive());
        node.setStartDate(sanction.getStartDate());
        node.setEndDate(sanction.getEndDate());
        node.setDescription(sanction.getDescription());
        return node;
    }

    // === WeatherCondition ===

    public WeatherCondition toDomain(Neo4jWeatherConditionNode node) {
        if (node == null) return null;
        return WeatherCondition.reconstitute(
                node.getId(),
                WeatherType.valueOf(node.getType()),
                node.getSeverity(),
                Coordinate.of(node.getLatitude(), node.getLongitude()),
                node.getTimestamp(),
                node.getDescription());
    }

    public Neo4jWeatherConditionNode toNode(WeatherCondition wc) {
        if (wc == null) return null;
        Neo4jWeatherConditionNode node = new Neo4jWeatherConditionNode();
        node.setId(wc.getId());
        node.setType(wc.getType().name());
        node.setSeverity(wc.getSeverity());
        node.setLatitude(wc.getLatitude());
        node.setLongitude(wc.getLongitude());
        node.setTimestamp(wc.getTimestamp());
        node.setDescription(wc.getDescription());
        return node;
    }

    // === SeaZone ===

    public SeaZone toDomain(Neo4jSeaZoneNode node) {
        if (node == null) return null;
        return SeaZone.reconstitute(
                node.getId(),
                node.getName(),
                node.getType(),
                node.getCountry(),
                Coordinate.of(node.getLatitude(), node.getLongitude()));
    }

    public Neo4jSeaZoneNode toNode(SeaZone sz) {
        if (sz == null) return null;
        Neo4jSeaZoneNode node = new Neo4jSeaZoneNode();
        node.setId(sz.getId());
        node.setName(sz.getName());
        node.setType(sz.getType());
        node.setCountry(sz.getCountry());
        node.setLatitude(sz.getLatitude());
        node.setLongitude(sz.getLongitude());
        return node;
    }

    // === Eez ===

    public Eez toDomain(Neo4jEezNode node) {
        if (node == null) return null;
        IsoCountryCode isoCode = (node.getIsoCode() != null && !node.getIsoCode().isBlank())
                ? IsoCountryCode.of(node.getIsoCode()) : null;
        return Eez.reconstitute(
                node.getId(),
                node.getName(),
                node.getCountry(),
                isoCode,
                node.getAreaKm2());
    }

    public Neo4jEezNode toNode(Eez eez) {
        if (eez == null) return null;
        Neo4jEezNode node = new Neo4jEezNode();
        node.setId(eez.getId());
        node.setName(eez.getName());
        node.setCountry(eez.getCountry());
        node.setIsoCode(eez.getIsoCodeValue());
        node.setAreaKm2(eez.getAreaKm2());
        return node;
    }

    // === AnomalyDetection ===

    public AnomalyDetection toDomain(Neo4jAnomalyDetectionNode node) {
        if (node == null) return null;
        return AnomalyDetection.reconstitute(
                node.getAnomalyId(),
                AnomalyType.valueOf(node.getType()),
                node.getVesselImo(),
                node.getVesselName(),
                node.getDescription(),
                node.getSeverity(),
                node.getLatitude(),
                node.getLongitude(),
                node.getDetectedAt(),
                node.isResolved(),
                node.getResolvedAt());
    }

    public Neo4jAnomalyDetectionNode toNode(AnomalyDetection anomaly) {
        if (anomaly == null) return null;
        Neo4jAnomalyDetectionNode node = new Neo4jAnomalyDetectionNode();
        node.setAnomalyId(anomaly.getId());
        node.setType(anomaly.getType().name());
        node.setVesselImo(anomaly.getVesselImo());
        node.setVesselName(anomaly.getVesselName());
        node.setDescription(anomaly.getDescription());
        node.setSeverity(anomaly.getSeverity());
        node.setLatitude(anomaly.getLatitude());
        node.setLongitude(anomaly.getLongitude());
        node.setDetectedAt(anomaly.getDetectedAt());
        node.setResolved(anomaly.isResolved());
        node.setResolvedAt(anomaly.getResolvedAt());
        return node;
    }
}

package com.anchoriq.core.domain.intelligence.anomaly.service;

import com.anchoriq.core.domain.intelligence.anomaly.model.AnomalyDetection;
import com.anchoriq.core.domain.intelligence.anomaly.model.AnomalyType;
import com.anchoriq.core.domain.intelligence.anomaly.repository.AnomalyRepository;

import java.util.List;

/**
 * 이상 탐지 도메인 서비스 구현체.
 * Bean 등록은 DomainServiceConfig에서 수행한다.
 */
public class AnomalyDetectionServiceImpl implements AnomalyDetectionService {

    private final AnomalyRepository anomalyRepository;

    public AnomalyDetectionServiceImpl(AnomalyRepository anomalyRepository) {
        this.anomalyRepository = anomalyRepository;
    }

    @Override
    public List<AnomalyDetection> findByType(AnomalyType type) {
        return anomalyRepository.findByType(type);
    }

    @Override
    public List<AnomalyDetection> findAisOffVessels() {
        return anomalyRepository.findRecentByType(AnomalyType.AIS_OFF, 50);
    }

    @Override
    public List<AnomalyDetection> findRouteDeviations() {
        return anomalyRepository.findRecentByType(AnomalyType.ROUTE_DEVIATION, 50);
    }

    @Override
    public List<AnomalyDetection> findSpeedAnomalies() {
        return anomalyRepository.findRecentByType(AnomalyType.SPEED_CHANGE, 50);
    }

    @Override
    public List<AnomalyDetection> findDarkShips() {
        return anomalyRepository.findRecentByType(AnomalyType.DARK_SHIP, 50);
    }

    @Override
    public AnomalyDetection recordAnomaly(AnomalyType type, String vesselImo,
                                            String vesselName, String description,
                                            String severity, double latitude, double longitude) {
        AnomalyDetection anomaly = AnomalyDetection.create(
                type, vesselImo, vesselName, description, severity, latitude, longitude);
        return anomalyRepository.save(anomaly);
    }
}

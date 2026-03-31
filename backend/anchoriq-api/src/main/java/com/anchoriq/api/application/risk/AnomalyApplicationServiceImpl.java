package com.anchoriq.api.application.risk;

import com.anchoriq.core.domain.intelligence.anomaly.model.AnomalyDetection;
import com.anchoriq.core.domain.intelligence.anomaly.service.AnomalyDetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 이상 탐지 Application Service 구현체.
 */
@Service
@RequiredArgsConstructor
public class AnomalyApplicationServiceImpl implements AnomalyApplicationService {

    private final AnomalyDetectionService anomalyDetectionService;

    @Override
    public List<AnomalyDetection> getAisOffVessels() {
        return anomalyDetectionService.findAisOffVessels();
    }

    @Override
    public List<AnomalyDetection> getRouteDeviations() {
        return anomalyDetectionService.findRouteDeviations();
    }

    @Override
    public List<AnomalyDetection> getSpeedAnomalies() {
        return anomalyDetectionService.findSpeedAnomalies();
    }

    @Override
    public List<AnomalyDetection> getDarkShips() {
        return anomalyDetectionService.findDarkShips();
    }
}

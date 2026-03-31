package com.anchoriq.api.application.risk;

import com.anchoriq.core.domain.intelligence.anomaly.model.AnomalyDetection;

import java.util.List;

/**
 * 이상 탐지 Application Service 인터페이스.
 */
public interface AnomalyApplicationService {

    List<AnomalyDetection> getAisOffVessels();

    List<AnomalyDetection> getRouteDeviations();

    List<AnomalyDetection> getSpeedAnomalies();

    List<AnomalyDetection> getDarkShips();
}

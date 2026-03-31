package com.anchoriq.core.domain.intelligence.anomaly.service;

import com.anchoriq.core.domain.intelligence.anomaly.model.AnomalyDetection;
import com.anchoriq.core.domain.intelligence.anomaly.model.AnomalyType;

import java.util.List;

/**
 * 이상 탐지 도메인 서비스.
 * AIS 소실, 항로 이탈, 속도 이상, 다크 쉽을 탐지한다.
 */
public interface AnomalyDetectionService {

    List<AnomalyDetection> findByType(AnomalyType type);

    List<AnomalyDetection> findAisOffVessels();

    List<AnomalyDetection> findRouteDeviations();

    List<AnomalyDetection> findSpeedAnomalies();

    List<AnomalyDetection> findDarkShips();

    AnomalyDetection recordAnomaly(AnomalyType type, String vesselImo,
                                    String vesselName, String description,
                                    String severity, double latitude, double longitude);
}

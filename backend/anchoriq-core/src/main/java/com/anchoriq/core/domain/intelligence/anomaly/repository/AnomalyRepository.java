package com.anchoriq.core.domain.intelligence.anomaly.repository;

import com.anchoriq.core.domain.intelligence.anomaly.model.AnomalyDetection;
import com.anchoriq.core.domain.intelligence.anomaly.model.AnomalyType;

import java.util.List;
import java.util.Optional;

/**
 * Anomaly Repository 인터페이스.
 */
public interface AnomalyRepository {

    AnomalyDetection save(AnomalyDetection anomaly);

    Optional<AnomalyDetection> findById(String id);

    List<AnomalyDetection> findByType(AnomalyType type);

    List<AnomalyDetection> findByVesselImo(String vesselImo);

    List<AnomalyDetection> findUnresolved();

    List<AnomalyDetection> findRecentByType(AnomalyType type, int limit);

    long countByType(AnomalyType type);
}

package com.anchoriq.core.domain.maritime.port.repository;

import com.anchoriq.core.domain.maritime.port.model.Port;

import java.util.List;
import java.util.Optional;

/**
 * Port Repository 인터페이스.
 */
public interface PortRepository {

    Optional<Port> findByLocode(String locode);

    List<Port> findByCountry(String country);

    List<Port> findAll();

    List<Port> findAll(int page, int size);

    List<Port> findCongestedPorts(double minCongestion);

    List<Port> findTopCongestedPorts(int limit);

    Port save(Port port);

    boolean existsByLocode(String locode);

    long count();

    List<Port> searchByName(String query);
}

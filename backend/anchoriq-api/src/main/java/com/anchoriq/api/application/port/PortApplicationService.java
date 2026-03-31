package com.anchoriq.api.application.port;

import com.anchoriq.api.dto.response.port.PortResponse;

import java.util.List;
import java.util.Map;

/**
 * 항만 Application Service 인터페이스.
 */
public interface PortApplicationService {

    List<PortResponse> findAll();

    List<PortResponse> findAll(int page, int size);

    PortResponse findByLocode(String locode);

    List<PortResponse> findByCountry(String country);

    List<PortResponse> findCongestedPorts();

    List<PortResponse> findTopCongestedPorts(int limit);

    double getCongestion(String locode);

    Map<String, Object> getCongestionDetail(String locode);

    long count();
}

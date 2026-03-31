package com.anchoriq.api.application.risk;

import java.util.List;
import java.util.Map;

/**
 * 비교 Application Service 인터페이스.
 */
public interface CompareApplicationService {

    Map<String, Object> compareRoutes(List<Long> routeIds);

    Map<String, Object> comparePorts(List<String> locodes);
}

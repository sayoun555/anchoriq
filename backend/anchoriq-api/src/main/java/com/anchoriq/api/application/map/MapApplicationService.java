package com.anchoriq.api.application.map;

import com.anchoriq.api.dto.response.map.ChokepointMapResponse;
import com.anchoriq.api.dto.response.map.MapVesselResponse;

import java.util.List;
import java.util.Map;

/**
 * 지도 데이터 Application Service 인터페이스.
 */
public interface MapApplicationService {

    List<MapVesselResponse> getVesselPositions();

    List<ChokepointMapResponse> getChokepoints();

    List<Map<String, Object>> getHeatmapData();
}

package com.anchoriq.api.dto.response.market;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * 시장 데이터 응답 DTO.
 */
@Getter
@Builder
public class MarketDataResponse {

    private final String type;
    private final Map<String, Object> data;
    private final String updatedAt;
}

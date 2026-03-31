package com.anchoriq.api.application.market;

import com.anchoriq.api.dto.response.market.MarketDataResponse;

/**
 * 시장 데이터 Application Service 인터페이스.
 */
public interface MarketApplicationService {

    MarketDataResponse getOilPrice();

    MarketDataResponse getExchangeRate();

    MarketDataResponse getOverview();
}

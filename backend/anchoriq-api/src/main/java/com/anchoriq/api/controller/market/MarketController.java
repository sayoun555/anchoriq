package com.anchoriq.api.controller.market;

import com.anchoriq.api.application.market.MarketApplicationService;
import com.anchoriq.api.dto.response.market.MarketDataResponse;
import com.anchoriq.api.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 시장 데이터 Controller.
 */
@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketController {

    private final MarketApplicationService marketApplicationService;

    @GetMapping("/oil/current")
    public ApiResponse<MarketDataResponse> getOilPrice() {
        return ApiResponse.success(marketApplicationService.getOilPrice());
    }

    @GetMapping("/exchange/current")
    public ApiResponse<MarketDataResponse> getExchangeRate() {
        return ApiResponse.success(marketApplicationService.getExchangeRate());
    }

    @GetMapping("/overview")
    public ApiResponse<MarketDataResponse> getOverview() {
        return ApiResponse.success(marketApplicationService.getOverview());
    }
}

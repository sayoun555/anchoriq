package com.anchoriq.api.controller.weather;

import com.anchoriq.api.application.weather.WeatherApplicationService;
import com.anchoriq.api.dto.response.weather.TyphoonResponse;
import com.anchoriq.api.dto.response.weather.WeatherResponse;
import com.anchoriq.api.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 날씨 조회 Controller.
 */
@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherApplicationService weatherApplicationService;

    @GetMapping("/current")
    public ApiResponse<List<WeatherResponse>> getCurrentWeather() {
        return ApiResponse.success(weatherApplicationService.getCurrentWeather());
    }

    @GetMapping("/alerts")
    public ApiResponse<List<WeatherResponse>> getWeatherAlerts() {
        return ApiResponse.success(weatherApplicationService.getWeatherAlerts());
    }

    @GetMapping("/typhoons")
    public ApiResponse<List<TyphoonResponse>> getActiveTyphoons() {
        return ApiResponse.success(weatherApplicationService.getActiveTyphoons());
    }
}

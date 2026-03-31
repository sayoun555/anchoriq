package com.anchoriq.api.application.weather;

import com.anchoriq.api.dto.response.weather.TyphoonResponse;
import com.anchoriq.api.dto.response.weather.WeatherResponse;

import java.util.List;

/**
 * 날씨 Application Service 인터페이스.
 */
public interface WeatherApplicationService {

    List<WeatherResponse> getCurrentWeather();

    List<WeatherResponse> getWeatherAlerts();

    List<TyphoonResponse> getActiveTyphoons();
}

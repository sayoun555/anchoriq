package com.anchoriq.api.application.weather;

import com.anchoriq.api.dto.response.weather.TyphoonResponse;
import com.anchoriq.api.dto.response.weather.WeatherResponse;
import com.anchoriq.core.domain.maritime.weather.model.WeatherCondition;
import com.anchoriq.core.domain.maritime.weather.model.WeatherType;
import com.anchoriq.core.domain.maritime.weather.repository.WeatherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 날씨 Application Service 구현체.
 */
@Service
@RequiredArgsConstructor
public class WeatherApplicationServiceImpl implements WeatherApplicationService {

    private final WeatherRepository weatherRepository;

    @Override
    public List<WeatherResponse> getCurrentWeather() {
        return weatherRepository.findAll().stream()
                .map(WeatherResponse::from)
                .toList();
    }

    @Override
    public List<WeatherResponse> getWeatherAlerts() {
        return weatherRepository.findSevereConditions().stream()
                .map(WeatherResponse::from)
                .toList();
    }

    @Override
    public List<TyphoonResponse> getActiveTyphoons() {
        return weatherRepository.findAll().stream()
                .filter(wc -> wc.getType() == WeatherType.TYPHOON)
                .map(TyphoonResponse::from)
                .toList();
    }
}

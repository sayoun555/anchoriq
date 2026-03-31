package com.anchoriq.core.domain.maritime.weather.repository;

import com.anchoriq.core.domain.maritime.weather.model.WeatherCondition;

import java.util.List;
import java.util.Optional;

/**
 * Weather Repository 인터페이스.
 */
public interface WeatherRepository {

    List<WeatherCondition> findAll();

    List<WeatherCondition> findSevereConditions();

    Optional<WeatherCondition> findById(Long id);

    WeatherCondition save(WeatherCondition condition);

    long count();
}

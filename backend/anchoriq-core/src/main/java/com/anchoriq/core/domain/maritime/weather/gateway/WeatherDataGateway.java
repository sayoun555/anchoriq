package com.anchoriq.core.domain.maritime.weather.gateway;

/**
 * 기상 데이터 외부 시스템 게이트웨이 인터페이스.
 * Open-Meteo API 연동 구현체가 이 인터페이스를 구현한다.
 */
public interface WeatherDataGateway {

    /**
     * 해역별 현재 기상 데이터를 수집한다.
     */
    void collectCurrentWeather();

    /**
     * 활성 태풍 정보를 수집한다.
     */
    void collectTyphoonData();
}

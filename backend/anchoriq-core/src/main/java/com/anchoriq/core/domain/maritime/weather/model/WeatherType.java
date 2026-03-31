package com.anchoriq.core.domain.maritime.weather.model;

public enum WeatherType {

    TYPHOON("Typhoon"),
    STORM("Storm"),
    FOG("Fog"),
    HEAVY_RAIN("Heavy Rain"),
    HIGH_WIND("High Wind"),
    HIGH_WAVE("High Wave"),
    CLEAR("Clear");

    private final String displayName;

    WeatherType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

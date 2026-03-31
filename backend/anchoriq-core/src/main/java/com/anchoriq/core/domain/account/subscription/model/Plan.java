package com.anchoriq.core.domain.account.subscription.model;

import java.util.Set;

public enum Plan {

    FREE(Features.of(Feature.DASHBOARD, Feature.AI_QUERY), 5),
    PRO(Features.of(Feature.DASHBOARD, Feature.AI_QUERY, Feature.REALTIME_ALERT,
            Feature.WORKFLOW, Feature.VESSEL_HISTORY, Feature.EXPORT), 100),
    ENTERPRISE(Features.of(Set.of(Feature.values())), Integer.MAX_VALUE);

    private final Features supportedFeatures;
    private final int dailyApiLimit;

    Plan(Features supportedFeatures, int dailyApiLimit) {
        this.supportedFeatures = supportedFeatures;
        this.dailyApiLimit = dailyApiLimit;
    }

    public boolean supports(Feature feature) {
        return supportedFeatures.supports(feature);
    }

    public Features getSupportedFeatures() {
        return supportedFeatures;
    }

    public int getDailyApiLimit() {
        return dailyApiLimit;
    }
}

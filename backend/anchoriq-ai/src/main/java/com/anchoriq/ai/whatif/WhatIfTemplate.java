package com.anchoriq.ai.whatif;

import java.util.Objects;

/**
 * What-if 시뮬레이션 템플릿.
 * 자주 사용되는 시나리오를 사전 정의한다.
 */
public class WhatIfTemplate {

    private final String id;
    private final String name;
    private final String description;
    private final String scenario;
    private final String defaultDuration;

    private WhatIfTemplate(String id, String name, String description,
                           String scenario, String defaultDuration) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.description = description;
        this.scenario = Objects.requireNonNull(scenario);
        this.defaultDuration = defaultDuration;
    }

    public static WhatIfTemplate of(String id, String name, String description,
                                     String scenario, String defaultDuration) {
        return new WhatIfTemplate(id, name, description, scenario, defaultDuration);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getScenario() {
        return scenario;
    }

    public String getDefaultDuration() {
        return defaultDuration;
    }
}

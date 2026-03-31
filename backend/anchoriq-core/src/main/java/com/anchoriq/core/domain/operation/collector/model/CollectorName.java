package com.anchoriq.core.domain.operation.collector.model;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 수집기 이름 VO.
 * 허용된 수집기 이름만 사용할 수 있도록 원시 타입(String)을 포장한다.
 */
public enum CollectorName {

    AIS("ais", "AIS 선박 위치"),
    WEATHER("weather", "기상 데이터"),
    NEWS("news", "해운 뉴스"),
    OIL_PRICE("oil-price", "유가"),
    EXCHANGE_RATE("exchange-rate", "환율"),
    SANCTION("sanction", "제재 목록"),
    GEOPOLITICAL("geopolitical", "지정학 이벤트"),
    PORT_CONGESTION("port-congestion", "항만 혼잡도"),
    UNCTAD_BASELINE("unctad-baseline", "UNCTAD 기준선");

    private static final Map<String, CollectorName> BY_VALUE = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(CollectorName::value, Function.identity()));

    private final String value;
    private final String displayName;

    CollectorName(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String value() {
        return value;
    }

    public String displayName() {
        return displayName;
    }

    /**
     * 문자열로부터 CollectorName을 찾는다.
     *
     * @throws IllegalArgumentException 허용되지 않은 수집기 이름인 경우
     */
    public static CollectorName from(String name) {
        CollectorName result = BY_VALUE.get(name);
        if (result == null) {
            throw new IllegalArgumentException(
                    "Unknown collector name: '" + name + "'. Allowed: " + BY_VALUE.keySet());
        }
        return result;
    }

    public boolean isAis() {
        return this == AIS;
    }
}

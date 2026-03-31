package com.anchoriq.collector.registry;

import com.anchoriq.core.domain.operation.collector.model.CollectorName;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 수집기별 자동 시작 설정을 관리하는 프로퍼티 클래스.
 * application.yml의 collector.auto-start 설정을 바인딩한다.
 * Bean 등록은 CollectorConfig에서 수행한다.
 */
public class CollectorAutoStartProperties {

    private final Map<CollectorName, Boolean> autoStartMap;

    public CollectorAutoStartProperties(boolean ais,
                                        boolean weather,
                                        boolean news,
                                        boolean oilPrice,
                                        boolean exchangeRate,
                                        boolean sanction,
                                        boolean geopolitical,
                                        boolean portCongestion,
                                        boolean unctadBaseline) {
        this.autoStartMap = new ConcurrentHashMap<>();
        autoStartMap.put(CollectorName.AIS, ais);
        autoStartMap.put(CollectorName.WEATHER, weather);
        autoStartMap.put(CollectorName.NEWS, news);
        autoStartMap.put(CollectorName.OIL_PRICE, oilPrice);
        autoStartMap.put(CollectorName.EXCHANGE_RATE, exchangeRate);
        autoStartMap.put(CollectorName.SANCTION, sanction);
        autoStartMap.put(CollectorName.GEOPOLITICAL, geopolitical);
        autoStartMap.put(CollectorName.PORT_CONGESTION, portCongestion);
        autoStartMap.put(CollectorName.UNCTAD_BASELINE, unctadBaseline);
    }

    /**
     * 지정한 수집기가 자동 시작 대상인지 확인한다.
     */
    public boolean isAutoStart(CollectorName name) {
        return autoStartMap.getOrDefault(name, false);
    }
}

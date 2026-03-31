package com.anchoriq.collector.source.geography;

import com.anchoriq.collector.source.port.UnLocodeLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 애플리케이션 시작 시 정적 지리 데이터를 로드하는 초기화 컴포넌트.
 * 초크포인트, EEZ, 항만 데이터를 DB에 시드한다.
 */
@Component
public class GeographyInitializer {

    private static final Logger log = LoggerFactory.getLogger(GeographyInitializer.class);

    private final List<GeographyLoader> geographyLoaders;
    private final UnLocodeLoader unLocodeLoader;
    private final DemoDataInitializer demoDataInitializer;

    public GeographyInitializer(List<GeographyLoader> geographyLoaders,
                                UnLocodeLoader unLocodeLoader,
                                DemoDataInitializer demoDataInitializer) {
        this.geographyLoaders = geographyLoaders;
        this.unLocodeLoader = unLocodeLoader;
        this.demoDataInitializer = demoDataInitializer;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeGeographyData() {
        log.info("Initializing geography data...");

        for (GeographyLoader loader : geographyLoaders) {
            try {
                loader.load();
                log.info("Loaded: {}", loader.loaderName());
            } catch (Exception e) {
                log.error("Failed to load {}: {}", loader.loaderName(), e.getMessage());
            }
        }

        try {
            unLocodeLoader.loadInitialPorts();
        } catch (Exception e) {
            log.error("Failed to load UN/LOCODE ports: {}", e.getMessage());
        }

        // 데모 데이터 비활성화 — 실제 수집기 데이터 사용
        log.info("Demo data loading skipped (real collectors active)");

        log.info("Geography data initialization completed");
    }
}

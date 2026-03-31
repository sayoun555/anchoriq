package com.anchoriq.collector.config;

import org.springframework.context.annotation.Configuration;

/**
 * Playwright 브라우저 자동화 설정.
 * UNCTAD 크롤링에 사용하는 Chromium 브라우저 설정을 관리한다.
 * Playwright는 Component 내에서 직접 생성/해제하므로 별도 Bean 선언이 불필요하다.
 * 이 설정 클래스는 향후 브라우저 풀링 등 확장 시 사용한다.
 */
@Configuration
public class PlaywrightConfig {
    // Playwright는 try-with-resources로 UncladCongestionCrawler에서 직접 관리
    // 브라우저 인스턴스 풀링이 필요한 경우 이 클래스에서 Bean으로 관리
}

package com.anchoriq.collector.source.port;

import com.anchoriq.collector.producer.PortCongestionKafkaProducer;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * UNCTAD 항만 혼잡도 크롤러.
 * Playwright 기반 브라우저 자동화로 UNCTAD Port Tracker에서 혼잡도를 수집하여
 * port-congestion 토픽으로 전송한다.
 * Bean 등록은 CollectorConfig에서 수행한다.
 */
public class UncladCongestionCrawler implements PortDataCollector {

    private static final Logger log = LoggerFactory.getLogger(UncladCongestionCrawler.class);
    private static final String UNCTAD_URL = "https://unctad.org/topic/transport-and-trade-logistics/port-call-statistics";

    private final PortCongestionKafkaProducer portCongestionKafkaProducer;

    public UncladCongestionCrawler(PortCongestionKafkaProducer portCongestionKafkaProducer) {
        this.portCongestionKafkaProducer = portCongestionKafkaProducer;
    }

    @Override
    public void collect() {
        log.info("Starting UNCTAD port congestion crawling");
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium()
                    .launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext();
            Page page = context.newPage();

            page.navigate(UNCTAD_URL);
            page.waitForLoadState();

            crawlPortData(page);

            context.close();
            browser.close();
            log.info("UNCTAD crawling completed");
        } catch (Exception e) {
            log.error("Failed to crawl UNCTAD port data: {}", e.getMessage());
        }
    }

    @Override
    public String sourceName() {
        return "UNCTAD";
    }

    private void crawlPortData(Page page) {
        try {
            Locator rows = page.locator("table tbody tr");
            int count = rows.count();

            for (int i = 0; i < count; i++) {
                try {
                    Locator row = rows.nth(i);
                    String portName = row.locator("td").nth(0).textContent().trim();
                    String locode = row.locator("td").nth(1).textContent().trim();
                    String congestionText = row.locator("td").nth(2).textContent().trim();

                    double congestionLevel = parseCongestionLevel(congestionText);

                    Map<String, Object> message = new LinkedHashMap<>();
                    message.put("locode", locode);
                    message.put("portName", portName);
                    message.put("congestionLevel", congestionLevel);
                    message.put("timestamp", Instant.now().toString());

                    portCongestionKafkaProducer.send(null, message);
                } catch (Exception e) {
                    log.warn("Failed to parse row {}: {}", i, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract table data, UNCTAD page structure may have changed: {}",
                    e.getMessage());
        }
    }

    private double parseCongestionLevel(String text) {
        try {
            String cleaned = text.replaceAll("[^0-9.]", "");
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}

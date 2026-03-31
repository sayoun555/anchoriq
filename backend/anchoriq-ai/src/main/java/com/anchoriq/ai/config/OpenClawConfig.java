package com.anchoriq.ai.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * OpenClaw 로컬 게이트웨이 설정.
 * 환경변수로 관리: OPENCLAW_GATEWAY_TOKEN, OPENCLAW_BASE_URL, OPENCLAW_MODEL
 */
@Getter
@Configuration
public class OpenClawConfig {

    @Value("${openclaw.base-url:http://127.0.0.1:18789}")
    private String baseUrl;

    @Value("${openclaw.gateway-token:}")
    private String gatewayToken;

    @Value("${openclaw.model:openclaw:main}")
    private String model;

    @Value("${openclaw.timeout-seconds:60}")
    private int timeoutSeconds;

    @Value("${openclaw.max-retries:3}")
    private int maxRetries;

    @Value("${openclaw.max-tokens:2000}")
    private int maxTokens;

    @Value("${openclaw.default-temperature:0.7}")
    private double defaultTemperature;

    @Bean("openClawWebClient")
    public WebClient openClawWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + gatewayToken)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}

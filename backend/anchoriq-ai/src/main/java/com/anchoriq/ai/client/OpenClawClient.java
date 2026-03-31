package com.anchoriq.ai.client;

import com.anchoriq.ai.config.OpenClawConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * OpenClaw 로컬 게이트웨이 클라이언트 구현체.
 * /v1/chat/completions 엔드포인트 (OpenAI 호환 형식) 사용.
 */
@Slf4j
@Component
public class OpenClawClient implements AiClient {

    private final WebClient webClient;
    private final OpenClawConfig config;

    public OpenClawClient(@Qualifier("openClawWebClient") WebClient webClient,
                          OpenClawConfig config) {
        this.webClient = webClient;
        this.config = config;
    }

    @Override
    public Mono<String> chat(String systemPrompt, String userMessage) {
        return chat(systemPrompt, userMessage, config.getDefaultTemperature());
    }

    @Override
    public Mono<String> chat(String systemPrompt, String userMessage, double temperature) {
        OpenClawRequest request = OpenClawRequest.of(
                systemPrompt, userMessage,
                config.getModel(), temperature, config.getMaxTokens());

        return webClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OpenClawResponse.class)
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .retryWhen(Retry.backoff(config.getMaxRetries(), Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(5))
                        .doBeforeRetry(signal ->
                                log.warn("Retrying OpenClaw call, attempt: {}", signal.totalRetries() + 1)))
                .map(OpenClawResponse::getContent)
                .doOnError(error ->
                        log.error("OpenClaw API call failed: {}", error.getMessage()))
                .onErrorReturn("AI service is currently unavailable. Please try again later.");
    }
}

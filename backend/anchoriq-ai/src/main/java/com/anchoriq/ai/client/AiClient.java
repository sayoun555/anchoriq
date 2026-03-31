package com.anchoriq.ai.client;

import reactor.core.publisher.Mono;

/**
 * AI 클라이언트 인터페이스.
 * LLM 호출을 추상화한다.
 */
public interface AiClient {

    Mono<String> chat(String systemPrompt, String userMessage);

    Mono<String> chat(String systemPrompt, String userMessage, double temperature);
}

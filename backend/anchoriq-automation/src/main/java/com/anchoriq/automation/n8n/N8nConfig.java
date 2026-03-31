package com.anchoriq.automation.n8n;

import org.springframework.context.annotation.Configuration;

/**
 * n8n 관련 설정 — 환경변수에서 base-url, webhook-path를 읽는다.
 * application.yml의 anchoriq.n8n.* 프로퍼티를 사용.
 */
@Configuration
public class N8nConfig {
    // N8nWebhookClient에서 @Value로 직접 주입
    // 복잡한 설정이 추가되면 @ConfigurationProperties로 전환
}

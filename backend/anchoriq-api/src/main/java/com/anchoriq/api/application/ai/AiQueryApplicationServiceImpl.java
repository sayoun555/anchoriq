package com.anchoriq.api.application.ai;

import com.anchoriq.ai.briefing.BriefingService;
import com.anchoriq.ai.consumer.AiDecisionLogConsumer;
import com.anchoriq.ai.query.NaturalLanguageQueryService;
import com.anchoriq.ai.recommendation.RecommendationService;
import com.anchoriq.ai.whatif.WhatIfResult;
import com.anchoriq.ai.whatif.WhatIfService;
import com.anchoriq.ai.whatif.WhatIfTemplate;
import com.anchoriq.core.common.exception.PlanLimitExceededException;
import com.anchoriq.core.domain.account.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 질의 Application Service 구현체.
 * 오케스트레이션만 담당: 플랜 체크, 사용량 체크, 캐시 확인, 서비스 호출.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiQueryApplicationServiceImpl implements AiQueryApplicationService {

    private final NaturalLanguageQueryService queryService;
    private final BriefingService briefingService;
    private final WhatIfService whatIfService;
    private final RecommendationService recommendationService;
    private final SubscriptionService subscriptionService;
    @Nullable
    private final AiDecisionLogConsumer aiDecisionLogConsumer;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Map<String, Object> handleQuery(String query, Long userId) {
        checkApiQuota(userId);

        String cacheKey = buildCacheKey(query);
        String cached = getCachedResult(cacheKey);
        if (cached != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("answer", cached);
            result.put("entities", List.of());
            result.put("cypher", "cached");
            return result;
        }

        Map<String, Object> result = queryService.executeQuery(query);

        cacheResult(cacheKey, (String) result.get("answer"));
        subscriptionService.incrementApiUsage(userId);
        if (aiDecisionLogConsumer != null) {
            aiDecisionLogConsumer.logDecision("QUERY", query, (String) result.get("answer"), userId, 0);
        }

        return result;
    }

    @Override
    public Map<String, Object> getDailyBriefing() {
        try {
            return briefingService.generateDailyBriefing();
        } catch (Exception e) {
            log.warn("Failed to generate daily briefing: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    @Override
    public WhatIfResult simulateWhatIf(String scenario, String duration) {
        return whatIfService.simulate(scenario, duration);
    }

    @Override
    public List<WhatIfResult> getWhatIfHistory(Long userId) {
        try {
            return whatIfService.getHistory(userId, 20);
        } catch (Exception e) {
            log.warn("Failed to fetch what-if history: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<WhatIfTemplate> getWhatIfTemplates() {
        try {
            return whatIfService.getTemplates();
        } catch (Exception e) {
            log.warn("Failed to fetch what-if templates: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<Map<String, Object>> getRecommendations() {
        try {
            return recommendationService.getRecommendations();
        } catch (Exception e) {
            log.warn("Failed to fetch recommendations: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public Map<String, Object> getUsage(Long userId) {
        Map<String, Object> usage = new HashMap<>();
        try {
            boolean hasQuota = subscriptionService.hasRemainingApiQuota(userId);
            usage.put("hasRemainingQuota", hasQuota);
            usage.put("plan", subscriptionService.getActiveSubscription(userId).getPlan().name());
        } catch (Exception e) {
            log.warn("Failed to fetch usage for user {}: {}", userId, e.getMessage());
            usage.put("hasRemainingQuota", true);
            usage.put("plan", "FREE");
        }
        return usage;
    }

    private void checkApiQuota(Long userId) {
        if (!subscriptionService.hasRemainingApiQuota(userId)) {
            throw new PlanLimitExceededException("Daily AI query limit exceeded. Upgrade to Pro for unlimited queries.");
        }
    }

    private String buildCacheKey(String query) {
        return "ai:result:" + query.hashCode();
    }

    private String getCachedResult(String cacheKey) {
        try {
            return stringRedisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.warn("Redis cache read failed, ignoring: {}", e.getMessage());
            return null;
        }
    }

    private void cacheResult(String cacheKey, String result) {
        try {
            if (result != null) {
                stringRedisTemplate.opsForValue().set(cacheKey, result, Duration.ofMinutes(5));
            }
        } catch (Exception e) {
            log.warn("Redis cache write failed, ignoring: {}", e.getMessage());
        }
    }
}

package com.anchoriq.api.infrastructure.subscription;

import com.anchoriq.core.common.exception.EntityNotFoundException;
import com.anchoriq.core.domain.account.subscription.model.Feature;
import com.anchoriq.core.domain.account.subscription.model.Subscription;
import com.anchoriq.core.domain.account.subscription.repository.SubscriptionRepository;
import com.anchoriq.core.domain.account.subscription.service.SubscriptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 구독 서비스 구현체.
 * Bean 등록은 DomainServiceConfig에서 수행한다.
 */
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final String API_USAGE_KEY_PREFIX = "api:usage:";

    private final SubscriptionRepository subscriptionRepository;
    private final StringRedisTemplate stringRedisTemplate;

    public SubscriptionServiceImpl(SubscriptionRepository subscriptionRepository,
                                    StringRedisTemplate stringRedisTemplate) {
        this.subscriptionRepository = subscriptionRepository;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean canUseFeature(Long userId, Feature feature) {
        Subscription subscription = getActiveSubscription(userId);
        return subscription.canAccess(feature);
    }

    @Override
    public boolean hasRemainingApiQuota(Long userId) {
        Subscription subscription = getActiveSubscription(userId);
        int dailyLimit = subscription.getPlan().getDailyApiLimit();

        if (dailyLimit == Integer.MAX_VALUE) {
            return true;
        }

        String key = buildDailyUsageKey(userId);
        String countStr = stringRedisTemplate.opsForValue().get(key);
        int currentCount = (countStr != null) ? Integer.parseInt(countStr) : 0;

        return currentCount < dailyLimit;
    }

    @Override
    public void incrementApiUsage(Long userId) {
        String key = buildDailyUsageKey(userId);
        try {
            Long newCount = stringRedisTemplate.opsForValue().increment(key);
            if (newCount != null && newCount == 1) {
                Duration ttl = calculateTtlUntilMidnight();
                stringRedisTemplate.expire(key, ttl);
            }
        } catch (Exception e) {
            log.warn("Failed to increment API usage in Redis, ignoring: {}", e.getMessage());
        }
    }

    @Override
    public Subscription getActiveSubscription(Long userId) {
        return subscriptionRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription", String.valueOf(userId)));
    }

    private String buildDailyUsageKey(Long userId) {
        return API_USAGE_KEY_PREFIX + userId + ":" + LocalDate.now();
    }

    private Duration calculateTtlUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = LocalDateTime.of(now.toLocalDate().plusDays(1), LocalTime.MIDNIGHT);
        return Duration.between(now, midnight);
    }
}

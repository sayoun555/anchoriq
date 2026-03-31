package com.anchoriq.core.domain.account.subscription.factory;

import com.anchoriq.core.common.exception.DuplicateException;
import com.anchoriq.core.common.exception.EntityNotFoundException;
import com.anchoriq.core.domain.account.subscription.model.Plan;
import com.anchoriq.core.domain.account.subscription.model.Subscription;
import com.anchoriq.core.domain.account.subscription.model.SubscriptionStatus;
import com.anchoriq.core.domain.account.subscription.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionFactory 팩토리 테스트")
class SubscriptionFactoryTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    private SubscriptionFactory subscriptionFactory;

    @BeforeEach
    void setUp() {
        subscriptionFactory = new SubscriptionFactory(subscriptionRepository);
    }

    @Test
    @DisplayName("기존 구독이 없으면 무료 구독을 생성할 수 있다")
    void should_createFreeSubscription_when_noExistingSubscription() {
        // Given
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.empty());

        // When
        Subscription subscription = subscriptionFactory.createFreeSubscription(1L);

        // Then
        assertThat(subscription).isNotNull();
        assertThat(subscription.getPlan()).isEqualTo(Plan.FREE);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("기존 구독이 있으면 DuplicateException을 던진다")
    void should_throwDuplicateException_when_subscriptionAlreadyExists() {
        // Given
        Subscription existing = Subscription.createFree(1L);
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.of(existing));

        // When & Then
        assertThatThrownBy(() -> subscriptionFactory.createFreeSubscription(1L))
                .isInstanceOf(DuplicateException.class)
                .hasMessageContaining("already has a subscription");
    }

    @Test
    @DisplayName("기존 구독을 PRO 플랜으로 업그레이드할 수 있다")
    void should_upgradeSubscription_when_existingSubscriptionFound() {
        // Given
        Subscription existing = Subscription.createPending(1L, Plan.FREE);
        when(subscriptionRepository.findByUserId(1L)).thenReturn(Optional.of(existing));

        // When
        Subscription upgraded = subscriptionFactory.upgradeSubscription(1L, Plan.PRO);

        // Then
        assertThat(upgraded.getPlan()).isEqualTo(Plan.PRO);
        assertThat(upgraded.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("구독이 없는 사용자의 업그레이드는 EntityNotFoundException을 던진다")
    void should_throwEntityNotFound_when_noSubscriptionToUpgrade() {
        // Given
        when(subscriptionRepository.findByUserId(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> subscriptionFactory.upgradeSubscription(999L, Plan.PRO))
                .isInstanceOf(EntityNotFoundException.class);
    }
}

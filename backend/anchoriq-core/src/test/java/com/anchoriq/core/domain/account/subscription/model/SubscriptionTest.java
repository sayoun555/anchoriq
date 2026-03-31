package com.anchoriq.core.domain.account.subscription.model;

import com.anchoriq.core.domain.common.event.DomainEvent;
import com.anchoriq.core.domain.common.event.SubscriptionActivatedEvent;
import com.anchoriq.core.domain.common.event.SubscriptionCancelledEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Subscription 엔티티 테스트")
class SubscriptionTest {

    @Nested
    @DisplayName("상태 전이 테스트")
    class StatusTransitionTest {

        @Test
        @DisplayName("PENDING에서 ACTIVE로 활성화할 수 있다")
        void should_activate_when_pendingStatus() {
            // Given
            Subscription subscription = Subscription.createPending(1L, Plan.PRO);

            // When
            subscription.activate(Plan.PRO);

            // Then
            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(subscription.getPlan()).isEqualTo(Plan.PRO);
        }

        @Test
        @DisplayName("ACTIVE에서 CANCELLED로 취소할 수 있다")
        void should_cancel_when_activeStatus() {
            // Given
            Subscription subscription = Subscription.createFree(1L);

            // When
            subscription.cancel();

            // Then
            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        }

        @Test
        @DisplayName("CANCELLED에서 ACTIVE로 재활성화할 수 있다")
        void should_reactivate_when_cancelledStatus() {
            // Given
            Subscription subscription = Subscription.createFree(1L);
            subscription.cancel();

            // When
            subscription.reactivate(Plan.PRO);

            // Then
            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(subscription.getPlan()).isEqualTo(Plan.PRO);
        }

        @Test
        @DisplayName("PENDING에서 CANCELLED로 취소할 수 있다")
        void should_cancel_when_pendingStatus() {
            Subscription subscription = Subscription.createPending(1L, Plan.PRO);

            subscription.cancel();

            assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        }

        @Test
        @DisplayName("CANCELLED에서 직접 CANCELLED로 전이할 수 없다")
        void should_throwException_when_cancellingCancelledSubscription() {
            Subscription subscription = Subscription.createFree(1L);
            subscription.cancel();

            assertThatThrownBy(() -> subscription.cancel())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invalid subscription status transition");
        }

        @Test
        @DisplayName("ACTIVE에서 직접 ACTIVE로 전이할 수 없다 (activate)")
        void should_throwException_when_activatingActiveSubscription() {
            Subscription subscription = Subscription.createFree(1L);

            assertThatThrownBy(() -> subscription.activate(Plan.PRO))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Invalid subscription status transition");
        }
    }

    @Nested
    @DisplayName("기능 접근 테스트")
    class CanAccessTest {

        @Test
        @DisplayName("FREE 플랜은 DASHBOARD와 AI_QUERY에 접근할 수 있다")
        void should_accessBasicFeatures_when_freePlan() {
            Subscription subscription = Subscription.createFree(1L);

            assertThat(subscription.canAccess(Feature.DASHBOARD)).isTrue();
            assertThat(subscription.canAccess(Feature.AI_QUERY)).isTrue();
        }

        @Test
        @DisplayName("FREE 플랜은 REALTIME_ALERT에 접근할 수 없다")
        void should_notAccessProFeatures_when_freePlan() {
            Subscription subscription = Subscription.createFree(1L);

            assertThat(subscription.canAccess(Feature.REALTIME_ALERT)).isFalse();
            assertThat(subscription.canAccess(Feature.WORKFLOW)).isFalse();
            assertThat(subscription.canAccess(Feature.EXPORT)).isFalse();
        }

        @Test
        @DisplayName("PRO 플랜은 REALTIME_ALERT, WORKFLOW, EXPORT에 접근할 수 있다")
        void should_accessProFeatures_when_proPlan() {
            Subscription subscription = Subscription.createPending(1L, Plan.PRO);
            subscription.activate(Plan.PRO);

            assertThat(subscription.canAccess(Feature.REALTIME_ALERT)).isTrue();
            assertThat(subscription.canAccess(Feature.WORKFLOW)).isTrue();
            assertThat(subscription.canAccess(Feature.EXPORT)).isTrue();
        }

        @Test
        @DisplayName("PRO 플랜은 EXTERNAL_API에 접근할 수 없다")
        void should_notAccessEnterpriseFeatures_when_proPlan() {
            Subscription subscription = Subscription.createPending(1L, Plan.PRO);
            subscription.activate(Plan.PRO);

            assertThat(subscription.canAccess(Feature.EXTERNAL_API)).isFalse();
            assertThat(subscription.canAccess(Feature.CUSTOM_ONTOLOGY)).isFalse();
        }

        @Test
        @DisplayName("ENTERPRISE 플랜은 모든 기능에 접근할 수 있다")
        void should_accessAllFeatures_when_enterprisePlan() {
            Subscription subscription = Subscription.createPending(1L, Plan.ENTERPRISE);
            subscription.activate(Plan.ENTERPRISE);

            for (Feature feature : Feature.values()) {
                assertThat(subscription.canAccess(feature))
                        .as("ENTERPRISE should access " + feature)
                        .isTrue();
            }
        }

        @Test
        @DisplayName("비활성 구독은 기능에 접근할 수 없다")
        void should_notAccess_when_inactiveSubscription() {
            Subscription subscription = Subscription.createFree(1L);
            subscription.cancel();

            assertThat(subscription.canAccess(Feature.DASHBOARD)).isFalse();
        }
    }

    @Nested
    @DisplayName("도메인 이벤트 발행 테스트")
    class DomainEventTest {

        @Test
        @DisplayName("활성화 시 SubscriptionActivatedEvent가 발행된다")
        void should_publishActivatedEvent_when_activated() {
            Subscription subscription = Subscription.createPending(1L, Plan.PRO);

            subscription.activate(Plan.PRO);

            List<DomainEvent> events = subscription.getDomainEvents();
            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(SubscriptionActivatedEvent.class);

            SubscriptionActivatedEvent event = (SubscriptionActivatedEvent) events.get(0);
            assertThat(event.getUserId()).isEqualTo(1L);
            assertThat(event.getPlan()).isEqualTo("PRO");
        }

        @Test
        @DisplayName("취소 시 SubscriptionCancelledEvent가 발행된다")
        void should_publishCancelledEvent_when_cancelled() {
            Subscription subscription = Subscription.createFree(1L);

            subscription.cancel();

            List<DomainEvent> events = subscription.getDomainEvents();
            assertThat(events).hasSize(1);
            assertThat(events.get(0)).isInstanceOf(SubscriptionCancelledEvent.class);

            SubscriptionCancelledEvent event = (SubscriptionCancelledEvent) events.get(0);
            assertThat(event.getUserId()).isEqualTo(1L);
            assertThat(event.getPlan()).isEqualTo("FREE");
        }

        @Test
        @DisplayName("clearDomainEvents로 이벤트를 비울 수 있다")
        void should_clearEvents_when_clearDomainEventsCalled() {
            Subscription subscription = Subscription.createFree(1L);
            subscription.cancel();
            assertThat(subscription.getDomainEvents()).isNotEmpty();

            subscription.clearDomainEvents();

            assertThat(subscription.getDomainEvents()).isEmpty();
        }
    }

    @Test
    @DisplayName("API 사용량 초과 시 IllegalStateException을 던진다")
    void should_throwException_when_apiLimitExceeded() {
        // Given
        Subscription subscription = Subscription.createFree(1L); // limit: 5

        // When - 5번 사용
        for (int i = 0; i < 5; i++) {
            subscription.checkAndDecrementQuota();
        }

        // Then - 6번째 사용 시 예외
        assertThatThrownBy(() -> subscription.checkAndDecrementQuota())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Daily API limit exceeded");
    }

    @Test
    @DisplayName("비활성 구독에서 API 사용 시 IllegalStateException을 던진다")
    void should_throwException_when_usingApiOnInactiveSubscription() {
        Subscription subscription = Subscription.createFree(1L);
        subscription.cancel();

        assertThatThrownBy(() -> subscription.checkAndDecrementQuota())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not active");
    }

    @Test
    @DisplayName("일일 사용량 리셋이 정상 동작한다")
    void should_resetUsage_when_resetDailyUsageCalled() {
        Subscription subscription = Subscription.createFree(1L);
        subscription.checkAndDecrementQuota();
        subscription.checkAndDecrementQuota();

        subscription.resetDailyUsage();

        assertThat(subscription.getDailyApiUsage()).isEqualTo(0);
    }
}

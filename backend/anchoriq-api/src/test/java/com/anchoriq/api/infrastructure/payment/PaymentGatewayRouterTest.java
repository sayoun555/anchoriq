package com.anchoriq.api.infrastructure.payment;

import com.anchoriq.core.domain.account.payment.gateway.PaymentGateway;
import com.anchoriq.core.domain.account.payment.model.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentGatewayRouter 통화별 분기 테스트")
class PaymentGatewayRouterTest {

    @Mock
    private StripePaymentGateway stripePaymentGateway;

    @Mock
    private TossPaymentGateway tossPaymentGateway;

    private PaymentGatewayRouter router;

    @BeforeEach
    void setUp() {
        router = new PaymentGatewayRouter(stripePaymentGateway, tossPaymentGateway);
    }

    @Test
    @DisplayName("KRW 통화이면 Toss 게이트웨이를 반환한다")
    void should_returnTossGateway_when_currencyIsKRW() {
        // When
        PaymentGateway gateway = router.resolve(Currency.KRW);

        // Then
        assertThat(gateway).isInstanceOf(TossPaymentGateway.class);
    }

    @Test
    @DisplayName("USD 통화이면 Stripe 게이트웨이를 반환한다")
    void should_returnStripeGateway_when_currencyIsUSD() {
        // When
        PaymentGateway gateway = router.resolve(Currency.USD);

        // Then
        assertThat(gateway).isInstanceOf(StripePaymentGateway.class);
    }

    @Test
    @DisplayName("같은 통화로 여러 번 호출해도 동일한 게이트웨이를 반환한다")
    void should_returnConsistentGateway_when_calledMultipleTimes() {
        // When
        PaymentGateway first = router.resolve(Currency.KRW);
        PaymentGateway second = router.resolve(Currency.KRW);

        // Then
        assertThat(first).isSameAs(second);
    }
}

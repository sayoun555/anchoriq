package com.anchoriq.api.infrastructure.persistence.payment;

import com.anchoriq.core.domain.account.payment.model.Currency;
import com.anchoriq.core.domain.account.payment.model.Payment;
import com.anchoriq.core.domain.account.payment.model.PaymentGatewayType;
import com.anchoriq.core.domain.account.user.model.Email;
import com.anchoriq.core.domain.account.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PaymentRepositoryImpl.class)
@DisplayName("PaymentRepository PostgreSQL 통합 테스트")
class PaymentRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("anchoriq_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private PaymentRepositoryImpl paymentRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Long userId;

    @BeforeEach
    void setUp() {
        User user = User.create(Email.of("payment@example.com"), "encoded-pw", "Payment User");
        User savedUser = entityManager.persistAndFlush(user);
        userId = savedUser.getId();
    }

    @Test
    @DisplayName("결제를 저장하고 ID로 조회할 수 있다")
    void should_saveAndFindById_when_paymentCreated() {
        // Given
        Payment payment = Payment.createSuccess(
                userId, PaymentGatewayType.STRIPE,
                "pi_test_123", BigDecimal.valueOf(29.99), Currency.USD);

        // When
        Payment saved = paymentRepository.save(payment);

        // Then
        assertThat(saved.getId()).isNotNull();
        Optional<Payment> found = paymentRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(29.99));
        assertThat(found.get().getCurrency()).isEqualTo(Currency.USD);
        assertThat(found.get().getGateway()).isEqualTo(PaymentGatewayType.STRIPE);
    }

    @Test
    @DisplayName("유저 ID로 결제 목록을 페이지네이션 조회할 수 있다")
    void should_findByUserIdPaginated_when_paymentsExist() {
        // Given
        Payment p1 = Payment.createSuccess(userId, PaymentGatewayType.STRIPE,
                "pi_1", BigDecimal.valueOf(29.99), Currency.USD);
        Payment p2 = Payment.createSuccess(userId, PaymentGatewayType.TOSS,
                "toss_1", BigDecimal.valueOf(33000), Currency.KRW);
        Payment p3 = Payment.createFailed(userId, PaymentGatewayType.STRIPE,
                BigDecimal.valueOf(29.99), Currency.USD);
        paymentRepository.save(p1);
        paymentRepository.save(p2);
        paymentRepository.save(p3);

        // When
        Page<Payment> page = paymentRepository.findByUserId(userId, PageRequest.of(0, 2));

        // Then
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("실패한 결제도 저장하고 조회할 수 있다")
    void should_saveFailedPayment_when_paymentFailed() {
        // Given
        Payment payment = Payment.createFailed(
                userId, PaymentGatewayType.TOSS,
                BigDecimal.valueOf(33000), Currency.KRW);

        // When
        Payment saved = paymentRepository.save(payment);

        // Then
        Optional<Payment> found = paymentRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().isSuccess()).isFalse();
        assertThat(found.get().getGatewayPaymentId()).isNull();
    }
}

package com.anchoriq.api.infrastructure.persistence.user;

import com.anchoriq.core.domain.account.user.model.Email;
import com.anchoriq.core.domain.account.user.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(UserRepositoryImpl.class)
@DisplayName("UserRepository PostgreSQL 통합 테스트")
class UserRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("anchoriq_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private UserRepositoryImpl userRepository;

    @Autowired
    private JpaUserRepository jpaUserRepository;

    @Test
    @DisplayName("유저를 저장하고 ID로 조회할 수 있다")
    void should_saveAndFindById_when_userCreated() {
        // Given
        User user = User.create(Email.of("test@example.com"), "encoded-pw-hash", "Test User");

        // When
        User saved = userRepository.save(user);

        // Then
        assertThat(saved.getId()).isNotNull();
        Optional<User> found = userRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getEmailValue()).isEqualTo("test@example.com");
        assertThat(found.get().getName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("이메일로 유저를 조회할 수 있다")
    void should_findByEmail_when_userExists() {
        // Given
        User user = User.create(Email.of("search@example.com"), "encoded-pw", "Search User");
        userRepository.save(user);

        // When
        Optional<User> found = userRepository.findByEmail("search@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Search User");
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 조회하면 빈 Optional을 반환한다")
    void should_returnEmpty_when_emailNotFound() {
        Optional<User> found = userRepository.findByEmail("nonexist@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("이메일 존재 여부를 확인할 수 있다")
    void should_returnTrue_when_emailExists() {
        // Given
        User user = User.create(Email.of("exists@example.com"), "encoded-pw", "Exists");
        userRepository.save(user);

        // When & Then
        assertThat(userRepository.existsByEmail("exists@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("notexists@example.com")).isFalse();
    }

    @Test
    @DisplayName("유저를 삭제할 수 있다")
    void should_delete_when_userExists() {
        // Given
        User user = User.create(Email.of("delete@example.com"), "encoded-pw", "Delete Me");
        User saved = userRepository.save(user);

        // When
        userRepository.deleteById(saved.getId());

        // Then
        assertThat(userRepository.findById(saved.getId())).isEmpty();
    }
}

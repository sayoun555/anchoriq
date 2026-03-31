package com.anchoriq.api.infrastructure.persistence.user;

import com.anchoriq.core.domain.account.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JpaUserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.email.value = :email")
    Optional<User> findByEmailValue(@Param("email") String email);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email.value = :email")
    boolean existsByEmailValue(@Param("email") String email);
}

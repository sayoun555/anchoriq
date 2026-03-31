package com.anchoriq.core.domain.account.user.repository;

import com.anchoriq.core.domain.account.user.model.User;

import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    void deleteById(Long id);
}

package com.anchoriq.api.infrastructure.persistence.apikey;

import com.anchoriq.core.domain.account.apikey.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByUserId(Long userId);

    Optional<ApiKey> findByKeyHash(String keyHash);

    void deleteByUserId(Long userId);
}

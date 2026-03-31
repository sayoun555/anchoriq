package com.anchoriq.core.domain.account.apikey.repository;

import com.anchoriq.core.domain.account.apikey.model.ApiKey;

import java.util.Optional;

public interface ApiKeyRepository {

    ApiKey save(ApiKey apiKey);

    Optional<ApiKey> findByUserId(Long userId);

    Optional<ApiKey> findByKeyHash(String keyHash);

    void deleteByUserId(Long userId);
}

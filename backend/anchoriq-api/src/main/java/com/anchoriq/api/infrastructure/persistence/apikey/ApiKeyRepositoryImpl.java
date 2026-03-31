package com.anchoriq.api.infrastructure.persistence.apikey;

import com.anchoriq.core.domain.account.apikey.model.ApiKey;
import com.anchoriq.core.domain.account.apikey.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ApiKeyRepositoryImpl implements ApiKeyRepository {

    private final JpaApiKeyRepository jpaApiKeyRepository;

    @Override
    public ApiKey save(ApiKey apiKey) {
        return jpaApiKeyRepository.save(apiKey);
    }

    @Override
    public Optional<ApiKey> findByUserId(Long userId) {
        return jpaApiKeyRepository.findByUserId(userId);
    }

    @Override
    public Optional<ApiKey> findByKeyHash(String keyHash) {
        return jpaApiKeyRepository.findByKeyHash(keyHash);
    }

    @Override
    public void deleteByUserId(Long userId) {
        jpaApiKeyRepository.deleteByUserId(userId);
    }
}

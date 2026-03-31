package com.anchoriq.core.domain.account.apikey.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "api_keys")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "key_hash", nullable = false)
    private String keyHash;

    @Column(length = 100)
    private String name;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private ApiKey(Long userId, String keyHash, String name) {
        this.userId = userId;
        this.keyHash = keyHash;
        this.name = name;
        this.createdAt = LocalDateTime.now();
    }

    public static ApiKey create(Long userId, String keyHash, String name) {
        return new ApiKey(userId, keyHash, name);
    }

    public void recordUsage() {
        this.lastUsedAt = LocalDateTime.now();
    }

    public void updateKeyHash(String newKeyHash) {
        this.keyHash = newKeyHash;
        this.lastUsedAt = null;
    }
}

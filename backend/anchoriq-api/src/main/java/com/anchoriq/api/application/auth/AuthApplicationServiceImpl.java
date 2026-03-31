package com.anchoriq.api.application.auth;

import com.anchoriq.api.dto.request.auth.LoginRequest;
import com.anchoriq.api.dto.request.auth.SignupRequest;
import com.anchoriq.api.dto.response.auth.AuthTokenResponse;
import com.anchoriq.api.dto.response.auth.UserResponse;
import com.anchoriq.api.infrastructure.security.JwtTokenProvider;
import com.anchoriq.core.common.exception.DuplicateException;
import com.anchoriq.core.common.exception.EntityNotFoundException;
import com.anchoriq.core.common.exception.InvalidCredentialsException;
import com.anchoriq.core.domain.account.subscription.factory.SubscriptionFactory;
import com.anchoriq.core.domain.account.subscription.model.Subscription;
import com.anchoriq.core.domain.account.subscription.repository.SubscriptionRepository;
import com.anchoriq.core.domain.account.user.model.Email;
import com.anchoriq.core.domain.account.user.model.User;
import com.anchoriq.core.domain.account.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthApplicationServiceImpl implements AuthApplicationService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionFactory subscriptionFactory;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${jwt.access-expiration}")
    private long accessExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Override
    @Transactional
    public UserResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateException("Email already registered: " + request.email());
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        User user = User.create(Email.of(request.email()), encodedPassword, request.name());
        User savedUser = userRepository.save(user);

        Subscription subscription = subscriptionFactory.createFreeSubscription(savedUser.getId());
        subscriptionRepository.save(subscription);

        log.info("User registered: email={}", request.email());
        return UserResponse.from(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthTokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmailValue(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        storeRefreshToken(user.getId(), refreshToken);

        log.info("User logged in: email={}", request.email());
        return AuthTokenResponse.of(accessToken, refreshToken, accessExpiration / 1000);
    }

    @Override
    public AuthTokenResponse refresh(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken) || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new InvalidCredentialsException();
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        String storedToken = stringRedisTemplate.opsForValue().get(buildRefreshTokenKey(userId));

        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", String.valueOf(userId)));

        String newAccessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmailValue(), user.getRole().name());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        storeRefreshToken(userId, newRefreshToken);

        return AuthTokenResponse.of(newAccessToken, newRefreshToken, accessExpiration / 1000);
    }

    @Override
    public void logout(Long userId) {
        stringRedisTemplate.delete(buildRefreshTokenKey(userId));
        log.info("User logged out: userId={}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", String.valueOf(userId)));
        return UserResponse.from(user);
    }

    private void storeRefreshToken(Long userId, String refreshToken) {
        try {
            stringRedisTemplate.opsForValue().set(
                    buildRefreshTokenKey(userId),
                    refreshToken,
                    Duration.ofMillis(refreshExpiration));
        } catch (Exception e) {
            log.warn("Failed to store refresh token in Redis, ignoring: {}", e.getMessage());
        }
    }

    private String buildRefreshTokenKey(Long userId) {
        return "auth:refresh:" + userId;
    }
}

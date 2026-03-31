package com.anchoriq.api.application.auth;

import com.anchoriq.api.dto.request.auth.LoginRequest;
import com.anchoriq.api.dto.request.auth.SignupRequest;
import com.anchoriq.api.dto.response.auth.AuthTokenResponse;
import com.anchoriq.api.dto.response.auth.UserResponse;

/**
 * 인증 Application Service 인터페이스.
 */
public interface AuthApplicationService {

    UserResponse signup(SignupRequest request);

    AuthTokenResponse login(LoginRequest request);

    AuthTokenResponse refresh(String refreshToken);

    void logout(Long userId);

    UserResponse getMe(Long userId);
}

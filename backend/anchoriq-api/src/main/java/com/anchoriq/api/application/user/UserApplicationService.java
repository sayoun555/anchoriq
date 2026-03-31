package com.anchoriq.api.application.user;

import com.anchoriq.api.dto.request.auth.PasswordChangeRequest;
import com.anchoriq.api.dto.request.auth.ProfileUpdateRequest;
import com.anchoriq.api.dto.response.auth.UserResponse;

/**
 * 유저 Application Service 인터페이스.
 */
public interface UserApplicationService {

    UserResponse updateProfile(Long userId, ProfileUpdateRequest request);

    void changePassword(Long userId, PasswordChangeRequest request);

    void deleteAccount(Long userId);
}

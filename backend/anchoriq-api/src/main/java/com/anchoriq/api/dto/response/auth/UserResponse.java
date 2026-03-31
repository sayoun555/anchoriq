package com.anchoriq.api.dto.response.auth;

import com.anchoriq.core.domain.account.user.model.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String name,
        String role,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmailValue(),
                user.getName(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }
}

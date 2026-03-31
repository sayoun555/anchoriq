package com.anchoriq.api.infrastructure.security;

public record UserPrincipal(Long userId, String email, String role) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}

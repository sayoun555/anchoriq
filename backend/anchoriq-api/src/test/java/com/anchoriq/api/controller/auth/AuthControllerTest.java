package com.anchoriq.api.controller.auth;

import com.anchoriq.api.application.auth.AuthApplicationService;
import com.anchoriq.api.dto.request.auth.LoginRequest;
import com.anchoriq.api.dto.request.auth.SignupRequest;
import com.anchoriq.api.dto.response.auth.AuthTokenResponse;
import com.anchoriq.api.dto.response.auth.UserResponse;
import com.anchoriq.api.infrastructure.security.JwtTokenProvider;
import com.anchoriq.api.infrastructure.security.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@DisplayName("AuthController API 엔드포인트 테스트")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthApplicationService authApplicationService;

    @MockitoBean
    private ApplicationEventPublisher eventPublisher;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("회원가입 성공 시 201 Created와 유저 정보를 반환한다")
    void should_return201_when_signupSuccessful() throws Exception {
        // Given
        SignupRequest request = new SignupRequest("test@example.com", "password123", "Test User");
        UserResponse response = new UserResponse(1L, "test@example.com", "Test User",
                "USER", LocalDateTime.now());
        when(authApplicationService.signup(any(SignupRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.name").value("Test User"));
    }

    @Test
    @DisplayName("이메일 형식이 잘못되면 400 Bad Request를 반환한다")
    void should_return400_when_invalidEmailFormat() throws Exception {
        // Given
        SignupRequest request = new SignupRequest("invalid-email", "password123", "Test User");

        // When & Then
        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("비밀번호가 8자 미만이면 400 Bad Request를 반환한다")
    void should_return400_when_passwordTooShort() throws Exception {
        // Given
        SignupRequest request = new SignupRequest("test@example.com", "short", "Test User");

        // When & Then
        mockMvc.perform(post("/api/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인 성공 시 200 OK와 JWT 토큰을 반환한다")
    void should_return200_when_loginSuccessful() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        AuthTokenResponse tokenResponse = AuthTokenResponse.of(
                "access-token-jwt", "refresh-token-jwt", 900);
        when(authApplicationService.login(any(LoginRequest.class))).thenReturn(tokenResponse);

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access-token-jwt"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token-jwt"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("토큰 갱신 성공 시 200 OK와 새 토큰을 반환한다")
    @WithMockUser
    void should_return200_when_refreshSuccessful() throws Exception {
        // Given
        AuthTokenResponse tokenResponse = AuthTokenResponse.of(
                "new-access-token", "new-refresh-token", 900);
        when(authApplicationService.refresh(any(String.class))).thenReturn(tokenResponse);

        String body = "{\"refreshToken\":\"old-refresh-token\"}";

        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"));
    }
}

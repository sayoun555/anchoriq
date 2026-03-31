package com.anchoriq.api.infrastructure.security;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import java.util.Base64;

/**
 * JWT 서명 키 설정.
 * SecretKey 생성 로직을 분리하여 JwtTokenProvider가 @RequiredArgsConstructor를 사용할 수 있게 한다.
 */
@Configuration
public class JwtConfig {

    @Bean
    public SecretKey jwtSecretKey(@Value("${jwt.secret}") String secret) {
        String base64Secret = Base64.getEncoder().encodeToString(secret.getBytes());
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
    }
}

package com.readverse.framework.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("readverse-unit-test-secret-key-at-least-32-bytes");
        properties.setAccessTtlSeconds(60);
        properties.setRefreshTtlSeconds(120);
        properties.setIssuer("readverse-test");
        jwtService = new JwtService(properties);
    }

    @Test
    void createsAndParsesAccessToken() {
        JwtService.TokenPair pair = jwtService.createTokens(42L, "reader");
        Claims claims = jwtService.parse(pair.accessToken());

        assertThat(jwtService.isAccess(claims)).isTrue();
        assertThat(jwtService.isRefresh(claims)).isFalse();
        assertThat(jwtService.getUserId(claims)).isEqualTo(42L);
        assertThat(jwtService.getUsername(claims)).isEqualTo("reader");
        assertThat(claims.getId()).isEqualTo(pair.accessJti());
    }

    @Test
    void createsRefreshTokenWithRefreshType() {
        JwtService.TokenPair pair = jwtService.createTokens(7L, "admin");
        Claims claims = jwtService.parse(pair.refreshToken());

        assertThat(jwtService.isRefresh(claims)).isTrue();
        assertThat(jwtService.isAccess(claims)).isFalse();
        assertThat(claims.getId()).isEqualTo(pair.refreshJti());
    }

    @Test
    void rejectsTamperedToken() {
        JwtService.TokenPair pair = jwtService.createTokens(1L, "reader");
        String tampered = pair.accessToken().substring(0, pair.accessToken().length() - 4) + "xxxx";

        assertThatThrownBy(() -> jwtService.parse(tampered)).isInstanceOf(JwtException.class);
    }
}

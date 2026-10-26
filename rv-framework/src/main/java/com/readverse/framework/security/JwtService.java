package com.readverse.framework.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties properties;

    public record TokenPair(String accessToken, String refreshToken, String accessJti, String refreshJti, long expiresIn) {
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public TokenPair createTokens(Long userId, String username) {
        String accessJti = UUID.randomUUID().toString().replace("-", "");
        String refreshJti = UUID.randomUUID().toString().replace("-", "");
        Instant now = Instant.now();
        String accessToken = buildToken(userId, username, accessJti, "access", now, properties.getAccessTtlSeconds());
        String refreshToken = buildToken(userId, username, refreshJti, "refresh", now, properties.getRefreshTtlSeconds());
        return new TokenPair(accessToken, refreshToken, accessJti, refreshJti, properties.getAccessTtlSeconds());
    }

    private String buildToken(Long userId, String username, String jti, String type, Instant now, long ttlSeconds) {
        return Jwts.builder()
                .id(jti)
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("type", type)
                .issuer(properties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key())
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getType(Claims claims) {
        return claims.get("type", String.class);
    }

    public Long getUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    public String getUsername(Claims claims) {
        return claims.get("username", String.class);
    }

    public boolean isAccess(Claims claims) {
        return "access".equals(getType(claims));
    }

    public boolean isRefresh(Claims claims) {
        return "refresh".equals(getType(claims));
    }
}

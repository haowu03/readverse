package com.readverse.system.vo;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserInfo user) {
}

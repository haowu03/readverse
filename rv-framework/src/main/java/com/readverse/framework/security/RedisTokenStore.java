package com.readverse.framework.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rv.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisTokenStore implements TokenStore {

    private static final String PREFIX = "rv:auth:token:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @Override
    public void save(String jti, LoginUser user, long ttlSeconds) {
        try {
            redis.opsForValue().set(PREFIX + jti, objectMapper.writeValueAsString(user), Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            throw new IllegalStateException("保存登录状态失败", e);
        }
    }

    @Override
    public LoginUser get(String jti) {
        try {
            String json = redis.opsForValue().get(PREFIX + jti);
            return json == null ? null : objectMapper.readValue(json, LoginUser.class);
        } catch (Exception e) {
            log.warn("读取登录状态失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public void remove(String jti) {
        try {
            redis.delete(PREFIX + jti);
        } catch (Exception e) {
            log.warn("删除登录状态失败: {}", e.getMessage());
        }
    }
}

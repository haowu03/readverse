package com.readverse.framework.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Lightweight Redis health check. The built-in Redis health indicator relies on INFO parsing,
 * which is brittle with older Windows Redis builds; a PING-like key lookup is enough here.
 */
@Component("rvRedis")
@RequiredArgsConstructor
public class RedisPingHealthIndicator implements HealthIndicator {

    private final StringRedisTemplate redis;

    @Value("${rv.redis.enabled:true}")
    private boolean enabled;

    @Override
    public Health health() {
        if (!enabled) {
            return Health.up().withDetail("enabled", false).build();
        }
        try {
            redis.hasKey("rv:health:ping");
            return Health.up().withDetail("enabled", true).build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}

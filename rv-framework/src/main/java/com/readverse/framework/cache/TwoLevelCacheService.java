package com.readverse.framework.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Caffeine L1 + Redis L2 cache. Redis failures degrade to local cache instead of failing requests.
 */
@Slf4j
@Service
public class TwoLevelCacheService {

    private final Cache<String, Object> local;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final CacheProperties properties;
    private final boolean redisEnabled;

    public TwoLevelCacheService(StringRedisTemplate redis,
                                ObjectMapper objectMapper,
                                CacheProperties properties,
                                @org.springframework.beans.factory.annotation.Value("${rv.redis.enabled:true}") boolean redisEnabled) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.redisEnabled = redisEnabled;
        this.local = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofSeconds(properties.getLocalTtlSeconds()))
                .recordStats()
                .build();
    }

    public <T> T get(String key, Class<T> type) {
        Object localValue = local.getIfPresent(key);
        if (localValue != null) {
            return type.cast(localValue);
        }
        if (!properties.isEnabled() || !redisEnabled) {
            return null;
        }
        try {
            String json = redis.opsForValue().get(key);
            if (!StringUtils.hasText(json)) {
                return null;
            }
            T value = objectMapper.readValue(json, type);
            local.put(key, value);
            return value;
        } catch (Exception e) {
            log.warn("Redis cache read failed for {}: {}", key, e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, TypeReference<T> typeReference) {
        Object localValue = local.getIfPresent(key);
        if (localValue != null) {
            return (T) localValue;
        }
        if (!properties.isEnabled() || !redisEnabled) {
            return null;
        }
        try {
            String json = redis.opsForValue().get(key);
            if (!StringUtils.hasText(json)) {
                return null;
            }
            T value = objectMapper.readValue(json, typeReference);
            local.put(key, value);
            return value;
        } catch (Exception e) {
            log.warn("Redis cache read failed for {}: {}", key, e.getMessage());
            return null;
        }
    }

    public void put(String key, Object value, long ttlSeconds) {
        local.put(key, value);
        if (!properties.isEnabled() || !redisEnabled || value == null) {
            return;
        }
        try {
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            log.warn("Redis cache write failed for {}: {}", key, e.getMessage());
        }
    }

    public void evict(String key) {
        local.invalidate(key);
        if (redisEnabled) {
            try {
                redis.delete(key);
            } catch (Exception e) {
                log.warn("Redis cache evict failed for {}: {}", key, e.getMessage());
            }
        }
    }

    public void evictAfterCommit(String key, long delayMillis) {
        local.invalidate(key);
        if (!redisEnabled) {
            return;
        }
        try {
            redis.delete(key);
            if (delayMillis > 0) {
                java.util.concurrent.CompletableFuture.delayedExecutor(delayMillis, TimeUnit.MILLISECONDS)
                        .execute(() -> {
                            try {
                                redis.delete(key);
                            } catch (Exception ignored) {
                            }
                        });
            }
        } catch (Exception e) {
            log.warn("Redis cache evict failed for {}: {}", key, e.getMessage());
        }
    }

    public CacheStats stats() {
        return local.stats();
    }
}

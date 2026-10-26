package com.readverse.framework.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fallback token store used when Redis is disabled (local demo / unit tests).
 */
@Component
@ConditionalOnProperty(name = "rv.redis.enabled", havingValue = "false")
public class InMemoryTokenStore implements TokenStore {

    private record Entry(LoginUser user, long expireAt) {
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public void save(String jti, LoginUser user, long ttlSeconds) {
        store.put(jti, new Entry(user, System.currentTimeMillis() + ttlSeconds * 1000));
    }

    @Override
    public LoginUser get(String jti) {
        Entry entry = store.get(jti);
        if (entry == null) {
            return null;
        }
        if (entry.expireAt() < System.currentTimeMillis()) {
            store.remove(jti);
            return null;
        }
        return entry.user();
    }

    @Override
    public void remove(String jti) {
        store.remove(jti);
    }
}

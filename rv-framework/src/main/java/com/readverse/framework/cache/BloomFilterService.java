package com.readverse.framework.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.BitSet;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bloom filter implemented on Redis bitmap (GETBIT/SETBIT), so it works without the RedisBloom module.
 * A local BitSet mirror is used when Redis is disabled/unavailable.
 */
@Slf4j
@Service
public class BloomFilterService {

    private static final String KEY = "rv:bloom:chapter";
    private static final int BIT_SIZE = 1 << 24;
    private static final int MASK = BIT_SIZE - 1;
    private static final int HASHES = 4;

    private final StringRedisTemplate redis;
    private final CacheProperties properties;
    private final boolean redisEnabled;
    private final BitSet localBits = new BitSet(BIT_SIZE);
    private final AtomicLong added = new AtomicLong();

    public BloomFilterService(StringRedisTemplate redis,
                              CacheProperties properties,
                              @Value("${rv.redis.enabled:true}") boolean redisEnabled) {
        this.redis = redis;
        this.properties = properties;
        this.redisEnabled = redisEnabled;
    }

    public void add(Long chapterId) {
        if (chapterId == null || !properties.isBloomEnabled()) {
            return;
        }
        int[] indexes = indexes(String.valueOf(chapterId));
        synchronized (localBits) {
            for (int index : indexes) {
                localBits.set(index);
            }
        }
        if (redisEnabled) {
            try {
                for (int index : indexes) {
                    redis.opsForValue().setBit(KEY, index, true);
                }
            } catch (Exception e) {
                log.warn("Bloom add failed: {}", e.getMessage());
            }
        }
        added.incrementAndGet();
    }

    public boolean mightContain(Long chapterId) {
        if (chapterId == null || !properties.isBloomEnabled()) {
            return true;
        }
        int[] indexes = indexes(String.valueOf(chapterId));
        if (redisEnabled) {
            try {
                // If Redis was flushed/restarted, fail open instead of false-negative 404s.
                if (!Boolean.TRUE.equals(redis.hasKey(KEY))) {
                    return true;
                }
                for (int index : indexes) {
                    Boolean bit = redis.opsForValue().getBit(KEY, index);
                    if (!Boolean.TRUE.equals(bit)) {
                        return false;
                    }
                }
                return true;
            } catch (Exception e) {
                log.warn("Bloom read failed, fallback to local: {}", e.getMessage());
            }
        }
        synchronized (localBits) {
            for (int index : indexes) {
                if (!localBits.get(index)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void init(Collection<Long> chapterIds) {
        if (chapterIds == null || chapterIds.isEmpty()) {
            return;
        }
        chapterIds.forEach(this::add);
        log.info("Bloom filter warmed with {} chapter ids", added.get());
    }

    public long size() {
        return added.get();
    }

    private int[] indexes(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            int[] result = new int[HASHES];
            for (int i = 0; i < HASHES; i++) {
                int offset = i * 4;
                int value32 = ((bytes[offset] & 0xff) << 24)
                        | ((bytes[offset + 1] & 0xff) << 16)
                        | ((bytes[offset + 2] & 0xff) << 8)
                        | (bytes[offset + 3] & 0xff);
                result[i] = Math.abs(value32) & MASK;
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("MD5 not available", e);
        }
    }
}

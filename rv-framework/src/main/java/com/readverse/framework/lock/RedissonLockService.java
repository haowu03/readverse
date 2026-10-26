package com.readverse.framework.lock;

import com.readverse.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Slf4j
@Service
public class RedissonLockService implements DistributedLock {

    private final RedissonClient redisson;
    private final Map<String, ReentrantLock> localLocks = new ConcurrentHashMap<>();

    public RedissonLockService(ObjectProvider<RedissonClient> redissonProvider) {
        this.redisson = redissonProvider.getIfAvailable();
    }

    @Override
    public <T> T execute(String key, long waitSeconds, long leaseSeconds, Supplier<T> action) {
        if (redisson == null) {
            return local(key, action);
        }
        RLock lock = redisson.getLock("rv:lock:" + key);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitSeconds, leaseSeconds, TimeUnit.SECONDS);
            if (!acquired) {
                throw new BizException("系统繁忙，请稍后重试");
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException("获取锁被中断");
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private <T> T local(String key, Supplier<T> action) {
        ReentrantLock lock = localLocks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}

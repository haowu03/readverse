package com.readverse.framework.lock;

import java.util.function.Supplier;

public interface DistributedLock {

    <T> T execute(String key, long waitSeconds, long leaseSeconds, Supplier<T> action);
}

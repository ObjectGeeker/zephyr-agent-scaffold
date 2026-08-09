package com.object.ai.common.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * 通用的进程内本地缓存管理器。
 *
 * @param <K> 缓存 key 类型
 * @param <V> 缓存 value 类型
 */
public final class LocalCacheManager<K, V> {

    public static final long DEFAULT_MAXIMUM_SIZE = 100;
    public static final Duration DEFAULT_EXPIRE_AFTER_ACCESS = Duration.ofMinutes(30);

    private final Cache<K, V> cache;

    public LocalCacheManager() {
        this(DEFAULT_MAXIMUM_SIZE, DEFAULT_EXPIRE_AFTER_ACCESS);
    }

    public LocalCacheManager(long maximumSize, Duration expireAfterAccess) {
        if (maximumSize <= 0) {
            throw new IllegalArgumentException("maximumSize must be greater than zero");
        }
        Objects.requireNonNull(expireAfterAccess, "expireAfterAccess must not be null");
        if (expireAfterAccess.isZero() || expireAfterAccess.isNegative()) {
            throw new IllegalArgumentException("expireAfterAccess must be greater than zero");
        }
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterAccess(expireAfterAccess)
                .build();
    }

    /**
     * 获取缓存值，不存在时通过 loader 原子加载。
     */
    public V get(K key, Callable<? extends V> loader) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(loader, "loader must not be null");
        try {
            return cache.get(key, loader);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new IllegalStateException("Failed to load local cache value", cause);
        }
    }

    /**
     * 写入缓存值。
     *
     * @param key 缓存 key
     * @param value 缓存 value
     */
    public void put(K key, V value) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        cache.put(key, value);
    }

    /**
     * 获取缓存值，不存在时返回 null，不触发加载。
     */
    public V getIfPresent(K key) {
        return key == null ? null : cache.getIfPresent(key);
    }

    public void invalidate(K key) {
        if (key != null) {
            cache.invalidate(key);
        }
    }

    public void clear() {
        cache.invalidateAll();
    }
}

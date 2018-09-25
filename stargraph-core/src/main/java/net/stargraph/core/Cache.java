package net.stargraph.core;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.ExecutionException;

public abstract class Cache<X, Y> {
    private LoadingCache<X, Y> cache;

    public Cache(long maximumSize) {
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(maximumSize)
                //.expireAfterWrite(10, TimeUnit.MINUTES)
                .build(
                        new CacheLoader<X, Y>() {
                            public Y load(X key) throws Exception {
                                return generateValue(key);
                            }
                        });
    }

    public abstract Y generateValue(X key) throws Exception;

    public Y get(X key) throws ExecutionException {
        return cache.get(key);
    }
}

package com.microsoft.azuretools.azurecommons.rediscacheprocessors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.microsoft.azure.management.redis.RedisCaches;


@Singleton
public class RedisCacheHelper {
    
    private final RedisCaches redisCaches;
    
    @Inject
    public RedisCacheHelper(RedisCaches redisCaches) {
        this.redisCaches = redisCaches;
    }

    public void deleteRedisById(String id) {
        redisCaches.deleteById(id);
    }

}

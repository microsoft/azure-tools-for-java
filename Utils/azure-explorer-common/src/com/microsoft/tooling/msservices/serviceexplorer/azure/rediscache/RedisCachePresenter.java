package com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache;

import javax.inject.Inject;

import com.microsoft.azure.management.redis.RedisCache;
import com.microsoft.azuretools.azurecommons.rediscacheprocessors.RedisCacheHelper;
import com.microsoft.tooling.msservices.serviceexplorer.azure.base.BasePresenter;

public class RedisCachePresenter<V extends RedisCacheMvpView> extends BasePresenter<V> 
    implements RedisCacheMvpPresenter<V> {
    
    
    private final RedisCacheHelper redisCacheHelper;
    
    @Inject
    public RedisCachePresenter(RedisCacheHelper redisCacheHelper) {
        this.redisCacheHelper = redisCacheHelper;
    }

    @Override
    public void onRedisCacheDelete(RedisCache redisCache, RedisCacheNode node) {
        if (redisCache == null) {
            getMvpView().onError("Redis Cache is null");
            return;
        }
        String id = redisCache.id();
        if (id == null || id.trim().isEmpty()) {
            getMvpView().onError("Cannot get Redis Cache's ID");
            return;
        }
        redisCacheHelper.deleteRedisById(id);
        getMvpView().onRemoveNode(node);
    }
}

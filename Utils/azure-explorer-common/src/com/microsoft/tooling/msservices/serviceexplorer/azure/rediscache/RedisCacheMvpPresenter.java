package com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache;

import com.microsoft.azure.management.redis.RedisCache;
import com.microsoft.tooling.msservices.serviceexplorer.azure.base.MvpPresenter;

public interface RedisCacheMvpPresenter<V extends RedisCacheMvpView> extends MvpPresenter<V>{

    void onRedisCacheDelete(RedisCache redisCache, RedisCacheNode node);
    
}

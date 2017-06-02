package com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache;

import com.microsoft.azure.management.redis.RedisCaches;
import com.microsoft.tooling.msservices.serviceexplorer.azure.base.BasePresenter;

public class RedisCachePresenter<V extends RedisCacheMvpView> extends BasePresenter<V> 
    implements RedisCacheMvpPresenter<V> {
    
    private final RedisCaches redisCaches;
    
    public RedisCachePresenter(RedisCaches redisCaches) {
        this.redisCaches = redisCaches;
    }

    @Override
    public void onRedisCacheDelete(String id, RedisCacheNode node) {
        if (id == null || id.trim().isEmpty()) {
            getMvpView().onError("Cannot get Redis Cache's ID");
            return;
        }
        redisCaches.deleteById(id);
        getMvpView().onRemoveNode(node);
    }

}

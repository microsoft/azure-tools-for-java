package com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache;

import java.io.IOException;
import java.util.HashMap;

import com.microsoft.azure.management.redis.RedisCaches;
import com.microsoft.azuretools.azurecommons.azuremodel.AzureModel;
import com.microsoft.azuretools.azurecommons.mvp.ui.base.BasePresenter;

public class RedisCachePresenter<V extends RedisCacheMvpView> extends BasePresenter<V> 
    implements RedisCacheMvpPresenter<V> {
    
    
    public RedisCachePresenter() { }

    @Override
    public void onRedisCacheDelete(String sid, String id, RedisCacheNode node) {
        if (sid == null || sid.trim().isEmpty()) {
            getMvpView().onError("Cannot get Subscription ID.");
            return;
        }
        if (id == null || id.trim().isEmpty()) {
            getMvpView().onError("Cannot get Redis Cache's ID.");
            return;
        }
        try {
            AzureModel.deleteRedisCache(sid, id);
        } catch (IOException e) {
            getMvpView().OnErrorWithException("Cannot delete Redis Cache.", e);
            return;
        }
        getMvpView().onRemoveNode(node);
    }

    @Override
    public void onRedisCacheRefresh() {
        HashMap<String, RedisCaches> redisCachesMap;
        try {
            redisCachesMap = AzureModel.getRedisCaches();
        } catch (IOException e) {
            getMvpView().OnErrorWithException("Cannot get Redis Cache's ID.", e);
            return;
        }
        getMvpView().onRefreshNode(redisCachesMap);
    }
}

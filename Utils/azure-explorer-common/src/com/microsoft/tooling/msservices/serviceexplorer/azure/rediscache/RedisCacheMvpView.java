package com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache;

import java.util.HashMap;

import com.microsoft.azure.management.redis.RedisCaches;
import com.microsoft.azuretools.azurecommons.mvp.ui.base.MvpView;

public interface RedisCacheMvpView extends MvpView {
    
    void onRemoveNode(RedisCacheNode redisCacheNode);
    
    void onRefreshNode(HashMap<String, RedisCaches> redisCachesMap);

}

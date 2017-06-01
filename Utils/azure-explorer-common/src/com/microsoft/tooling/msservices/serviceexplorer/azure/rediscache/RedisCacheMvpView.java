package com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache;

import com.microsoft.tooling.msservices.serviceexplorer.azure.base.MvpView;

public interface RedisCacheMvpView extends MvpView {
    
    void onRemoveNode(RedisCacheNode redisCacheNode);

}

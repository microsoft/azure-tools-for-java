package com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache;

import com.microsoft.tooling.msservices.serviceexplorer.azure.base.MvpPresenter;

public interface RedisCacheMvpPresenter<V extends RedisCacheMvpView> extends MvpPresenter<V>{

    void onRedisCacheDelete(String id, RedisCacheNode node);
    
}

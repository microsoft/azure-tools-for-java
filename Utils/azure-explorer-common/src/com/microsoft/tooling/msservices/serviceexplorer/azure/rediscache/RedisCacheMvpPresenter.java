package com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache;

import com.microsoft.azuretools.azurecommons.mvp.ui.base.MvpPresenter;

public interface RedisCacheMvpPresenter<V extends RedisCacheMvpView> extends MvpPresenter<V>{

    void onRedisCacheDelete(String sid, String id, RedisCacheNode node);
    
    void onRedisCacheRefresh();
    
}

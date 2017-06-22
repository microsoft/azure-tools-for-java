package com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache;

import com.microsoft.azuretools.azurecommons.mvp.ui.base.MvpPresenter;
import com.microsoft.azuretools.core.model.rediscache.RedisConnectionPools;
import com.microsoft.azuretools.core.model.rediscache.RedisExplorerMvpModel;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import rx.Observable;
import rx.schedulers.Schedulers;

public class RedisExplorerPresenter<V extends RedisExplorerMvpView> extends MvpPresenter<V> {
    
    private static final String CANNOT_GET_REDIS_INFO = "Cannot get Redis Cache's information.";

    public void onReadDbNum(String sid, String id) {
        Observable.fromCallable(() -> {
            return RedisExplorerMvpModel.getInstance().getDbNumber(sid, id);
        })
        .subscribeOn(Schedulers.io())
        .subscribe(number -> {
          DefaultLoader.getIdeHelper().invokeLater(() -> {
              getMvpView().renderDbCombo(number.intValue());
          });
        }, e -> {
            getMvpView().onErrorWithException(CANNOT_GET_REDIS_INFO, (Exception) e);
        });
    }
    
    public void onRelease(String id) {
        RedisConnectionPools.getInstance().releasePool(id);
    }
}

/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache;

import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.core.mvp.model.rediscache.AzureRedisMvpModel;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.azuretools.core.mvp.ui.rediscache.RedisCacheProperty;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import rx.Observable;

public class RedisPropertyViewPresenter<V extends RedisPropertyMvpView> extends MvpPresenter<V> {

    private static final String CANNOT_GET_SUBSCRIPTION_ID = "Cannot get Subscription ID.";
    private static final String CANNOT_GET_REDIS_ID = "Cannot get Redis Cache's ID.";
    private static final String CANNOT_GET_REDIS_PROPERTY = "Cannot get Redis Cache's property.";

    /**
     * Called from view when the view needs to show the property.
     *
     * @param sid
     *            Subscription Id
     * @param id
     *            Redis Cache's Id
     */
    public void onGetRedisProperty(String sid, String id) {
        if (Utils.isEmptyString(sid)) {
            getMvpView().onError(CANNOT_GET_SUBSCRIPTION_ID);
            return;
        }
        if (Utils.isEmptyString(id)) {
            getMvpView().onError(CANNOT_GET_REDIS_ID);
            return;
        }
        if (!(getMvpView() instanceof RedisPropertyMvpView)) {
            return;
        }
        Observable.fromCallable(() -> {
            return AzureRedisMvpModel.getInstance().getRedisCache(sid, id);
        })
        .subscribeOn(getSchedulerProvider().io())
        .subscribe(redis -> {
            DefaultLoader.getIdeHelper().invokeLater(() -> {
                if (isViewDetached()) {
                    return;
                }
                if (redis == null) {
                    getMvpView().onError(CANNOT_GET_REDIS_PROPERTY);
                    return;
                }
                RedisCacheProperty property = new RedisCacheProperty(redis.name(), redis.type(), redis.resourceGroupName(),
                      redis.regionName(), sid, redis.redisVersion(), redis.sslPort(), redis.nonSslPort(),
                      redis.keys().primaryKey(), redis.keys().secondaryKey(), redis.hostName());
                getMvpView().showProperty(property);
            });
        }, e -> {
            errorHandler(CANNOT_GET_REDIS_PROPERTY, (Exception) e);
        });
    }

    private void errorHandler(String msg, Exception e) {
        DefaultLoader.getIdeHelper().invokeLater(() -> {
            if (isViewDetached()) {
                return;
            }
            getMvpView().onErrorWithException(msg, e);
        });
    }
}

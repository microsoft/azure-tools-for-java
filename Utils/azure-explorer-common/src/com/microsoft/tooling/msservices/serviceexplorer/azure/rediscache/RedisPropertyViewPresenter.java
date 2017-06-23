/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache;

import com.microsoft.azuretools.azurecommons.mvp.ui.base.MvpPresenter;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.core.model.AzureMvpModelHelper;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisCacheProperty;

import rx.Observable;
import rx.schedulers.Schedulers;

public class RedisPropertyViewPresenter<V extends RedisPropertyMvpView> extends MvpPresenter<V> {

    private static final String CANNOT_GET_SUBCROPTION_ID = "Cannot get Subscription ID.";
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
            getMvpView().onError(CANNOT_GET_SUBCROPTION_ID);
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
            return AzureMvpModelHelper.getInstance().getRedisCache(sid, id);
        })
        .subscribeOn(Schedulers.io())
        .subscribe(redis -> {
            if (redis == null) {
                getMvpView().onError(CANNOT_GET_REDIS_PROPERTY);
                return;
            }
            RedisCacheProperty property = new RedisCacheProperty(redis.name(), redis.type(), redis.resourceGroupName(),
                  redis.regionName(), sid, redis.redisVersion(), redis.sslPort(), redis.nonSslPort(),
                  redis.keys().primaryKey(), redis.keys().secondaryKey(), redis.hostName());
            DefaultLoader.getIdeHelper().invokeLater(() -> {
                getMvpView().showProperty(property);
            });
        }, e -> {
            getMvpView().onErrorWithException(CANNOT_GET_REDIS_PROPERTY, (Exception) e);
        });
    }
}

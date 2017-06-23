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
import com.microsoft.azuretools.azurecommons.mvp.ui.base.RedisScanResult;
import com.microsoft.azuretools.core.model.rediscache.RedisConnectionPools;
import com.microsoft.azuretools.core.model.rediscache.RedisExplorerMvpModel;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import rx.Observable;
import rx.schedulers.Schedulers;

public class RedisExplorerPresenter<V extends RedisExplorerMvpView> extends MvpPresenter<V> {

    private static final String CANNOT_GET_REDIS_INFO = "Cannot get Redis Cache's information.";

    /**
     * Called when the explorer needs the number of databases in Redis Cache.
     * 
     * @param sid
     *            subscription id of Redis Cache
     * @param id
     *            resource id of Redis Cache
     */
    public void onReadDbNum(String sid, String id) {
        Observable.fromCallable(() -> {
            return RedisExplorerMvpModel.getInstance().getDbNumber(sid, id);
        })
        .subscribeOn(Schedulers.io())
        .subscribe(number -> {
            DefaultLoader.getIdeHelper().invokeLater(() -> {
                getMvpView().renderDbCombo(number);
            });
        }, e -> {
            getMvpView().onErrorWithException(CANNOT_GET_REDIS_INFO, (Exception) e);
        });
    }
    
    /**
     * Called when the database combo selection event is fired.
     * 
     * @param sid
     *             subscription id of Redis Cache
     * @param id
     *             resource id of Redis Cache
     * @param db
     *             index of Redis Cache database
     * @param cursor
     *             cursor for Redis Scan command
     * @param pattern
     *             pattern for Redis Scan Param
     */
    public void onDbSelect(String sid, String id, int db) {
        Observable.fromCallable(() -> {
            return RedisExplorerMvpModel.getInstance().scanKeys(sid, id, db, "", "");
        })
        .subscribeOn(Schedulers.io())
        .subscribe(result -> {
            DefaultLoader.getIdeHelper().invokeLater(() -> {
                getMvpView().showScanResult(new RedisScanResult(result));
            });
        }, e -> {
            getMvpView().onErrorWithException(CANNOT_GET_REDIS_INFO, (Exception) e);
        });
    }

    /**
     * Called when the jedis pool needs to be released.
     * 
     * @param id
     *             resource id of Redis Cache
     */
    public void onRelease(String id) {
        RedisConnectionPools.getInstance().releasePool(id);
    }
}

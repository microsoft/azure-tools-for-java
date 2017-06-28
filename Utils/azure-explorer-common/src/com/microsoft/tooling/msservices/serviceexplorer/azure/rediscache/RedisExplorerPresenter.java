/**
 * Copyright (c) Microsoft Corporation
 * 
 * All rights reserved.
 * 
 * MIT License
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * 
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache;

import static redis.clients.jedis.ScanParams.SCAN_POINTER_START;

import com.microsoft.azuretools.azurecommons.helpers.RedisKeyType;
import com.microsoft.azuretools.azurecommons.mvp.ui.base.MvpPresenter;
import com.microsoft.azuretools.azurecommons.mvp.ui.base.RedisScanResult;
import com.microsoft.azuretools.azurecommons.mvp.ui.base.RedisValueData;
import com.microsoft.azuretools.core.model.rediscache.RedisConnectionPools;
import com.microsoft.azuretools.core.model.rediscache.RedisExplorerMvpModel;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import redis.clients.jedis.ScanResult;
import redis.clients.jedis.Tuple;
import rx.Observable;
import rx.schedulers.Schedulers;

public class RedisExplorerPresenter<V extends RedisExplorerMvpView> extends MvpPresenter<V> {
    
    private String sid;
    private String id;

    private static final String DEFAULT_SCAN_PATTERN = "*";

    private static final String CANNOT_GET_REDIS_INFO = "Cannot get Redis Cache's information.";

    /**
     * Called when the explorer needs the number of databases in Redis Cache.
     * 
     * @param sid
     *            subscription id of Redis Cache
     * @param id
     *            resource id of Redis Cache
     */
    public void onReadDbNum() {
        Observable.fromCallable(() -> {
            return RedisExplorerMvpModel.getInstance().getDbNumber(sid, id);
        })
        .subscribeOn(Schedulers.io())
        .subscribe(number -> {
            DefaultLoader.getIdeHelper().invokeLater(() -> {
                if (isViewDetached()) {
                    return;
                }
                getMvpView().renderDbCombo(number);
            });
        }, e -> {
            errorHandler(CANNOT_GET_REDIS_INFO, (Exception) e);
        });
    }

    /**
     * Called when the database combo selection event is fired.
     * 
     * @param sid
     *            subscription id of Redis Cache
     * @param id
     *            resource id of Redis Cache
     * @param db
     *            index of Redis Cache database
     */
    public void onDbSelect(int db) {
        onKeyList(db, SCAN_POINTER_START, DEFAULT_SCAN_PATTERN);
    }

    /**
     * Called when Scan button is clicked.
     * 
     * @param sid
     *            subscription id of Redis Cache
     * @param id
     *            resource id of Redis Cache
     * @param db
     *            index of Redis Cache database
     * @param cursor
     *            scan cursor for Redis Cache
     * @param pattern
     *            scan match pattern for Redis Cache
     */
    public void onKeyList(int db, String cursor, String pattern) {
        Observable.fromCallable(() -> {
            return RedisExplorerMvpModel.getInstance().scanKeys(sid, id, db, cursor, pattern);
        })
        .subscribeOn(Schedulers.io())
        .subscribe(result -> {
            if (isViewDetached()) {
                return;
            }
            DefaultLoader.getIdeHelper().invokeLater(() -> {
                getMvpView().showScanResult(new RedisScanResult(result));
            });
        }, e -> {
            errorHandler(CANNOT_GET_REDIS_INFO, (Exception) e);
        });
    }

    /**
     * Called when one list item is selected.
     * 
     * @param sid
     *            subscription id of Redis Cache
     * @param id
     *            resource id of Redis Cache
     * @param db
     *            index of Redis Cache database
     * @param key
     *            target key name for Redis Cache
     */
    public void onkeySelect(int db, String key) {
        Observable.fromCallable(() -> {
            String type = RedisExplorerMvpModel.getInstance().getKeyType(sid, id, db, key).toUpperCase();
            ArrayList<String[]> columnData = new ArrayList<String[]>();
            switch (RedisKeyType.valueOf(type)) {
                case STRING:
                    String stringVal = RedisExplorerMvpModel.getInstance().getStringValue(sid, id, db, key);
                    columnData.add(new String[] { stringVal });
                    return new RedisValueData(columnData, RedisKeyType.STRING);
                case LIST:
                    List<String> listVal = RedisExplorerMvpModel.getInstance().getListValue(sid, id, db, key);
                    for (int i = 0; i < listVal.size(); i++) {
                        columnData.add(new String[] { String.valueOf(i + 1), listVal.get(i) });
                    }
                    return new RedisValueData(columnData, RedisKeyType.LIST);
                case SET:
                    ScanResult<String> setVal = RedisExplorerMvpModel.getInstance().getSetValue(sid, id, db, key,
                            SCAN_POINTER_START);
                    for (String row : setVal.getResult()) {
                        columnData.add(new String[] { row });
                    }
                    return new RedisValueData(columnData, RedisKeyType.SET);
                case ZSET:
                    Set<Tuple> zsetVal = RedisExplorerMvpModel.getInstance().getZSetValue(sid, id, db, key);
                    for (Tuple tuple : zsetVal) {
                        columnData.add(new String[] { String.valueOf(tuple.getScore()), tuple.getElement() });
                    }
                    return new RedisValueData(columnData, RedisKeyType.ZSET);
                case HASH:
                    ScanResult<Entry<String, String>> hashVal = RedisExplorerMvpModel.getInstance().getHashValue(sid,
                            id, db, key, SCAN_POINTER_START);
                    for (Entry<String, String> hash : hashVal.getResult()) {
                        columnData.add(new String[] { hash.getKey(), hash.getValue() });
                    }
                    return new RedisValueData(columnData, RedisKeyType.HASH);
                default:
                    return null;

            }
        })
        .subscribeOn(Schedulers.io())
        .subscribe(result -> {
            if (isViewDetached()) {
                return;
            }
            if (result == null) {
                getMvpView().onError(CANNOT_GET_REDIS_INFO);
                return;
            }
            DefaultLoader.getIdeHelper().invokeLater(() -> {
                getMvpView().showContent(result);
            });
        }, e -> {
            errorHandler(CANNOT_GET_REDIS_INFO, (Exception) e);
        });
    }

    /**
     * Called when the jedis pool needs to be released.
     * 
     * @param id
     *            resource id of Redis Cache
     */
    public void onRelease(String id) {
        RedisConnectionPools.getInstance().releasePool(id);
    }
    
    public void initializeResourceData(String sid, String id) {
        this.sid = sid;
        this.id = id;
    }
    
    private void errorHandler(String msg, Exception e) {
        if (isViewDetached()) {
            return;
        }
        DefaultLoader.getIdeHelper().invokeLater(() -> {
            getMvpView().onErrorWithException(msg, e);
        });
    }
}

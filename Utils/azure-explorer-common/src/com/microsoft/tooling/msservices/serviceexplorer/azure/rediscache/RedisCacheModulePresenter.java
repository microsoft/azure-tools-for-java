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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.microsoft.azuretools.azurecommons.mvp.ui.base.MvpPresenter;
import com.microsoft.azuretools.core.model.AzureMvpModelHelper;
import com.microsoft.azuretools.core.model.NodeContent;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

public class RedisCacheModulePresenter<V extends RedisCacheModule> extends MvpPresenter<V> {
    
	private static final String CANNOT_GET_SUBCROPTION_ID = "Cannot get Subscription ID.";
    private static final String CANNOT_GET_REDIS_ID = "Cannot get Redis Cache's ID.";
    private static final String CANNOT_DELETE_REDIS = "Cannot delete Redis Cache.";
    
    private final AzureMvpModelHelper azureMvpModelHelper = AzureMvpModelHelper.getInstance();
    
    public void onModuleRefresh() {
        HashMap<String, ArrayList<NodeContent>> redisCachesMap;
        try {
            redisCachesMap = azureMvpModelHelper.getRedisCaches();
        } catch (IOException e) {
            getMvpView().onErrorWithException(CANNOT_GET_REDIS_ID, e);
            return;
        }
        getMvpView().onRefreshNode(redisCachesMap);
    }
    
    public void onNodeDelete(String sid, String id, Node node) {
        if (sid == null || sid.trim().isEmpty()) {
            getMvpView().onError(CANNOT_GET_SUBCROPTION_ID);
            return;
        }
        if (id == null || id.trim().isEmpty()) {
            getMvpView().onError(CANNOT_GET_REDIS_ID);
            return;
        }
        try {
            azureMvpModelHelper.deleteRedisCache(sid, id);
        } catch (IOException e) {
            getMvpView().onErrorWithException(CANNOT_DELETE_REDIS, e);
            return;
        }
        getMvpView().removeDirectChildNode(node);
    }
}

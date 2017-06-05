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

package com.microsoft.azuretools.core.model;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.redis.RedisCaches;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.sdkmanage.AzureManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

public class AzureMvpModel {
    
    private AzureMvpModel() {}
    
    private static final class  AzureMvpModelHolder {
        private static final AzureMvpModel INSTANCE = new AzureMvpModel();
    }
    
    public static AzureMvpModel getInstance() {
        return AzureMvpModelHolder.INSTANCE;
    }
    
    /**
     * Get all redis caches.
     * @return A map containing RedisCaches with subscription id as the key
     * @throws IOException getAzureManager Exception
     */
    public HashMap<String, RedisCaches> getRedisCaches() throws IOException {
        HashMap<String, RedisCaches> redisCacheMaps = new HashMap<String, RedisCaches>();
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        if (azureManager == null) {
            return redisCacheMaps;
        }
        SubscriptionManager subscriptionManager = azureManager.getSubscriptionManager();
        if (subscriptionManager == null) {
            return redisCacheMaps;
        }
        Set<String> sidList = subscriptionManager.getAccountSidList();
        for (String sid : sidList) {
            Azure azure = azureManager.getAzure(sid);
            if (azure == null) {
                continue;
            }
            redisCacheMaps.put(sid, azure.redisCaches());
        }
        return redisCacheMaps;
    }
    
    /**
     * Delete a redis cache.
     * @param sid Subscription Id
     * @param id Redis cache's id
     * @throws IOException getAzureManager Exception
     */
    public void deleteRedisCache(String sid, String id) throws IOException {
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        if (azureManager == null) {
            return;
        }
        Azure azure = azureManager.getAzure(sid);
        if (azure == null) {
            return;
        }
        RedisCaches redisCaches = azure.redisCaches();
        if (redisCaches == null) {
            return;
        }
        redisCaches.deleteById(id);
    }
}

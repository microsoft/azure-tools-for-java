package com.microsoft.azuretools.azurecommons.azuremodel;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.redis.RedisCaches;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.sdkmanage.AzureManager;

public class AzureModel {
    public static HashMap<String, RedisCaches> getRedisCaches() throws IOException {
        HashMap<String, RedisCaches> redisCacheMaps = new HashMap<String, RedisCaches>();
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        if (azureManager == null) {
            return redisCacheMaps;
        }
        SubscriptionManager subscriptionManager = azureManager.getSubscriptionManager();
        Set<String> sidList = subscriptionManager.getAccountSidList();
        for (String sid : sidList) {
            Azure azure = azureManager.getAzure(sid);
            redisCacheMaps.put(sid, azure.redisCaches());
        }
        return redisCacheMaps;
    }
    
    public static void deleteRedisCache(String sid, String id) throws IOException {
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        if (azureManager == null) {
            return;
        }
        Azure azure = azureManager.getAzure(sid);
        RedisCaches redisCaches = azure.redisCaches();
        redisCaches.deleteById(id);
    }
}

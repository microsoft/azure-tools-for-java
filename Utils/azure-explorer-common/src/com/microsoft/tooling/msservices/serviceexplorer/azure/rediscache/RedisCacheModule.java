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

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.redis.RedisCache;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class RedisCacheModule extends AzureRefreshableNode {
    private static final String REDIS_SERVICE_MODULE_ID = com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisCacheModule.class.getName();
    private static final String ICON_PATH = "RedisCache.png";
    private static final String BASE_MODULE_NAME = "Redis Caches";

    public RedisCacheModule(Node parent) {
        super(REDIS_SERVICE_MODULE_ID, BASE_MODULE_NAME, parent, ICON_PATH);
    }
    @Override
    protected void refreshItems() throws AzureCmdException {
        List<Pair<String, String>> failedSubscriptions = new ArrayList<>();
        try {
            AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
            // not signed in
            if (azureManager == null) {
                return;
            }

            SubscriptionManager subscriptionManager = azureManager.getSubscriptionManager();
            Set<String> sidList = subscriptionManager.getAccountSidList();
            for (String sid : sidList) {
                try {
                    Azure azure = azureManager.getAzure(sid);
                    for (RedisCache cache : azure.redisCaches().list())
                    {
                        addChildNode(new RedisCacheNode(this, sid, cache));
                    }
                } catch (Exception ex) {
                    failedSubscriptions.add(new ImmutablePair<>(sid, ex.getMessage()));
                    continue;
                }
            }
        } catch (Exception ex) {
            DefaultLoader.getUIHelper().logError("An error occurred when trying to load Redis Caches\n\n" + ex.getMessage(), ex);
        }
        if (!failedSubscriptions.isEmpty()) {
            StringBuilder errorMessage = new StringBuilder("An error occurred when trying to load Redis Caches for the subscriptions:\n\n");
            for (Pair error : failedSubscriptions) {
                errorMessage.append(error.getKey()).append(": ").append(error.getValue()).append("\n");
            }
            DefaultLoader.getUIHelper().logError("An error occurred when trying to load Redis Caches\n\n" + errorMessage.toString(), null);
        }
    }
}

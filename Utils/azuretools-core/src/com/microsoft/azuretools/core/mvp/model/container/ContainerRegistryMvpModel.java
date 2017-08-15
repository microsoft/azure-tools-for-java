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

package com.microsoft.azuretools.core.mvp.model.container;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.containerregistry.Registries;
import com.microsoft.azure.management.containerregistry.Registry;
import com.microsoft.azure.management.redis.RedisCache;
import com.microsoft.azure.management.redis.RedisCaches;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.sdkmanage.AzureManager;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContainerRegistryMvpModel {

    private static final String NOT_SIGNED_ERROR = "Azure account is not signed in.";
    private static final String FAILED_LOAD_AZURE = "Failed to load Azure resources.";

    private ContainerRegistryMvpModel() {}

    private static final class SingletonHolder {
        private static final ContainerRegistryMvpModel INSTANCE = new ContainerRegistryMvpModel();
    }

    public static ContainerRegistryMvpModel getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public Map<String, Registries> getContainerRegistries() throws Exception {
        Map<String, Registries> registries = new HashMap<>();
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        if (azureManager == null) {
            throw new Exception(NOT_SIGNED_ERROR);
        }
        List<Subscription> subscriptions = AzureMvpModel.getInstance().getSelectedSubscriptions();
        for (Subscription sub: subscriptions) {
            Azure azure = azureManager.getAzure(sub.subscriptionId());
            if (azure == null || azure.containerRegistries() == null) {
                continue;
            }
            registries.put(sub.subscriptionId(), azure.containerRegistries());
        }
        return registries;
    }

    /**
     * Get ACR by Id.
     */
    public Registry getContainerRegistry(String sid, String id) throws Exception {
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        if (azureManager == null) {
            throw new Exception(NOT_SIGNED_ERROR);
        }
        Azure azure = azureManager.getAzure(sid);
        if (azure == null) {
            throw new Exception(FAILED_LOAD_AZURE);
        }
        Registries registries = azure.containerRegistries();
        if (registries == null) {
            return null;
        }
        return registries.getById(id);
    }
}
